/*
    Turbulence, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package turbulence

import parasite.*
import rudiments.*
import anticipation.*
import perforate.*

import java.util.concurrent as juc

object Multiplexer:
  private object Termination

case class Multiplexer[KeyType, ElemType]()(using Monitor):
  private val tasks: TrieMap[KeyType, Async[Unit]] = TrieMap()
  
  private val queue: juc.LinkedBlockingQueue[ElemType | Multiplexer.Termination.type] =
    juc.LinkedBlockingQueue()

  def close(): Unit = tasks.keys.foreach(remove(_))

  @tailrec
  private def pump(key: KeyType, stream: LazyList[ElemType])(using Submonitor[Unit]): Unit =
    if stream.isEmpty then remove(key) else
      acquiesce()
      queue.put(stream.head)
      pump(key, stream.tail)

  def add(key: KeyType, stream: LazyList[ElemType]): Unit = tasks(key) =
    Async(pump(key, stream))
 
  private def remove(key: KeyType): Unit = synchronized:
    tasks -= key
    if tasks.isEmpty then queue.put(Multiplexer.Termination)
  
  def stream: LazyList[ElemType] = LazyList.continually(queue.take().nn).takeWhile(_ != Multiplexer.Termination)

extension [ElemType](stream: LazyList[ElemType])

  def deduplicate: LazyList[ElemType] =
    def recur(last: ElemType, stream: LazyList[ElemType]): LazyList[ElemType] =
      stream match
        case head #:: tail => if last == head then recur(last, tail) else head #:: recur(head, tail)
        case _             => LazyList()

    stream match
      case head #:: tail => head #:: recur(head, tail)
      case _             => LazyList()

  def rate
      [DurationType: GenericDuration: SpecificDuration](duration: DurationType)
      (using monitor: Monitor, cancel: Raises[CancelError])
      : LazyList[ElemType] =
    
    def recur(stream: LazyList[ElemType], last: Long): LazyList[ElemType] = stream match
      case head #:: tail =>
        val delay = SpecificDuration(duration.milliseconds - (System.currentTimeMillis - last))
        if delay.milliseconds > 0 then sleep(delay)
        stream
      
      case _ =>
        LazyList()

    Async(recur(stream, System.currentTimeMillis)).await()

  def multiplexWith(that: LazyList[ElemType])(using Monitor): LazyList[ElemType] =
    unsafely(LazyList.multiplex(stream, that))

  def regulate(tap: Tap)(using Monitor): LazyList[ElemType] =
    def defer
        (active: Boolean, stream: LazyList[Some[ElemType] | Tap.Regulation], buffer: List[ElemType])
        : LazyList[ElemType] =
      recur(active, stream, buffer)

    @tailrec
    def recur
        (active: Boolean, stream: LazyList[Some[ElemType] | Tap.Regulation], buffer: List[ElemType])
        : LazyList[ElemType] =
      
      if active && buffer.nonEmpty then buffer.head #:: defer(true, stream, buffer.tail)
      else if stream.isEmpty then LazyList()
      else stream.head match
        case Tap.Regulation.Start =>
          recur(true, stream.tail, buffer)
        
        case Tap.Regulation.Stop =>
          recur(false, stream.tail, Nil)
        
        case Some(other) =>
          if active then other.nn #:: defer(true, stream.tail, Nil)
          else recur(false, stream.tail, other.nn :: buffer)

    LazyList() #::: recur(true, stream.map(Some(_)).multiplexWith(tap.stream), Nil)

  def cluster
      [DurationType: SpecificDuration: GenericDuration]
      (duration: DurationType, maxSize: Maybe[Int] = Unset, maxDelay: Maybe[DurationType] = Unset)
      (using Monitor)
      : LazyList[List[ElemType]] =
    
    def defer(stream: LazyList[ElemType], list: List[ElemType], expiry: Long): LazyList[List[ElemType]] =
      recur(stream, list, expiry)

    @tailrec
    def recur(stream: LazyList[ElemType], list: List[ElemType], expiry: Long): LazyList[List[ElemType]] =
      if list.isEmpty then
        val expiry2: Long =
          maxDelay.option.map(_.milliseconds).fold(Long.MaxValue)(_ + System.currentTimeMillis)
        
        if stream.isEmpty then LazyList() else recur(stream.tail, List(stream.head), expiry2)
      
      else
        val hasMore: Async[Boolean] = Async(!stream.isEmpty)

        val recurse: Option[Boolean] =
          try throwErrors:
            val deadline: Long = duration.milliseconds.min(expiry - System.currentTimeMillis).max(0)
            if hasMore.await(SpecificDuration(deadline)) then Some(true) else None
          catch case err: (TimeoutError | CancelError) => Some(false)

        // The try/catch above seems to fool tail-call identification
        if recurse.isEmpty then LazyList(list)
        else if recurse.get then recur(stream.tail, stream.head :: list, expiry)
        else list.reverse #:: defer(stream, Nil, Long.MaxValue)
    
    LazyList() #::: recur(stream, Nil, Long.MaxValue)

  def parallelMap[ElemType2](fn: ElemType => ElemType2)(using monitor: Monitor): LazyList[ElemType2] =
    
    val out: Funnel[ElemType2] = Funnel()
    
    Async:
      stream.map: elem =>
        Async(out.put(fn(elem)))
    
    out.stream
