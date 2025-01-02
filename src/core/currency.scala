/*
    Plutocrat, version [unreleased]. Copyright 2025 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package plutocrat

import anticipation.*
import gossamer.*
import hieroglyph.*, textMetrics.uniform
import hypotenuse.*
import prepositional.*
import rudiments.*
import spectacular.*
import symbolism.*

import language.experimental.captureChecking

open case class Currency(isoCode: Text, symbol: Text, name: Text, modulus: Int):
  this: Currency =>
  def apply(value: Double): Money[this.type] =
    val integral = value.toLong
    val tweak = (if integral < 0 then -0.5 else 0.5)/modulus
    Money(this)(integral, ((value - integral + tweak)*modulus).toInt)

  def zero: Money[this.type] = apply(0.00)

case class Price[CurrencyType <: Currency & Singleton: ValueOf]
   (principal: Money[CurrencyType], tax: Money[CurrencyType]):

  def effectiveTaxRate: Double = tax/principal

  @targetName("add")
  infix def + (right: Price[CurrencyType]): Price[CurrencyType] =
    Price(principal + right.principal, tax + right.tax)

  @targetName("subtract")
  infix def - (right: Price[CurrencyType]): Price[CurrencyType] =
    Price(principal - right.principal, tax - right.tax)

  @targetName("negate")
  def `unary_-`: Price[CurrencyType] = Price(-principal, -tax)

  @targetName("multiply")
  infix def * (right: Double): Price[CurrencyType] = Price(principal*right, tax*right)

  @targetName("divide")
  infix def / (right: Double): Price[CurrencyType] = Price(principal/right, tax/right)

  def inclusive: Money[CurrencyType] = principal + tax

trait CurrencyStyle:
  def format(currency: Currency, unit: Text, subunit: Text): Text

package currencyStyles:
  given local: CurrencyStyle = (currency, unit, subunit) => t"${currency.symbol}$unit.$subunit"
  given generic: CurrencyStyle = (currency, unit, subunit) => t"$unit.$subunit ${currency.isoCode}"

object Plutocrat:
  opaque type Money[+CurrencyType <: Currency & Singleton] = Long

  object Money:
    erased given underlying[CurrencyType <: Currency & Singleton]: Underlying[Money[CurrencyType], Int] = ###

    def apply(currency: Currency & Singleton)(wholePart: Long, subunit: Int): Money[currency.type] =
      wholePart*currency.modulus + subunit

    given [CurrencyType <: Currency & Singleton]: Ordering[Money[CurrencyType]] =
      Ordering.Long match
        case ordering: Ordering[Money[CurrencyType]] => ordering

    given [CurrencyType <: Currency & Singleton: ValueOf](using currencyStyle: CurrencyStyle)
        => Money[CurrencyType] is Showable =

      money =>
        val currency = valueOf[CurrencyType]
        val units = (money/currency.modulus).toString.show
        val subunit = (money%currency.modulus).toString.show.pad(2, Rtl, '0')

        currencyStyle.format(currency, units, subunit)

    given [CurrencyType <: Currency & Singleton]
        => Money[CurrencyType] is Addable by Money[CurrencyType] into Money[CurrencyType] as addable =
      _ + _

    given [CurrencyType <: Currency & Singleton]
        => Money[CurrencyType] is Subtractable by Money[CurrencyType] into Money[CurrencyType] as subtractable =
      _ - _

    given [CurrencyType <: Currency & Singleton]
        => Money[CurrencyType] is Multiplicable by Int into Money[CurrencyType] as multiplicable =
      _*_

    // given [CurrencyType <: Currency & Singleton, DoubleType <: Double]
    //     => Money[CurrencyType] is Multiplicable by DoubleType into Money[CurrencyType] as multiplicable2 =
    //   (left, right) =>
    //     val value = left*right
    //     (value + value.signum/2).toLong

  extension [CurrencyType <: Currency & Singleton: ValueOf](left: Money[CurrencyType])
    @targetName("greaterThan")
    infix def > (right: Money[CurrencyType]): Boolean = (left: Long) > (right: Long)

    @targetName("greaterThanOrEqual")
    infix def >= (right: Money[CurrencyType]): Boolean = (left: Long) >= (right: Long)

    @targetName("lessThan")
    infix def < (right: Money[CurrencyType]): Boolean = (left: Long) < (right: Long)

    @targetName("lessThanOrEqual")
    infix def <= (right: Money[CurrencyType]): Boolean = (left: Long) <= (right: Long)

    @targetName("divide")
    infix def / (right: Double): Money[CurrencyType] =
      val value = left/right
      (value + value.signum/2).toLong

    @targetName("divide2")
    infix def / (right: Money[CurrencyType]): Double = left.toDouble/right.toDouble

    @targetName("negate")
    def `unary_-`: Money[CurrencyType] = -left
    def tax(rate: Double): Price[CurrencyType] = Price(left, (left*rate + 0.5).toLong)

    @tailrec
    def split(right: Int, result: List[Money[CurrencyType]] = Nil): List[Money[CurrencyType]] =
      if right == 1 then left :: result else
        val share: Money[CurrencyType] = left/right
        val remainder: Money[CurrencyType] = (left - share)
        remainder.split(right - 1, share :: result)

export Plutocrat.Money

extension [CurrencyType <: Currency & Singleton: ValueOf](seq: Iterable[Money[CurrencyType]])
  def total: Money[CurrencyType] =
    def recur(seq: Iterable[Money[CurrencyType]], total: Money[CurrencyType]): Money[CurrencyType] =
      if seq.isEmpty then total else recur(seq.tail, total + seq.head)

    val currency: CurrencyType = summon[ValueOf[CurrencyType]].value
    recur(seq, currency.zero)
