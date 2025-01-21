/*
    Superlunary, version [unreleased]. Copyright 2025 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package superlunary

import anthology.*
import anticipation.*
import contingency.*
import digression.*
import fulminate.*
import galilei.*
import gossamer.*
import hellenism.*
import inimitable.*
import jacinta.*
import rudiments.*
import serpentine.*, pathNavigation.linux
import spectacular.*
import vacuous.*

import scala.compiletime.*
import scala.quoted.*
import scala.reflect.Selectable.reflectiveSelectable

extension [ValueType](value: ValueType)(using Quotes)
  inline def put(using references: References): Expr[ValueType] =
    '{  import strategies.throwUnsafely
        ${references.array}(${ToExpr.IntToExpr(references.allocate[ValueType](value))})
        . as[ValueType]  }
