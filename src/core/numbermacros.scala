/*
    Cardinality, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package cardinality

import rudiments.*

import scala.quoted.*
import scala.compiletime.*

object CardinalityMacro:
  def apply
      [D1 <: Double: Type, D2 <: Double: Type](digits: Expr[String])(using Quotes): Expr[D1 ~ D2] =
    import quotes.reflect.*
    
    digits.value match
      case Some(str) =>
        TypeRepr.of[D1].asMatchable match
          case ConstantType(DoubleConstant(lb)) =>
            TypeRepr.of[D2].asMatchable match
              case ConstantType(DoubleConstant(ub)) =>
                val value = str.toDouble
            
                if value < lb
                then fail(s"the value $str is less than the lower bound for this value, $lb")
            
                if value > ub
                then fail(s"the value $str is greater than the upper bound for this value, $ub")
  
                '{${Expr(value)}.asInstanceOf[D1 ~ D2]}
          
              case _ =>
                fail("upper bound must be a Double singleton literal types")
          case _ =>
            fail("lower bound must be a Double singleton literal types")
      case None =>
        '{NumericRange($digits.toDouble)}
