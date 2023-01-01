/*
    Punctuation, version 0.4.0. Copyright 2020-23 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package punctuation

import escapade.*
import rudiments.*

extension (value: Markdown[Markdown.Ast.Node])
  def render(width: Int): AnsiText = TextConverter().convert(value.nodes, 0).serialize(width)

extension (value: Markdown.Ast.Inline)
  @targetName("ansi2")
  def render(width: Int): AnsiText = TextConverter().phrasing(value)
