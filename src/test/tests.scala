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

import probably.*
import gossamer.*
import rudiments.*
import eucalyptus.*

object Tests extends Suite(t"Dissonance tests"):
  given Realm = Realm(t"tests")

  def run(): Unit =
    suite(t"Diff tests"):
      test(t"Empty lists"):
        diff(IArray[Char](), IArray[Char]())
      .assert(_ == Diff())
      
      test(t"One element, equal"):
        diff(IArray('a'), IArray('a'))
      .assert(_ == Diff(Par(0, 0, 'a')))
      
      test(t"Straight swap"):
        diff(IArray('a'), IArray('A'))
      .assert(_ == Diff(Del(0, 'a'), Ins(0, 'A')))
      
      test(t"Two elements, equal"):
        diff(IArray('a', 'b'), IArray('a', 'b'))
      .assert(_ == Diff(Par(0, 0, 'a'), Par(1, 1, 'b')))
      
      test(t"Insertion to empty list"):
        diff(IArray[Char](), IArray('a'))
      .assert(_ == Diff(Ins(0, 'a')))
      
      test(t"Deletion to become empty list"):
        diff(t"a".chars, t"".chars)
      .assert(_ == Diff(Del(0, 'a')))
      
      test(t"Prefix to short list"):
        diff(t"BC".chars, t"ABC".chars)
      .assert(_ == Diff(Ins(0, 'A'), Par(0, 1, 'B'), Par(1, 2, 'C')))
      
      test(t"Suffix to short list"):
        diff(t"AB".chars, t"ABC".chars)
      .assert(_ == Diff(Par(0, 0, 'A'), Par(1, 1, 'B'), Ins(2, 'C')))
      
      test(t"Insertion in middle of short list"):
        diff(t"AC".chars, t"ABC".chars)
      .assert(_ == Diff(Par(0, 0, 'A'), Ins(1, 'B'), Par(1, 2, 'C')))
      
      test(t"Deletion from middle of short list"):
        diff(t"ABC".chars, t"AC".chars)
      .assert(_ == Diff(Par(0, 0, 'A'), Del(1, 'B'), Par(2, 1, 'C')))
      
      test(t"Deletion from start of short list"):
        diff(t"ABC".chars, t"BC".chars).edits.to(List)
      .assert(_ == List(Del(0, 'A'), Par(1, 0, 'B'), Par(2, 1, 'C')))
      
      test(t"Deletion from end of short list"):
        diff(t"ABC".chars, t"AB".chars)
      .assert(_ == Diff(Par(0, 0, 'A'), Par(1, 1, 'B'), Del(2, 'C')))
      
      test(t"Multiple inner keeps"):
        diff(t"BCD".chars, t"ABC".chars)
      .assert(_ == Diff(Ins(0, 'A'), Par(0, 1, 'B'), Par(1, 2, 'C'), Del(2, 'D')))
      
      test(t"Example from blog"):
        diff(t"ABCABBA".chars, t"CBABAC".chars).edits.to(List)
      .assert(_ == List(Del(0, 'A'), Del(1, 'B'), Par(2, 0, 'C'), Ins(1, 'B'), Par(3, 2, 'A'),
          Par(4, 3, 'B'), Del(5, 'B'), Par(6, 4, 'A'), Ins(5, 'C')))
      
      test(t"Reversed example from blog"):
        diff(t"CBABAC".chars, t"ABCABBA".chars).edits.to(List)
      .assert(_ == List(Del(0, 'C'), Ins(0, 'A'), Par(1, 1, 'B'), Ins(2, 'C'), Par(2, 3, 'A'),
          Par(3, 4, 'B'), Ins(5, 'B'), Par(4, 6, 'A'), Del(5, 'C')))
      
      test(t"Item swap"):
        diff(t"AB".chars, t"BA".chars)
      .assert(_ == Diff(Del(0, 'A'), Par(1, 0, 'B'), Ins(1, 'A')))
      
      test(t"Item change"):
        diff(t"A".chars, t"C".chars)
      .assert(_ == Diff(Del(0, 'A'), Ins(0, 'C')))
      
      test(t"Item change between values"):
        diff(t"NAN".chars, t"NCN".chars)
      .assert(_ == Diff(Par(0, 0, 'N'), Del(1, 'A'), Ins(1, 'C'), Par(2, 2, 'N')))
      
      test(t"Item swap between values"):
        diff(t"NABN".chars, t"NBAN".chars)
      .assert(_ == Diff(Par(0, 0, 'N'), Del(1, 'A'), Par(2, 1, 'B'), Ins(2, 'A'), Par(3, 3, 'N')))
      
      test(t"Item swap interspersed with values"):
        diff(t"AZB".chars, t"BZA".chars)
      .assert(_ == Diff(Del(0, 'A'), Del(1, 'Z'), Par(2, 0, 'B'), Ins(1, 'Z'), Ins(2, 'A')))
      
      test(t"real-world example 1"):
        diff(IArray('a', 'b', 'c'), IArray('A', 'b', 'C')).edits.to(List)
      .assert(_ == Diff(Del(0, 'a'), Ins(0, 'A'), Par(1, 1, 'b'), Del(2, 'c'), Ins(2, 'C')).edits.to(List))
      
      test(t"real-world example 2"):
        diff(IArray(t"A", t"B"), IArray(t"B", t"C", t"D"))
      .assert(_ == Diff(Del(0, t"A"), Par(1, 0, t"B"), Ins(1, t"C"), Ins(2, t"D")))
      
  