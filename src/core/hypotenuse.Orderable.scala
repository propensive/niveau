/*
    Hypotenuse, version 0.26.0. Copyright 2025 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package hypotenuse

import language.experimental.captureChecking

import scala.annotation.*

object Orderable:
  given [ValueType: Ordering] => ValueType is Orderable:
    inline def compare
       (inline left:    ValueType,
        inline right:   ValueType,
        inline strict:  Boolean,
        inline greater: Boolean)
            : Boolean =
      val n = ValueType.compare(left, right)
      inline if greater
      then inline if strict then n > 0 else n >= 0
      else inline if strict then n < 0 else n <= 0

trait Orderable extends Commensurable:
  type Self
  type Operand = Self
