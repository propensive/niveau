/*
    Rudiments, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package rudiments

import language.experimental.captureChecking

extension [ValueType](seq: Iterable[ValueType])
  transparent inline def mtwin: Iterable[(ValueType, ValueType)] = seq.map { x => (x, x) }
  transparent inline def mtriple: Iterable[(ValueType, ValueType, ValueType)] = seq.map { x => (x, x, x) }

  def indexBy[ValueType2](fn: ValueType -> ValueType2): Map[ValueType2, ValueType] throws DuplicateIndexError =
    val map = seq.map: value =>
      (fn(value), value)
    
    if seq.size != map.size then throw DuplicateIndexError() else map.to(Map)

case class KeyNotFoundError(name: Text)
extends Error(ErrorMessage[Text *: EmptyTuple](List(Text("key "), Text(" not found")), name *: EmptyTuple))

case class DuplicateIndexError()
extends Error(ErrorMessage[EmptyTuple](
  List(Text("the sequence contained more than one element that mapped to the same index")), EmptyTuple
))

extension [ElemType](value: IArray[ElemType])
  inline def mutable(using erased Unsafe.type): Array[ElemType] = value.asInstanceOf[Array[ElemType]]

extension [ElemType](value: Array[ElemType])
  inline def immutable(using erased Unsafe.type): IArray[ElemType] = value match
    case array: IArray[ElemType] @unchecked => array
    case _                                  => throw Mistake("Should never match")

  def snapshot(using ClassTag[ElemType]): IArray[ElemType] =
    val newArray = new Array[ElemType](value.length)
    System.arraycopy(value, 0, newArray, 0, value.length)
    newArray.immutable(using Unsafe)

extension [KeyType, ValueType](map: Map[KeyType, ValueType])
  def upsert(key: KeyType, op: Maybe[ValueType] => ValueType): Map[KeyType, ValueType] =
    map.updated(key, op(if map.contains(key) then map(key) else Unset))

  def collate(otherMap: Map[KeyType, ValueType])(merge: (ValueType, ValueType) => ValueType)
             : Map[KeyType, ValueType] =
    otherMap.foldLeft(map): (acc, kv) =>
      acc.updated(kv(0), acc.get(kv(0)).fold(kv(1))(merge(kv(1), _)))

extension [K, V](map: Map[K, List[V]])
  def plus(key: K, value: V): Map[K, List[V]] = map.updated(key, map.get(key).fold(List(value))(value :: _))

extension [ElemType](seq: Seq[ElemType])
  def random: ElemType = seq(util.Random().nextInt(seq.length))
  transparent inline def shuffle: Seq[ElemType] = util.Random().shuffle(seq)
  
  def runs(fn: ElemType => Any): List[List[ElemType]] =
    @tailrec
    def recur(current: Any, todo: Seq[ElemType], run: List[ElemType], done: List[List[ElemType]])
             : List[List[ElemType]] =
      if todo.isEmpty then (run.reverse :: done).reverse
      else
        val focus = fn(todo.head)
        if current == focus then recur(current, todo.tail, todo.head :: run, done)
        else recur(focus, todo.tail, List(todo.head), run.reverse :: done)

    if seq.isEmpty then Nil else recur(fn(seq.head), seq.tail, List(seq.head), Nil)

object Cursor:
  opaque type Cursor = Int
  opaque type CursorSeq[T] <: IndexedSeq[T] = IndexedSeq[T]

  extension (cursor: Cursor)
    inline def index: Int = cursor
    
    inline def of[ElemType](inline seq: CursorSeq[ElemType]): ElemType = seq(cursor.index)
    
    inline def of[ElemType](inline seq: CursorSeq[ElemType], inline offset: Int): Maybe[ElemType] =
      if (cursor.index + offset) >= 0 && (cursor.index + offset) < seq.length then seq(cursor.index + offset)
      else Unset

  inline def curse
      [ElemType, ElemType2](seq: IndexedSeq[ElemType])(inline fn: (CursorSeq[ElemType], Cursor) ?=> ElemType2)
      : IndexedSeq[ElemType2] =
    seq.indices.map { index => fn(using seq, index) }

inline def cursor
    [ElemType](using inline seq: Cursor.CursorSeq[ElemType], inline cursor: Cursor.Cursor): ElemType =
  cursor.of(seq)

inline def precursor
    [ElemType](using inline seq: Cursor.CursorSeq[ElemType], inline cursor: Cursor.Cursor): Maybe[ElemType] =
  cursor.of(seq, -1)

inline def postcursor
    [ElemType](using inline seq: Cursor.CursorSeq[ElemType], inline cursor: Cursor.Cursor): Maybe[ElemType] =
  cursor.of(seq, 1)

inline def cursorIndex(using inline cursor: Cursor.Cursor): Int = cursor.index

inline def cursorOffset
    [ElemType](n: Int)(using inline seq: Cursor.CursorSeq[ElemType], inline cursor: Cursor.Cursor)
    : Maybe[ElemType] =
  cursor.of(seq, n)

extension [ElemType](seq: IndexedSeq[ElemType])
  transparent inline def curse
      [ElemType2](inline fn: (Cursor.CursorSeq[ElemType], Cursor.Cursor) ?=> ElemType2): IndexedSeq[ElemType2] =
    Cursor.curse(seq)(fn)

extension (iarray: IArray.type)
  def create[ElemType: ClassTag](size: Int)(fn: Array[ElemType] => Unit): IArray[ElemType] =
    val array: Array[ElemType] = new Array[ElemType](size)
    fn(array)
    array.immutable(using Unsafe)
