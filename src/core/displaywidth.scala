/*
    Hieroglyph, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package hieroglyph

import rudiments.*
import kaleidoscope.*
import java.io as ji

import scala.collection.immutable.TreeMap

object Unicode:
  import Hieroglyph.*
  
  object EaWidth:
    def unapply(code: Text): Option[EaWidth] =
      code.s.only:
        case "N"  => Neutral
        case "W"  => Wide
        case "A"  => Ambiguous
        case "H"  => HalfWidth
        case "F"  => FullWidth
        case "Na" => Narrow
      .option
  
  enum EaWidth:
    case Neutral, Narrow, Wide, Ambiguous, FullWidth, HalfWidth

    def width: Int = this match
      case Wide | FullWidth => 2
      case _                => 1

  object Hex:
    def unapply(text: Text): Option[Int] =
      try Some(Integer.parseInt(text.s, 16)) catch case err: NumberFormatException => None

  def eastAsianWidth(char: Char): Maybe[EaWidth] =
    eastAsianWidths.minAfter(CharRange(char.toInt, char.toInt)).maybe.mm(_(1))

  var count = 0

  lazy val eastAsianWidths: TreeMap[CharRange, EaWidth] =
    extension (map: TreeMap[CharRange, EaWidth])
      def append(range: CharRange, width: EaWidth): TreeMap[CharRange, EaWidth] =
        if map.isEmpty then map.updated(range, width)
        else if map.lastKey.to == (range.from - 1) && map(map.lastKey) == width
        then map.removed(map.lastKey).updated(CharRange(map.lastKey.from, range.to), width)
        else map.updated(range, width)

    @tailrec
    def recur(stream: LazyList[Text], map: TreeMap[CharRange, EaWidth]): TreeMap[CharRange, EaWidth] =
      stream match
        case r"${Hex(from)}([0-9A-F]{4})\.\.${Hex(to)}([0-9A-F]{4});${EaWidth(w)}([AFHNW]a?).*" #:: tail =>
          recur(tail, map.append(CharRange(from, to), w))
        case r"${Hex(from)}([0-9A-F]{4});${EaWidth(w)}([AFHNW]a?).*" #:: tail =>
          recur(tail, map.append(CharRange(from, from), w))
        case head #:: tail =>
          recur(tail, map)
        case _ =>
          map
    
    val in: ji.InputStream = Option(getClass.getResourceAsStream("/hieroglyph/EastAsianWidth.txt")).map(_.nn).getOrElse:
      throw Mistake(msg"could not find hieroglyph/EastAsianWidth.txt on the classpath")
    
    val stream = scala.io.Source.fromInputStream(in).getLines.map(Text(_)).to(LazyList)
  
    recur(stream, TreeMap())

extension (char: Char)
  def displayWidth: Int = Unicode.eastAsianWidth(char).mm(_.width).or(1)

@missingContext("a contextual TextWidthCalculator is required to work out the horizontal space a string of text takes when rendered in a monospaced font; for most purposes,\n\n    hieroglyph.textWidthCalculation.uniform\n\nwill suffice, but if using East Asian scripts,\n\n    import hieroglyph.textWidthCalculation.eastAsianScripts\n\nshould be used.")
trait TextWidthCalculator:
  def width(text: Text): Int
  def width(char: Char): Int

package textWidthCalculation:
  given uniform: TextWidthCalculator with
    def width(text: Text): Int = text.s.length
    def width(char: Char): Int = 1
