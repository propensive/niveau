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

import anticipation.*
import gossamer.*
import larceny.*
import perforate.*
import probably.*
import rudiments.*

import language.experimental.captureChecking

case class Person(name: Text, age: Int)

object Tests extends Suite(t"Rudiments Tests"):
  def run(): Unit =
    
    test(t"Dual extraction"):
      object AsInt:
        def unapply(x: String): Option[Char] = Some('I')
      
      object AsLong:
        def unapply(x: String): Option[Char] = Some('J')

      "123" match
        case AsInt(x) & AsLong(y) => (x, y)
        case _                    => ('X', 'X')
    .assert(_ == ('I', 'J'))

    test(t"Display a PID"):
      Pid(2999).toString
    .assert(_ == "↯2999")

    suite(t"bin tests"):
      test(t"Specify a byte"):
        val x: Byte = bin"10101010"
        x
      .assert(_ == -86)
      
      test(t"Specify a short"):
        val x: Short = bin"01111111 11111111"
        x
      .assert(_ == 32767)
      
      test(t"Specify an integer"):
        val x: Int = bin"00000000 11111111 11111111 11111111"
        x
      .assert(_ == 16777215)
      
      test(t"Specify a long"):
        val x: Long = bin"10101010 10101010 10101010 10101010 00000000 11111111 11111111 11111111"
        x
      .assert(_ == -6148914694083051521L)
      
      test(t"Too many bits"):
        demilitarize:
          val x: Long = bin"010101010 10101010 10101010 10101010 00000000 11111111 11111111 11111111"
          x
        .map(_.errorId)
      .assert(_ == List(ErrorId.NoExplanationID))
      
      test(t"Incorrect bit count"):
        demilitarize:
          val x: Long = bin"0101010 10101010 10101010 10101010 00000000 11111111 11111111 11111111"
          x
        .map(_.errorId)
      .assert(_ == List(ErrorId.NoExplanationID))
      
      test(t"Too many bits for type"):
        demilitarize:
          val x: Byte = bin"00011111 11111111"
          x
        .map(_.errorId)
      .assert(_ == List(ErrorId.TypeMismatchID))
      
      test(t"Non-binary content"):
        demilitarize:
          bin"00011112 11111111"
        .map(_.errorId)
      .assert(_ == List(ErrorId.NoExplanationID))

    suite(t"hex tests"):
      test(t"Specify some bytes"):
        hex"bacdf1e9".to(List)
      .assert(_ == Bytes(-70, -51, -15, -23).to(List))
      
      test(t"Specify some bytes in uppercase with a space"):
        hex"BACD F1E9".to(List)
      .assert(_ == Bytes(-70, -51, -15, -23).to(List))
      
      test(t"Non-even number of bytes"):
        demilitarize:
          hex"bacdf1e"
        .map(_.message)
      .assert(_ == List(t"rudiments: a hexadecimal value must have an even number of digits"))
    
      test(t"Non-hex content"):
        demilitarize:
          hex"bacdf1eg"
        .map(_.message)
      .assert(_ == List(t"rudiments: g is not a valid hexadecimal character"))
   
      test(t"Convert a byte to hex"):
        126.toByte.hex
      .assert(_ == t"7e")
      
      test(t"Convert a short to hex"):
        32767.toShort.hex
      .assert(_ == t"7fff")

      test(t"Convert an integer to hex"):
        123456789.hex
      .assert(_ == t"75bcd15")
      
      test(t"Convert a long to hex"):
        654321123456789L.hex
      .assert(_ == t"2531a0221f715")

      test(t"Pattern match hex"):
        t"1234" match
          case Hex(value) => value
          case _          => 0
      .assert(_ == 4660)

    suite(t"Collections tests"):
      val numbers = List(t"one", t"two", t"four", t"six", t"eight", t"nine")
      
      test(t"Index unique numbers by their first letter"):
        safely:
          numbers.indexBy(_.head)
      .assert(_ == Map('o' -> t"one", 't' -> t"two", 'f' -> t"four", 's' -> t"six", 'e' -> t"eight", 'n' -> t"nine"))
      
      //test(t"Index unique numbers by their length"):
      //  capture[DuplicateIndexError]:
      //    numbers.indexBy(_.length)
      //.assert(_ == DuplicateIndexError())
      
      test(t"Sift some options"):
        List(None, Some(1), Some(2), None).sift[Some[Int]]
      .assert(_ == List(Some(1), Some(2)))

      test(t"Sift on singleton type"):
        List.range(0, 10).sift[5]
      .assert(_ == List(5))

      test(t"Sift on a union of singleton types"):
        List.range(0, 10).sift[5 | 7]
      .assert(_ == List(5, 7))
      
      test(t"Map a List to twins"):
        List(1, 2, 3).mtwin
      .assert(_ == List((1, 1), (2, 2), (3, 3)))
      
      test(t"Map a Set to triples"):
        Set(1, 2, 3).mtriple
      .assert(_ == Set((1, 1, 1), (2, 2, 2), (3, 3, 3)))

      test(t"Take a snapshot of an array"):
        val array = Array[Int](1, 2, 3, 4, 5)
        array(1) = 17
        val snapshot: IArray[Int] = array.snapshot
        array(1) = 42
        snapshot.to(List)
      .assert(_ == List(1, 17, 3, 4, 5))
      
      test(t"Take Map#upsert as an insertion"):
        val map = Map(1 -> "one", 2 -> "two")
        map.upsert(3, _.or("three"))
      .assert(_ == Map(1 -> "one", 2 -> "two", 3 -> "three"))
      
      test(t"Take Map#upsert as an update"):
        val map = Map(1 -> "one", 2 -> "two")
        map.upsert(2, _.or("")+"!")
      .assert(_ == Map(1 -> "one", 2 -> "two!"))
      
      test(t"Collation"):
        val map1 = Map(1 -> List("one"), 2 -> List("two"))
        val map2 = Map(2 -> List("deux"), 3 -> List("trois"))
        map1.collate(map2): (left, right) =>
          left ::: right
      .assert(_ == Map(1 -> List("one"), 2 -> List("two", "deux"), 3 -> List("trois")))

      test(t"runs"):
        List(1, 2, 2, 1, 1, 1, 4, 4).runs
      .assert(_ == List(List(1), List(2, 2), List(1, 1, 1), List(4, 4)))

      test(t"runsBy"):
        List(1, 2, 2, 1, 1, 1, 4, 4).runsBy(_%3)
      .assert(_ == List(List(1), List(2, 2), List(1, 1, 1, 4, 4)))

    suite(t"Longest train tests"):
      test(t"Find longest train of zeros in middle"):
        List(1, 0, 0, 2, 3, 4, 0, 0, 0, 5, 6, 0, 7).longestTrain(_ == 0)
      .assert(_ == (6, 3))
      
      test(t"Find longest train of zeros at start"):
        List(0, 0, 0, 2, 3, 4, 0, 0, 1, 5, 6, 0, 7).longestTrain(_ == 0)
      .assert(_ == (0, 3))
      
      test(t"Find longest train of zeros at end"):
        List(0, 0, 1, 2, 3, 4, 0, 0, 1, 5, 6, 0, 0, 0, 0).longestTrain(_ == 0)
      .assert(_ == (11, 4))
    
    suite(t"Optional tests"):
      val absentInt: Optional[Int] = Unset
      val setInt: Optional[Int] = 42

      test(t"Check whether absent value is absent"):
        absentInt.absent
      .assert(_ == true)

      test(t"Check thet set value is not absent"):
        setInt.absent
      .assert(_ == false)

      test(t"Unsafely vouch a set value"):
        val x: Int = setInt.vouch(using Unsafe)
        x
      .assert(_ == 42)

      test(t"Provide an alternative for an absent value"):
        absentInt.or(1)
      .assert(_ == 1)

      test(t"Provide an alternative for a set value"):
        setInt.or(1)
      .assert(_ == 42)

      test(t"Presume a default value for an absent value"):
        absentInt.presume
      .assert(_ == 0)

      test(t"Convert an absent value to an Option"):
        absentInt.option
      .assert(_ == None)

      test(t"Convert a set value to an Option"):
        setInt.option
      .assert(_ == Some(42))

      test(t"Fold over a Optional"):
        absentInt.fm(0)(_ + 1)
      .assert(_ == 0)

      test(t"Fold over a set Optional"):
        setInt.fm(0)(_ + 1)
      .assert(_ == 43)

      test(t"Map over an absent Optional"):
        absentInt.let(_ + 1)
      .assert(_ == Unset)

      test(t"Map over a set Optional"):
        setInt.let(_ + 1)
      .assert(_ == 43)

      test(t"Construct a new Optional from a null value"):
        val x: String | Null = null
        Optional(x)
      .assert(_ == Unset)

      test(t"Construct a new Optional from a possibly-null value"):
        val x: String | Null = ""
        Optional(x)
      .assert(_ == "")

      test(t"Convert an option to a maybe"):
        val x: Option[Int] = Some(42)
        x.maybe
      .assert(_ == 42)

      test(t"Convert an empty Option to a maybe"):
        val x: Option[Int] = None
        x.maybe
      .assert(_ == Unset)

      test(t"Presume a value for an empty Option"):
        val x: Option[List[Int]] = None
        x.presume
      .assert(_ == Nil)

    suite(t"PID & exit status tests"):
      test(t"Zero exit-status is OK"):
        ExitStatus(0)
      .assert(_ == ExitStatus.Ok)
      
      test(t"Positive exit-status is a failure"):
        ExitStatus(1)
      .assert(_ == ExitStatus.Fail(1))

      test(t"Ok has exit status 0"):
        ExitStatus.Ok
      .assert(_() == 0)
      
      test(t"Failure has non-zero exit status"):
        ExitStatus.Fail(3)
      .assert(_() == 3)
    
    suite(t"Bytes tests"):
      test(t"Construct a `Bytes` literal"):
        Bytes(1, 2, 3)
      .assert(_.length == 3)
      
      test(t"Construct a `Bytes` value from a Long"):
        Bytes(Long.MaxValue)
      .assert(_.length == 8)
      
      test(t"Construct an empty `Bytes`"):
        Bytes()
      .assert(_.length == 0)

    suite(t"Byte Size tests"):
      test(t"Construct a simple ByteSize"):
        4.b: ByteSize
      .assert(_ == ByteSize(4))
      
      test(t"Construct a simple ByteSize in kB"):
        4.kb: ByteSize
      .assert(_ == ByteSize(4096))
      
      test(t"Construct a simple ByteSize in MB"):
        4.mb: ByteSize
      .assert(_ == ByteSize(4096*1024L))
      
      test(t"Construct a simple ByteSize in GB"):
        4.gb: ByteSize
      .assert(_ == ByteSize(4096*1024L*1024L))

      test(t"Construct a simple ByteSize in TB"):
        4.tb: ByteSize
      .assert(_ == ByteSize(4096*1024L*1024L*1024L))

      test(t"Compare bytes with >"):
        4.gb > 4.mb
      .assert(_ == true)
      
      test(t"Compare bytes with >="):
        4.gb >= 4.mb*1024
      .assert(_ == true)

      test(t"Sort some byte sizes"):
        List(1.b, 1.mb, 1.kb).sorted
      .assert(_ == List(1.b, 1.kb, 1.mb))
    
    suite(t"Y-combinator test"):
      test(t"Check factorial implementation"):
        def factorial(n: Int): Int = fix[Int] { i => if i <= 0 then 1 else i*recur(i - 1) } (n)
        factorial(4)
      .assert(_ == 24)
   
    suite(t"Inequality tests"):
      test(t"1.2 < x < 1.4"):
        List(1.1, 1.2, 1.3, 1.4, 1.5).filter(1.2 < _ < 1.4)
      .assert(_ == List(1.3))
      
      test(t"1.2 < x <= 1.4"):
        List(1.1, 1.2, 1.3, 1.4, 1.5).filter(1.2 < _ <= 1.4)
      .assert(_ == List(1.3, 1.4))
      
      test(t"1.2 <= x < 1.4"):
        List(1.1, 1.2, 1.3, 1.4, 1.5).filter(1.2 <= _ < 1.4)
      .assert(_ == List(1.2, 1.3))
      
      test(t"1.2 <= x <= 1.4"):
        List(1.1, 1.2, 1.3, 1.4, 1.5).filter(1.2 <= _ <= 1.4)
      .assert(_ == List(1.2, 1.3, 1.4))
      
      test(t"2 < x < 4"):
        List(1, 2, 3, 4, 5).filter(2 < _ < 4)
      .assert(_ == List(3))
      
      test(t"2 < x <= 4"):
        List(1, 2, 3, 4, 5).filter(2 < _ <= 4)
      .assert(_ == List(3, 4))
      
      test(t"2 <= x < 4"):
        List(1, 2, 3, 4, 5).filter(2 <= _ < 4)
      .assert(_ == List(2, 3))
      
      test(t"2 <= x <= 4"):
        List(1, 2, 3, 4, 5).filter(2 <= _ <= 4)
      .assert(_ == List(2, 3, 4))
      
      test(t"2L < x < 4L"):
        List(1L, 2L, 3L, 4L, 5L).filter(2L < _ < 4L)
      .assert(_ == List(3L))
      
      test(t"2L < x <= 4L"):
        List(1L, 2L, 3L, 4L, 5L).filter(2L < _ <= 4L)
      .assert(_ == List(3L, 4L))
      
      test(t"2L <= x < 4L"):
        List(1L, 2L, 3L, 4L, 5L).filter(2L <= _ < 4L)
      .assert(_ == List(2L, 3L))
      
      test(t"2L <= x <= 4L"):
        List(1L, 2L, 3L, 4L, 5L).filter(2L <= _ <= 4L)
      .assert(_ == List(2L, 3L, 4L))
      
      test(t"2F < x < 4F"):
        List(1F, 2F, 3F, 4F, 5F).filter(2F < _ < 4F)
      .assert(_ == List(3F))
      
      test(t"2F < x <= 4F"):
        List(1F, 2F, 3F, 4F, 5F).filter(2F < _ <= 4F)
      .assert(_ == List(3F, 4F))
      
      test(t"2F <= x < 4F"):
        List(1F, 2F, 3F, 4F, 5F).filter(2F <= _ < 4F)
      .assert(_ == List(2F, 3F))
      
      test(t"2F <= x <= 4F"):
        List(1F, 2F, 3F, 4F, 5F).filter(2F <= _ <= 4F)
      .assert(_ == List(2F, 3F, 4F))
      
      test(t"'2' < x < '4'"):
        List('1', '2', '3', '4', '5').filter('2' < _ < '4')
      .assert(_ == List('3'))
      
      test(t"'2' < x <= '4'"):
        List('1', '2', '3', '4', '5').filter('2' < _ <= '4')
      .assert(_ == List('3', '4'))
      
      test(t"'2' <= x < '4'"):
        List('1', '2', '3', '4', '5').filter('2' <= _ < '4')
      .assert(_ == List('2', '3'))
      
      test(t"'2' <= x <= '4'"):
        List('1', '2', '3', '4', '5').filter('2' <= _ <= '4')
      .assert(_ == List('2', '3', '4'))
      
      test(t"2.toByte < x < 4.toByte"):
        List(1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte).filter(2.toByte < _ < 4.toByte)
      .assert(_ == List(3.toByte))
      
      test(t"2.toByte < x <= 4.toByte"):
        List(1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte).filter(2.toByte < _ <= 4.toByte)
      .assert(_ == List(3.toByte, 4.toByte))
      
      test(t"2.toByte <= x < 4.toByte"):
        List(1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte).filter(2.toByte <= _ < 4.toByte)
      .assert(_ == List(2.toByte, 3.toByte))
      
      test(t"2.toByte <= x <= 4.toByte"):
        List(1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte).filter(2.toByte <= _ <= 4.toByte)
      .assert(_ == List(2.toByte, 3.toByte, 4.toByte))
      
      test(t"2.toShort < x < 4.toShort"):
        List(1.toShort, 2.toShort, 3.toShort, 4.toShort, 5.toShort).filter(2.toShort < _ < 4.toShort)
      .assert(_ == List(3.toShort))
      
      test(t"2.toShort < x <= 4.toShort"):
        List(1.toShort, 2.toShort, 3.toShort, 4.toShort, 5.toShort).filter(2.toShort < _ <= 4.toShort)
      .assert(_ == List(3.toShort, 4.toShort))
      
      test(t"2.toShort <= x < 4.toShort"):
        List(1.toShort, 2.toShort, 3.toShort, 4.toShort, 5.toShort).filter(2.toShort <= _ < 4.toShort)
      .assert(_ == List(2.toShort, 3.toShort))
      
      test(t"2.toShort <= x <= 4.toShort"):
        List(1.toShort, 2.toShort, 3.toShort, 4.toShort, 5.toShort).filter(2.toShort <= _ <= 4.toShort)
      .assert(_ == List(2.toShort, 3.toShort, 4.toShort))



      test(t"1.2 > x > 1.4"):
        List(1.1, 1.2, 1.3, 1.4, 1.5).filter(1.4 > _ > 1.2)
      .assert(_ == List(1.3))
      
      test(t"1.2 >= x > 1.4"):
        List(1.1, 1.2, 1.3, 1.4, 1.5).filter(1.4 >= _ > 1.2)
      .assert(_ == List(1.3, 1.4))
      
      test(t"1.2 > x >= 1.4"):
        List(1.1, 1.2, 1.3, 1.4, 1.5).filter(1.4 > _ >= 1.2)
      .assert(_ == List(1.2, 1.3))
      
      test(t"1.2 >= x >= 1.4"):
        List(1.1, 1.2, 1.3, 1.4, 1.5).filter(1.4 >= _ >= 1.2)
      .assert(_ == List(1.2, 1.3, 1.4))
      
      test(t"2 > x > 4"):
        List(1, 2, 3, 4, 5).filter(4 > _ > 2)
      .assert(_ == List(3))
      
      test(t"2 >= x > 4"):
        List(1, 2, 3, 4, 5).filter(4 >= _ > 2)
      .assert(_ == List(3, 4))
      
      test(t"2 > x >= 4"):
        List(1, 2, 3, 4, 5).filter(4 > _ >= 2)
      .assert(_ == List(2, 3))
      
      test(t"2 >= x >= 4"):
        List(1, 2, 3, 4, 5).filter(4 >= _ >= 2)
      .assert(_ == List(2, 3, 4))
      
      test(t"2L > x > 4L"):
        List(1L, 2L, 3L, 4L, 5L).filter(4L > _ > 2L)
      .assert(_ == List(3L))
      
      test(t"2L >= x > 4L"):
        List(1L, 2L, 3L, 4L, 5L).filter(4L >= _ > 2L)
      .assert(_ == List(3L, 4L))
      
      test(t"2L > x >= 4L"):
        List(1L, 2L, 3L, 4L, 5L).filter(4L > _ >= 2L)
      .assert(_ == List(2L, 3L))
      
      test(t"2L >= x >= 4L"):
        List(1L, 2L, 3L, 4L, 5L).filter(4L >= _ >= 2L)
      .assert(_ == List(2L, 3L, 4L))
      
      test(t"2F > x > 4F"):
        List(1F, 2F, 3F, 4F, 5F).filter(4F > _ > 2F)
      .assert(_ == List(3F))
      
      test(t"2F >= x > 4F"):
        List(1F, 2F, 3F, 4F, 5F).filter(4F >= _ > 2F)
      .assert(_ == List(3F, 4F))
      
      test(t"2F > x >= 4F"):
        List(1F, 2F, 3F, 4F, 5F).filter(4F > _ >= 2F)
      .assert(_ == List(2F, 3F))
      
      test(t"2F >= x >= 4F"):
        List(1F, 2F, 3F, 4F, 5F).filter(4F >= _ >= 2F)
      .assert(_ == List(2F, 3F, 4F))
      
      test(t"'2' > x > '4'"):
        List('1', '2', '3', '4', '5').filter('4' > _ > '2')
      .assert(_ == List('3'))
      
      test(t"'2' >= x > '4'"):
        List('1', '2', '3', '4', '5').filter('4' >=_ > '2')
      .assert(_ == List('3', '4'))
      
      test(t"'2' > x >= '4'"):
        List('1', '2', '3', '4', '5').filter('4' > _ >= '2')
      .assert(_ == List('2', '3'))
      
      test(t"'2' >= x >= '4'"):
        List('1', '2', '3', '4', '5').filter('4' >= _ >= '2')
      .assert(_ == List('2', '3', '4'))
      
      test(t"2.toByte > x > 4.toByte"):
        List(1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte).filter(4.toByte > _ > 2.toByte)
      .assert(_ == List(3.toByte))
      
      test(t"2.toByte >= x > 4.toByte"):
        List(1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte).filter(4.toByte >= _ > 2.toByte)
      .assert(_ == List(3.toByte, 4.toByte))
      
      test(t"2.toByte > x >= 4.toByte"):
        List(1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte).filter(4.toByte > _ >= 2.toByte)
      .assert(_ == List(2.toByte, 3.toByte))
      
      test(t"2.toByte >= x >= 4.toByte"):
        List(1.toByte, 2.toByte, 3.toByte, 4.toByte, 5.toByte).filter(4.toByte >= _ >= 2.toByte)
      .assert(_ == List(2.toByte, 3.toByte, 4.toByte))
      
      test(t"2.toShort > x > 4.toShort"):
        List(1.toShort, 2.toShort, 3.toShort, 4.toShort, 5.toShort).filter(4.toShort > _ > 2.toShort)
      .assert(_ == List(3.toShort))
      
      test(t"2.toShort >= x > 4.toShort"):
        List(1.toShort, 2.toShort, 3.toShort, 4.toShort, 5.toShort).filter(4.toShort >= _ > 2.toShort)
      .assert(_ == List(3.toShort, 4.toShort))
      
      test(t"2.toShort > x >= 4.toShort"):
        List(1.toShort, 2.toShort, 3.toShort, 4.toShort, 5.toShort).filter(4.toShort > _ >= 2.toShort)
      .assert(_ == List(2.toShort, 3.toShort))
      
      test(t"2.toShort >= x >= 4.toShort"):
        List(1.toShort, 2.toShort, 3.toShort, 4.toShort, 5.toShort).filter(4.toShort >= _ >= 2.toShort)
      .assert(_ == List(2.toShort, 3.toShort, 4.toShort))
