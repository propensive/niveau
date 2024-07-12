/*
    Spectacular, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package spectacular

import anticipation.*

import language.experimental.captureChecking

extension [ValueType: Showable](value: ValueType)
  def show: Text = ValueType.text(value)

extension [ValueType: Inspectable](value: ValueType) def inspect: Text = ValueType.text(value)

extension (text: Text)
  def decodeAs[ValueType](using decoder: Decoder[ValueType]): ValueType =
    decoder.decode(text)

extension [ValueType](value: ValueType)
  def encode(using encoder: Encoder[ValueType]): Text = encoder.encode(value)

package booleanStyles:
  given BooleanStyle as yesNo = BooleanStyle("yes".tt, "no".tt)
  given BooleanStyle as onOff = BooleanStyle("on".tt, "off".tt)
  given BooleanStyle as trueFalse = BooleanStyle("true".tt, "false".tt)
  given BooleanStyle as oneZero = BooleanStyle("1".tt, "0".tt)
