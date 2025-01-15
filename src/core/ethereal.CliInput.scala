/*
    Ethereal, version 0.26.0. Copyright 2025 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package ethereal

import anticipation.*
import gossamer.*
import prepositional.*
import spectacular.*

object CliInput:
  given Decoder[CliInput] as decoder = text => valueOf(text.lower.capitalize.s)
  given CliInput is Encodable in Text as encodable = _.toString.tt.lower

enum CliInput:
  case Terminal, Pipe
