/*
    Parasite, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package parasite

import fulminate.*

import language.experimental.captureChecking

object ConcurrencyError:
  object Reason:
    given Reason is Communicable =
      case Cancelled       => msg"the operation was cancelled"
      case Incomplete      => msg"the task was not completed"
      case AlreadyComplete => msg"the promise was already completed"
      case Timeout         => msg"the operation timed out"

  enum Reason:
    case Cancelled, Incomplete, AlreadyComplete, Timeout

case class ConcurrencyError(reason: ConcurrencyError.Reason) extends Error(reason.communicate)
