/*
    Xylophone, version 0.1.0. Copyright 2021-22 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package xylophone

import rudiments.*
import gossamer.*

case class XmlParseError(line: Int, column: Int) extends Error:
  def message: Text = t"the XML source could not be parsed at line $line, column $column"

case class XmlReadError() extends Error:
  def message: Text = t"could not read value"

case class XmlAccessError(index: Int, path: XmlPath) extends Error:
  def message: Text =
    val ref: Text = if index == 0 then t"any nodes" else t"node $index"
    t"could not access $ref at path ${Xml.pathString(path)}"