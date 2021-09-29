/*
    Gossamer, version 0.5.0. Copyright 2020-21 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package gossamer

import rudiments.*
import contextual.*
import wisteria.*

import annotation.targetName

import java.util.regex.*
import java.net.{URLEncoder, URLDecoder}

opaque type Txt = String

extension (text: Txt)
  def s: String = text

extension (text: String)
  def text: Txt = text
  
  @targetName("add")
  infix def +(other: Txt): String = text+other
  
  def slice(index: Int): String = text.substring(index).nn
  def slice(from: Int, to: Int): String = text.substring(from, to).nn
  def length: Int = text.length
  
  def apply(idx: Int): Char throws OutOfRangeError =
    if idx >= 0 && idx < text.length then text(idx)
    else throw OutOfRangeError(idx, 0, text.length)
  
  def populated: Option[String] = if text.isEmpty then None else Some(text)
  def cut(delimiter: String): IArray[String] = cut(delimiter, 0)
  
  def cut(delimiter: String, limit: Int): IArray[String] =
    IArray.from(text.split(Pattern.quote(delimiter), limit).nn.map(_.nn))
  
  def bytes: IArray[Byte] = IArray.from(text.getBytes("UTF-8").nn)
  def chars: IArray[Char] = IArray.from(text.toCharArray.nn)
  def urlEncode: String = URLEncoder.encode(text, "UTF-8").nn
  def urlDecode: String = URLDecoder.decode(text, "UTF-8").nn
  def punycode: String = java.net.IDN.toASCII(text).nn
  def lower: String = text.toLowerCase.nn
  def upper: String = text.toUpperCase.nn

  def padRight(length: Int, char: Char = ' '): String = 
    if text.length < length then text+char.toString*(length - text.length) else text
  
  def padLeft(length: Int, char: Char = ' '): String =
    if text.length < length then char.toString*(length - text.length)+text else text
  
  def editDistanceTo(other: String): Int =
    val m = text.length
    val n = other.length
    val old = new Array[Int](n + 1)
    val dist = new Array[Int](n + 1)

    for j <- 1 to n do old(j) = old(j - 1) + 1
    
    for i <- 1 to m do
      dist(0) = old(0) + 1

      for j <- 1 to n do
        dist(j) = (old(j - 1) + (if text.charAt(i - 1) == other.charAt(j - 1) then 0 else 1))
          .min(old(j) + 1).min(dist(j - 1) + 1)

      for j <- 0 to n
      do
        old(j) = dist(j)
    
    dist(n)

object Txt:
  def apply(str: String): Txt = str

extension (values: Iterable[String])
  def join: String = values.mkString
  def join(separator: String): String = values.mkString(separator)
  
  def join(left: String, separator: String, right: String): String =
    values.mkString(left, separator, right)
  
  def join(separator: String, last: String): String = values.size match
    case 0 => ""
    case 1 => values.head
    case _ => values.init.mkString(separator)+last+values.last

case class OutOfRangeError(idx: Int, from: Int, to: Int)
extends Exception(s"gossamer: the index $idx exceeds the range $from-$to")

object Interpolation:
  case class Input(string: String)

  given [T: Show]: Insertion[Input, T] = value => Input(summon[Show[T]].show(value).s)

  object Str extends Interpolator[Input, String, String]:
    def initial: String = ""
    def parse(state: String, next: String): String = state+next
    def skip(state: String): String = state
    def insert(state: String, input: Input): String = state+input.string
    def complete(state: String): String = state
  
  object Txt extends Interpolator[Input, String, Txt]:
    def initial: String = ""
    def parse(state: String, next: String): String = state+next
    def skip(state: String): String = state
    def insert(state: String, input: Input): String = state+input.string

    def complete(state: String): Txt =
      gossamer.Txt(state.split("\\n\\s*\\n").nn.map(_.nn.replaceAll("\\s\\s*", " ").nn.trim.nn).mkString("\n").nn)