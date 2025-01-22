/*
    Savagery, version [unreleased]. Copyright 2025 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package savagery

import anticipation.*
import prepositional.*
import gossamer.*
import rudiments.*
import spectacular.*

object Coordinates:
  given Coordinates is Encodable in Text as encodable =
    case Rel(vector) => vector.encode
    case Abs(point)  => point.encode

enum Coordinates:
  case Rel(vector: Shift)
  case Abs(point: Point)

  def key(char: Char): Text = this match
    case Rel(_) => char.show.lower
    case Abs(_) => char.show.upper
