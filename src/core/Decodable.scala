/*
    Jacinta, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package anticipation

infix type in[CodingType, CodecType] = CodingType { type Codec = CodecType }

trait Encodable:
  inline def encodable: this.type = this
  type Self
  type Codec
  def encode(value: Self): Codec
  def omit(value: Self): Boolean = false

  def contramap[Self2](lambda: Self2 => Self): Self2 is Encodable in Codec = value =>
    encodable.encode(lambda(value))

trait Decodable:
  inline def decodable: this.type = this
  type Self
  type Codec
  def decode(value: Codec, omit: Boolean): Self

  def map[Self2](lambda: Self => Self2): Self2 is Decodable in Codec =
    (value, omit) => lambda(decodable.decode(value, omit))
