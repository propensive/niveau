/*
    Symbolism, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package symbolism

import prepositional.*

import language.experimental.captureChecking

object Rootable:
  class Basic[RootType <: Int & Singleton, OperandType, ResultType](lambda: OperandType => ResultType)
  extends Rootable[RootType]:
    type Self = OperandType
    type Result = ResultType

    def root(operand: OperandType): ResultType = lambda(operand)

  given Double is Rootable[2] into Double as double = math.sqrt(_)
  given Double is Rootable[3] into Double as double2 = math.cbrt(_)

trait Rootable[RootType <: Int & Singleton]:
  type Self
  type Result
  def root(value: Self): Result
