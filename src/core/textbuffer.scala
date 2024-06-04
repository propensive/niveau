/*
    Gossamer, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package gossamer

import rudiments.*
import anticipation.*

def append[TextType: Textual, ValueType](using buffer: Buffer[TextType])(value: ValueType)
    (using show: TextType.ShowType[ValueType])
        : Unit =
  buffer.append(TextType.show(value))

extension (textObject: Text.type)
  def construct(block: (buffer: TextBuffer) ?=> Unit): Text =
    val buffer = TextBuffer()
    block(using buffer)
    buffer()

  def fill(length: Int)(lambda: Int => Char): Text =
    val array = new Array[Char](length)
    (0 until length).each { index => array(index) = lambda(index) }

    String(array).tt

abstract class Buffer[TextType]():
  protected def put(text: TextType): Unit
  protected def wipe(): Unit
  protected def result(): TextType

  def append(text: TextType): this.type = this.also(put(text))
  def apply(): TextType = result()
  def clear(): this.type = this.also(wipe())

class TextBuffer() extends Buffer[Text]():
  private val buffer: StringBuilder = StringBuilder()
  protected def put(text: Text): Unit = buffer.append(text)
  protected def wipe(): Unit = buffer.clear()
  protected def result(): Text = buffer.toString().tt
