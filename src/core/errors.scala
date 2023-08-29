/*
    Parasite, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

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

import rudiments.*
import fulminate.*

import language.experimental.captureChecking

case class CancelError() extends Error(msg"the operation was cancelled")
case class IncompleteError() extends Error(msg"the task was not completed")
case class AlreadyCompleteError() extends Error(msg"the promise was already completed")
case class TimeoutError() extends Error(msg"the operation timed out")