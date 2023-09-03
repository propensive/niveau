/*
    Nettlesome, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package nettlesome

import rudiments.*
import spectacular.*
import gossamer.*
import perforate.*
import fulminate.*
import anticipation.*
import hieroglyph.*, textWidthCalculation.uniform

import scala.quoted.*

object IpAddressError:
  enum Reason:
    case Ipv4ByteOutOfRange(byte: Int)
    case Ipv4WrongNumberOfBytes(count: Int)
    case Ipv6GroupWrongLength(group: Text)
    case Ipv6GroupNotHex(group: Text)
    case Ipv6TooManyNonzeroGroups(count: Int)
    case Ipv6WrongNumberOfGroups(count: Int)
    case Ipv6MultipleDoubleColons
  
  object Reason:
    given MessageShow[Reason] =
      case Ipv4ByteOutOfRange(byte)       => msg"the number $byte is not in the range 0-255"
      case Ipv4WrongNumberOfBytes(count)  => msg"the address contains $count numbers instead of 4"
      case Ipv6GroupNotHex(group)         => msg"the group '$group' is not a hexadecimal number"
      case Ipv6WrongNumberOfGroups(count) => msg"the address has $count groups, but should have 8"
      case Ipv6MultipleDoubleColons       => msg":: appears more than once"
      
      case Ipv6TooManyNonzeroGroups(count) =>
        msg"the address has $count non-zero groups, which is more than is permitted"
      
      case Ipv6GroupWrongLength(group) =>
        msg"the group is more than 4 hexadecimal characters long"

object MacAddressError:
  enum Reason:
    case WrongGroupCount(count: Int)
    case WrongGroupLength(group: Int, length: Int)
    case NotHex(group: Int, content: Text)

  object Reason:
    given MessageShow[Reason] =
      case WrongGroupCount(count)          => msg"there should be six colon-separated groups, but there were $count"
      case WrongGroupLength(group, length) => msg"group $group should be two hex digits, but its length is $length"
      case NotHex(group, content)          => msg"group $group should be a two-digit hex number, but it is $content"

case class MacAddressError(reason: MacAddressError.Reason)
extends Error(msg"the MAC address is not valid because $reason")

import IpAddressError.Reason, Reason.*

case class IpAddressError(reason: Reason)
extends Error(msg"the IP address is not valid because $reason")

object Nettlesome:
  object Opaques:
    opaque type Ipv4 = Int
    opaque type MacAddress = Long

    object Ipv4:
      def apply(byte0: Int, byte1: Int, byte2: Int, byte3: Int): Ipv4 =
        ((byte0 & 255) << 24) + ((byte1 & 255) << 16) + ((byte2 & 255) << 8) + (byte3 & 255)
      
      given debug: Debug[Ipv4] = _.show
      
      given show: Show[Ipv4] = ip =>
        t"${ip.byte0.toString}.${ip.byte1.toString}.${ip.byte2.toString}.${ip.byte3.toString}"
      
      def parse(text: Text)(using Raises[IpAddressError]): Ipv4 = text.cut(t".") match
        case List(As[Int](byte0), As[Int](byte1), As[Int](byte2), As[Int](byte3)) =>
          for byte <- List(byte0, byte1, byte2, byte3)
          do if !(0 <= byte <= 255) then raise(IpAddressError(Ipv4ByteOutOfRange(byte)))(0.toByte)

          Ipv4(byte0.toByte, byte1.toByte, byte2.toByte, byte3.toByte)
        
        case list =>
          raise(IpAddressError(Ipv4WrongNumberOfBytes(list.length)))(Ipv4(0, 0, 0, 0))

    object MacAddress:
      def apply(value: Long): MacAddress = value
      def parse(text: Text): MacAddress raises MacAddressError =
        val groups = text.cut(t":")
        if groups.length != 6 then raise(MacAddressError(MacAddressError.Reason.WrongGroupCount(groups.length)))(())

        @tailrec
        def recur(todo: List[Text], index: Int = 0, acc: Long = 0L): Long = todo match
          case Nil =>
            acc

          case head :: tail =>
            if head.length != 2 then raise(MacAddressError(MacAddressError.Reason.WrongGroupLength(index, head.length)))(())
            
            val value = try Integer.parseInt(head.s, 16) catch case error: NumberFormatException =>
              raise(MacAddressError(MacAddressError.Reason.NotHex(index, head)))(0)
            
            recur(tail, index + 1, (acc << 8) + value)

        recur(groups)

      def apply(byte0: Byte, byte1: Byte, byte2: Byte, byte3: Byte, byte4: Byte, byte5: Byte): MacAddress =
        def recur(todo: List[Byte], done: Long): Long = todo match
          case head :: tail => recur(tail, (done << 8) + head)
          case Nil          => done

        recur(List(byte0, byte1, byte2, byte3, byte4, byte5), 0L)

    extension (macAddress: MacAddress)
      def byte0: Int = (macAddress >>> 40).toInt
      def byte1: Int = (macAddress >>> 32).toInt & 255
      def byte2: Int = (macAddress >>> 24).toInt & 255
      def byte3: Int = (macAddress >>> 16).toInt & 255
      def byte4: Int = (macAddress >>> 8).toInt & 255
      def byte5: Int = macAddress.toInt & 255

      def text: Text =
        List(byte0, byte1, byte2, byte3, byte4, byte5).map(_.toHexString.tt.pad(2, Rtl, '0')).join(t":")

      def long: Long = macAddress
  
    extension (ip: Ipv4)
      def byte0: Int = ip >>> 24
      def byte1: Int = (ip >>> 16) & 255
      def byte2: Int = (ip >>> 8) & 255
      def byte3: Int = ip & 255
    
      @targetName("subnet")
      def /(size: Int): Ipv4Subnet = Ipv4Subnet(ip & (-1 << (32 - size)), size)

  object Ipv4Subnet:
    given Show[Ipv4Subnet] = subnet => t"${subnet.ipv4}/${subnet.size}"

  case class Ipv4Subnet(ipv4: Ipv4, size: Int)
  
  case class Ipv6(highBits: Long, lowBits: Long)

  def ip(context: Expr[StringContext])(using Quotes): Expr[Ipv4 | Ipv6] =
    val text = Text(context.valueOrAbort.parts.head)
    
    failCompilation:
      if text.contains(t".") then
        val ipv4 = Ipv4.parse(text)
        '{Ipv4(${Expr(ipv4.byte0)}, ${Expr(ipv4.byte1)}, ${Expr(ipv4.byte2)}, ${Expr(ipv4.byte3)})}
      else
        val ipv6 = Ipv6.parse(text)
        '{Ipv6(${Expr(ipv6.highBits)}, ${Expr(ipv6.lowBits)})}

  def mac(context: Expr[StringContext])(using Quotes): Expr[MacAddress] = failCompilation:
    val macAddress = MacAddress.parse(context.valueOrAbort.parts.head.tt)
    '{MacAddress(${Expr(macAddress.long)})}

  object Ipv6:
    given debug: Debug[Ipv6] = _.show
    
    given show: Show[Ipv6] = ip =>
      def unpack(long: Long, groups: List[Int] = Nil): List[Int] =
        if groups.length == 4 then groups else unpack(long >>> 16, (long & 65535).toInt :: groups)
      
      def hex(values: List[Int]): Text =
        values.map(Integer.toHexString(_).nn).map(Text(_)).join(t":")

      val groups = unpack(ip.highBits) ++ unpack(ip.lowBits)
      val (middleIndex, middleLength) = groups.longestTrain(_ == 0)

      if middleLength < 2 then hex(groups)
      else t"${hex(groups.take(middleIndex))}::${hex(groups.drop(middleIndex + middleLength))}"

    def apply
        (group0: Int, group1: Int, group2: Int, group3: Int, group4: Int, group5: Int, group6: Int,
            group7: Int): Ipv6 =
      Ipv6(pack(List(group0, group1, group2, group3)), pack(List(group4, group5, group6, group7)))
    
    def parseGroup(text: Text)(using Raises[IpAddressError]): Int =
      if text.length > 4 then raise(IpAddressError(Ipv6GroupWrongLength(text)))(())
      
      text.lower.s.foreach: char =>
        if !('0' <= char <= '9' || 'a' <= char <= 'f')
        then raise(IpAddressError(Ipv6GroupNotHex(text)))(())
      
      Integer.parseInt(text.s, 16)
    
    def pack(groups: List[Int], accumulator: Long = 0L): Long = groups match
      case Nil          => accumulator
      case head :: tail => pack(tail, (accumulator << 16) + (head & 65535))
    
    def parse(text: Text)(using Raises[IpAddressError]): Ipv6 =
      val groups: List[Text] = text.cut(t"::") match
        case List(left, right) =>
          val leftGroups = left.cut(t":").filter(_ != t"")
          val rightGroups = right.cut(t":").filter(_ != t"")
          
          if leftGroups.length + rightGroups.length > 7
          then raise(IpAddressError(Ipv6TooManyNonzeroGroups(leftGroups.length + rightGroups.length)))(())
          
          leftGroups ++ List.fill((8 - leftGroups.length - rightGroups.length))(t"0") ++ rightGroups

        case List(whole) =>
          val groups = whole.cut(t":")
          if groups.length != 8
          then raise(IpAddressError(Ipv6WrongNumberOfGroups(groups.length)))(List.fill(8)(t"0")) else groups
        
        case _ =>
          raise(IpAddressError(Ipv6MultipleDoubleColons))(List.fill(8)(t"0"))
      
      Ipv6(pack(groups.take(4).map(parseGroup)), pack(groups.drop(4).map(parseGroup)))

export Nettlesome.Ipv6
export Nettlesome.Opaques.Ipv4
export Nettlesome.Opaques.MacAddress

extension (inline context: StringContext)
  transparent inline def ip(): Ipv4 | Ipv6 = ${Nettlesome.ip('context)}
  inline def mac(): MacAddress = ${Nettlesome.mac('context)}
