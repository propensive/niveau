/*
    Quantitative, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package quantitative

import gossamer.*
import rudiments.*
import anticipation.*
import hypotenuse.*
import symbolism.*
import spectacular.*

import scala.quoted.*

import language.implicitConversions
import language.experimental.captureChecking

trait SubstituteUnits[UnitsType <: Measure](val name: Text)

object SubstituteUnits:
  given joules: SubstituteUnits[Kilograms[1] & Metres[2] & Seconds[-2]](t"J")
  given newtons: SubstituteUnits[Kilograms[1] & Metres[1] & Seconds[-2]](t"N")
