/*
    Dissonance, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package dissonance

import gossamer.*
import rudiments.*
import eucalyptus.*
import spectacular.*
import annotation.*

import language.experimental.captureChecking

sealed trait Change[+ElemType] extends Product

sealed trait SimpleChange[+ElemType] extends Change[ElemType]:
  def value: ElemType

object Change:
  case class Ins[+ElemType](right: Int, value: ElemType) extends SimpleChange[ElemType]
  case class Del[+ElemType](left: Int, value: ElemType) extends SimpleChange[ElemType]
  case class Keep[+ElemType](left: Int, right: Int, value: ElemType) extends SimpleChange[ElemType]
  case class Replace[+ElemType](left: Int, right: Int, leftValue: ElemType, rightValue: ElemType) extends Change[ElemType]

import Change.*

enum Region[ElemType]:
  case Changed(deletions: List[Change.Del[ElemType]], insertions: List[Change.Ins[ElemType]])
  case Unchanged(retentions: List[Change.Keep[ElemType]])

enum ChangeBlock[+ElemType]:
  case Ins(startRight: Int, values: List[ElemType])
  case Del(startLeft: Int, values: List[ElemType])
  case Keep(startLeft: Int, startRight: Int, values: List[ElemType])
  case Replace(startLeft: Int, startRight: Int, valuesLeft: List[ElemType], valuesRight: List[ElemType])

case class RDiff[ElemType](changes: Change[ElemType]*):
  def flip: RDiff[ElemType] =
    val changes2 = changes.map:
      case Keep(l, r, v)         => Keep(r, l, v)
      case Del(l, v)             => Ins(l, v)
      case Ins(r, v)             => Del(r, v)
      case Replace(l, r, lv, rv) => Replace(r, l, rv, lv)
    
    RDiff[ElemType](changes2*)

case class Diff[ElemType](changes: SimpleChange[ElemType]*):
  def flip: Diff[ElemType] =
    val changes2 = changes.map:
      case Keep(l, r, v) => Keep(r, l, v)
      case Del(l, v)     => Ins(l, v)
      case Ins(r, v)     => Del(r, v)
    
    Diff[ElemType](changes2*)
  
  def apply(list: List[ElemType], update: (ElemType, ElemType) -> ElemType): LazyList[ElemType] =
    def recur(todo: List[SimpleChange[ElemType]], list: List[ElemType]): LazyList[ElemType] = todo match
      case Ins(_, value) :: tail     => value #:: recur(tail, list)
      case Del(_, _) :: tail         => recur(tail, list.tail)
      case Keep(_, _, value) :: tail => update(value, list.head) #:: recur(tail, list.tail)
      case Nil                       => LazyList()

    recur(changes.to(List), list)

  def collate2: List[Region[ElemType]] =
    changes.runs:
      case Keep(_, _, _) => true
      case _             => false
    .map:
      case xs@(Keep(_, _, _) :: _) => val keeps = xs.collect { case keep@Keep(_, _, _) => keep }
                                      Region.Unchanged(keeps)
      case xs                      => val dels = xs.collect { case del@Del(_, _) => del }
                                      val inss = xs.collect { case ins@Ins(_, _) => ins }
                                      Region.Changed(dels, inss)
  
  def rdiff2(similar: (ElemType, ElemType) -> Boolean, swapSize: Int = 1): RDiff[ElemType] =
    val changes = collate2.flatMap:
      case Region.Unchanged(keeps)   => keeps
      case Region.Changed(dels, Nil) => dels
      case Region.Changed(Nil, inss) => inss
      
      case Region.Changed(dels, inss) =>
        if inss.length == dels.length && inss.length <= swapSize
        then dels.zip(inss).map: (del, ins) =>
          Change.Replace[ElemType](del.left, ins.right, del.value, ins.value)
        else
          diff(dels.map(_.value).to(IndexedSeq), inss.map(_.value).to(IndexedSeq), similar).changes.map:
            case Keep(l, r, _) =>
              Change.Replace[ElemType](dels(l).left, inss(r).right, dels(l).value, inss(r).value)
            
            case Del(l, v) =>
              Change.Del[ElemType](dels(l).left, dels(l).value)
            
            case Ins(r, v) =>
              Change.Ins[ElemType](inss(r).right, inss(r).value)
    
    RDiff(changes*)
          
  def rdiff(similar: (ElemType, ElemType) -> Boolean, bySize: Int = 1): RDiff[ElemType] =
    val changes = collate(similar, bySize).flatMap:
      case ChangeBlock.Ins(rightIndex, values) =>
        values.zipWithIndex.map: (value, offset) =>
          Ins(rightIndex + offset, value)
      
      case ChangeBlock.Del(leftIndex, values) =>
        values.zipWithIndex.map: (value, offset) =>
          Del(leftIndex + offset, value)
      
      case ChangeBlock.Keep(leftIndex, rightIndex, values) =>
        values.zipWithIndex.map: (value, offset) =>
          Keep(leftIndex + offset, rightIndex + offset, value)
      
      case ChangeBlock.Replace(leftIndex, rightIndex, leftValues, rightValues) =>
        leftValues.zip(rightValues).zipWithIndex.map:
          case ((leftValue, rightValue), offset) =>
            Replace(leftIndex + offset, rightIndex + offset, leftValue, rightValue)
    
    RDiff(changes*)

  def collate
      (similar: (ElemType, ElemType) -> Boolean, bySize: Int = 1)
      : List[ChangeBlock[ElemType]] =
    changes.runs:
      case Keep(_, _, _) => true
      case _             => false
    .flatMap:
      case xs@(Keep(left, right, _) :: _) => List(ChangeBlock.Keep(left, right, xs.map(_.value)))
      case xs@(Ins(idx, _) :: _)          => List(ChangeBlock.Ins(idx, xs.map(_.value)))
      
      case xs@(Del(leftIdx, _) :: _) =>
        val dels =
          xs.takeWhile:
            case Del(_, _) => true
            case _         => false
          .to(IndexedSeq)
        
        val delValues = dels.map(_.value).to(List)

        xs.drop(dels.length) match
          case Nil =>
            List(ChangeBlock.Del(leftIdx, xs.map(_.value)))
          
          case inss@(Ins(rightIdx, _) :: _) =>
            val insValues = inss.map(_.value)
  
            if dels.length <= bySize && insValues.length <= bySize
            then List(ChangeBlock.Replace(leftIdx, rightIdx, delValues, insValues))
            else diff(delValues.to(IndexedSeq), insValues.to(IndexedSeq),
                similar).changes.runs(_.productPrefix).map:
              case xs@(Ins(idx, _) :: _) => ChangeBlock.Ins(leftIdx + idx, xs.map(_.value))
              case xs@(Del(idx, _) :: _) => ChangeBlock.Del(leftIdx + idx, xs.map(_.value))
              
              case xs@(Keep(left, right, _) :: _) =>
                val valuesLeft = delValues.drop(left).take(xs.length)
                val valuesRight = insValues.drop(right).take(xs.length)
                ChangeBlock.Replace(left + leftIdx, right + rightIdx, valuesLeft, valuesRight)
              case Nil =>
                throw Mistake("Should never have an empty list here")
          case _ =>
            throw Mistake("Should never have an empty list here")

      case Nil =>
        throw Mistake("Should never have an empty list here")
  
object Diff:
  object Point:
    opaque type Point = Long
    def apply(x: Int, y: Int): Point = (x.toLong << 32) + y
    
    given Debug[Point] = point => t"(${point.x},${point.y})"
    extension (point: Point)
      def x: Int = (point >> 32).toInt
      def y: Int = point.toInt
      def del: Point = Point(x + 1, y)
      def ins: Point = Point(x, y + 1)
      def keep: Point = Point(x + 1, y + 1)
      def unkeep: Point = Point(x - 1, y - 1)
      def text: Text = t"[$x,$y]"

import Diff.Point, Point.*

def diff
    [ElemType]
    (left: IndexedSeq[ElemType], right: IndexedSeq[ElemType],
        cmp: (ElemType, ElemType) -> Boolean = { (a: ElemType, b: ElemType) => a == b })
    : Diff[ElemType] =
  val end = Point(left.size, right.size)
  
  @tailrec
  def distance
      (last: IArray[Point] = IArray(count(Point(0, 0))), trace: List[IArray[Point]] = Nil)
      : Diff[ElemType] =
    //println(s"distance(last=${last.debug}, trace=${trace.debug})")
    if last.contains(end) then
      val idx = last.indexOf(end)
      if trace.isEmpty then countback(idx, end, Nil)
      else
        if trace.head.length > idx && count(trace.head(idx).ins) == end
        then countback(idx, end, trace)
        else countback(idx - 1, end, trace)
    
    else
      val round = last.size
      
      val next = IArray.create[Point](round + 1): arr =>
        arr(0) = last(0).ins

        last.indices.foreach: i =>
          arr(i + 1) = count(last(i).del)
          count(last(i).ins).pipe { pt => if i == round || pt.x > arr(i).x then arr(i) = pt }
      
      println(next.debug)

      distance(next, last :: trace)

  @tailrec
  def count(pt: Point): Point =
    if pt.x >= left.size || pt.y >= right.size || !cmp(left(pt.x), right(pt.y)) then pt
    else count(pt.keep)

  def countback
     (idx: Int, cur: Point, trace: List[IArray[Point]], result: List[SimpleChange[ElemType]] = Nil)
     : Diff[ElemType] =
    trace match
      case head :: tail =>
        val target = head(idx)
        println("countback: "+cur.debug+" : "+result.debug)
        if cur == Point(0, 0) then Diff(result*)
        else if cur.x + target.y - cur.y - target.x < 0 then
          println("ins target="+target.debug+" idx="+idx.debug)
          val idx2 = (idx - 1).min(tail.length - 1).max(0)
          val cb = countback(idx2, target, tail, Ins(target.y, right(target.y)) :: result)
          println(cb.debug)
          cb
        else if cur.x + target.y - cur.y - target.x > 0 then
          println("del target="+target.debug+" idx="+idx.debug)
          val cb = countback(0.max(idx).min(tail.length - 1), target, tail, Del(cur.x - 1, left(cur.x - 1)) :: result)
          println(cb.debug)
          cb
        else if cur.x > target.x && cur.y > target.y then
          println("keep target="+target.debug+" idx="+idx.debug)
          val cb = countback(idx, cur.unkeep, trace, Keep(cur.x - 1, cur.y - 1, left(cur.x - 1)) :: result)
          println(cb.debug)
          cb
        else throw Mistake(s"Unexpected: idx=$idx cur=${cur.text} result=${result}")

      case Nil =>
        if cur == Point(0, 0) then Diff(result*)
        else countback(0, cur.unkeep, Nil, Keep(cur.x - 1, cur.y - 1, left(cur.x - 1)) :: result)

  distance()

given Realm = Realm(t"dissonance")
