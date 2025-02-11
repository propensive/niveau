/*
    Iridescence, version 0.26.0. Copyright 2025 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package iridescence

import language.experimental.captureChecking

import scala.util.chaining.*

import anticipation.*

object Cmyk:
  given Cmyk is Chromatic = _.srgb.rgb24.asInt

case class Cmyk(cyan: Double, magenta: Double, yellow: Double, key: Double):
  def srgb: Srgb = cmy.srgb
  def cmy: Cmy = Cmy(cyan*(1 - key) + key, magenta*(1 - key) + key, yellow*(1 - key) + key)
