/*
    Serpentine, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package serpentine

import gossamer.*
import rudiments.*
import digression.*

import language.experimental.captureChecking

object PathError:
  enum Reason:
    case InvalidChar(char: Char)
    case InvalidName(name: Text)
    case ParentOfRoot
    case NotRooted

  given Show[Reason] =
    case Reason.InvalidChar(char) => t"the character '$char' cannot appear in a path"
    case Reason.InvalidName(name) => t"the name '$name' is reserved"
    case Reason.ParentOfRoot      => t"the root has no parent"
    case Reason.NotRooted         => t"the path is not rooted"

case class PathError(reason: PathError.Reason) extends Error(err"the path is invalid because $reason")
