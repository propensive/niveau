/*
    Contingency, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package contingency

import probably.*
import gossamer.*
import fulminate.*
import rudiments.*
import larceny.*

case class AlphaError() extends Error(m"Alpha")
case class BetaError() extends Error(m"Beta")
case class GammaError(number: Int) extends Error(m"Gamma $number")
case class DeltaError() extends Error(m"Delta")
case class EpsilonError() extends Error(m"Epsilon")
case class ZetaError(string: String) extends Error(m"Zeta $string")

case class MiscErrors(errors: List[String] = Nil)
extends Error(m"There was at least one GammaError"):
  @targetName("add")
  infix def + (error: String): MiscErrors = MiscErrors(error :: errors)

case class GammaErrors(errors: List[GammaError] = Nil)
extends Error(m"There was at least one GammaError"):
  @targetName("add")
  infix def + (error: GammaError): GammaErrors = GammaErrors(error :: errors)

object Tests extends Suite(t"Contingency tests"):
  def run(): Unit =
    test(t"an exception can't just be thrown"):
      demilitarize(abort(AlphaError()))
    .assert(_.length > 0)

    test(t"exceptions can be thrown unsafely"):
      demilitarize:
        import strategies.throwUnsafely
        abort(AlphaError())
    .assert(_.length == 0)

    test(t"`safely` allows an exception to be thrown"):
      demilitarize(safely(abort(AlphaError())))
    .assert(_.length == 0)

    test(t"`unsafely` allows an exception to be thrown"):
      demilitarize(safely(abort(AlphaError())))
    .assert(_.length == 0)

    test(t"a specific exception can be captured"):
      given ExpectationError[Any] is Fatal = _ => ExitStatus.Ok
      capture[AlphaError]:
        abort(AlphaError())
    .assert(_ == AlphaError())

    test(t"an unspecified exception can be captured"):
      given ExpectationError[Any] is Fatal = _ => ExitStatus.Ok
      capture:
        abort(AlphaError())
    .assert(_ == AlphaError())

    test(t"one exception can be quelled into another"):
      given ExpectationError[Any] is Fatal = _ => ExitStatus.Ok
      capture[BetaError]:
        quell:
          case AlphaError() => BetaError()
        .within:
          abort(AlphaError())
    .assert(_ == BetaError())

    test(t"one exception can be quashed into a value"):
      quash:
        case AlphaError() => 17
      .within:
        abort(AlphaError())
        7
    .assert(_ == 17)

    test(t"a quashed block returns its value"):
      quash:
        case AlphaError() => 1
      .within:
        17
    .assert(_ == 17)

    test(t"a quelled block returns its value"):
      given BetaError is Fatal = error => ExitStatus.Ok

      quell:
        case AlphaError() => BetaError()
      .within:
        17
    .assert(_ == 17)

    test(t"quelled block can transform to same type"):
      given ExpectationError[Any] is Fatal = error => ExitStatus.Ok
      capture[GammaError]:
        quell:
          case GammaError(n) => GammaError(n + 1)
        .within:
          abort(GammaError(1))
    .assert(_ == GammaError(2))

    test(t"quelled block can transform to different types"):
      import strategies.throwUnsafely
      given ExpectationError[Any] is Fatal = error => ExitStatus.Ok

      capture[ZetaError]:
        quell:
          case AlphaError()  => DeltaError()
          case BetaError()   => EpsilonError()
          case GammaError(n) => ZetaError("gamma")
        .within:
          abort(GammaError(1))
    .assert(_ == ZetaError("gamma"))

    test(t"quashed block can transform to different values"):
      quash:
        case AlphaError()  => "alpha"
        case BetaError()   => "beta"
        case GammaError(n) => "gamma"
      .within:
        abort(BetaError())
        "success"

    test(t"amalgamation failure"):
      amalgamate:
        abort(BetaError())
        42
    .assert(_ == BetaError())

    test(t"amalgamation success"):
      amalgamate:
        if false then abort(BetaError())
        42
    .assert(_ == 42)

    test(t"accrual with two raises"):
      given ExpectationError[?] is Fatal = error => ExitStatus.Fail(1)
      capture[GammaErrors]:
        accrue(GammaErrors()):
          case error: GammaError => accrual + error
        .within:
          raise(GammaError(1))
          raise(GammaError(2))
          "string"

    .assert(_ == GammaErrors(GammaError(2) :: GammaError(1) :: Nil))

    test(t"accrual with no raises"):
      given GammaErrors is Fatal = error => ExitStatus.Fail(1)

      accrue(GammaErrors()):
        case error: GammaError => accrual + error
      .within:
        "string"

    .assert(_ == "string")

    test(t"accrual with a raise and an abort"):
      erased given ExpectationError[?] is Unchecked = ###

      capture[GammaErrors]:
        accrue(GammaErrors()):
          case error: GammaError => accrual + error
        .within:
          raise(GammaError(1))
          abort(GammaError(2))
          "string"
    .assert(_ == GammaErrors(List(GammaError(2), GammaError(1))))

    test(t"accrual with just an abort"):
      erased given ExpectationError[?] is Unchecked = ###

      capture[GammaErrors]:
        accrue(GammaErrors()):
          case error: GammaError => accrual + error
        .within:
          abort(GammaError(2))
          "string"
    .assert(_ == GammaErrors(GammaError(2) :: Nil))

    test(t"accrual with multiple error types"):
      given ExpectationError[?] is Fatal = error => ExitStatus.Fail(1)

      capture[MiscErrors]:
        accrue(MiscErrors()):
          case GammaError(n) => accrual + s"gamma $n"
          case BetaError()   => accrual + "beta"
        .within:
          raise(GammaError(1))
          raise(BetaError())
          raise(GammaError(2))
          "string"

    .assert(_ == MiscErrors("gamma 2" :: "beta" :: "gamma 1" :: Nil))
