/*
    Honeycomb, version [unreleased]. Copyright 2025 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package honeycomb

import anticipation.*
import gossamer.*
import rudiments.*
import spectacular.*
import vacuous.*

object Node:
  given html: [HtmlType <: Html[?]] => HtmlType is Showable = html => html.runtimeChecked match
    case text: Text    => text
    case int: Int      => int.show
    case node: Node[?] => node.show

  given seq: Seq[Html[?]] is Showable = _.map(_.show).join

  given node: [NodeType <: Node[?]] => NodeType is Showable = item =>
    val filling =
      item.attributes.map: keyValue =>
        keyValue.runtimeChecked match
          case (key, Unset)       => t" $key"
          case (key, value: Text) => t""" $key="$value""""

      . join

    if item.children.isEmpty && !item.verbatim
    then t"<${item.label}$filling${if item.unclosed then t"" else t"/"}>"
    else t"<${item.label}$filling>${item.children.map(_.show).join}</${item.label}>"

  def apply(label0: Text, attributes0: Attributes, children0: Seq[Html[?]]): Html[?] = new Node:
    def label = label0
    def attributes = attributes0
    def children = children0

trait Node[+NameType <: Label]:
  def label: Text
  def attributes: Attributes
  def children: Seq[Html[?]]

  def verbatim: Boolean = Html.verbatimElements(label)
  def unclosed: Boolean = Html.unclosedElements(label)

  lazy val block: Boolean = !Html.inlineElements(label) || children.exists:
    case node: Node[?] => node.block
    case _             => false
