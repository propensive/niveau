/*
    Honeycomb, version 0.9.0. Copyright 2018-21 Jon Pretty, Propensive OÜ.

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

import rudiments.*
import gossamer.*

import scala.annotation.*
import scala.quoted.*

import language.dynamics

private[honeycomb] type Label = String & Singleton
type Html[Children <: Label] = Node[Children] | Text | Int
type Attributes = Map[String, Maybe[Text]]

object Node:
  given Show[Html[?]] =
    case text: Text    => text
    case int: Int      => int.show
    case item: Node[?] => item.show
    case _             => throw Impossible("this should never match")
  
  given Show[Seq[Html[?]]] = _.map(_.show).join
  
  given Show[Node[?]] = item =>
    val filling = item.attributes.map:
      case (key, Unset)       => t" $key"
      case (key, value: Text) => t""" $key="${value}""""
      case _                  => throw Impossible("should never match")
    .join
    
    if item.children.isEmpty && !item.verbatim
    then t"<${item.label}$filling${if item.unclosed then t"" else t"/"}>"
    else t"<${item.label}$filling>${item.children.map(_.show).join}</${item.label}>"

sealed trait Node[+Name <: Label] extends Shown[Node[?]]:
  node =>
    def label: Text
    def attributes: Attributes
    def children: Seq[Html[?]]
    def inline: Boolean
    def unclosed: Boolean
    def verbatim: Boolean
  
    inline def refine[N <: Label]: Option[Node[N]] = label.s match
      case lbl: N => Some:
        new Node[N]:
          def label: Text = lbl.show
          export node.{attributes, children, inline, unclosed, verbatim}
      case _ => None

object Element:
  @targetName("make")
  def apply[T <: Label, C <: Label]
           (labelString: String, unclosed: Boolean, inline: Boolean,
                verbatim: Boolean, attributes: Attributes,
                children: Seq[Html[C] | Seq[Html[C]]] = Nil): Element[T] =
    new Element(labelString, unclosed, inline, verbatim, attributes, flatten(children))

  private def flatten[C <: Label](nodes: Seq[Html[C] | Seq[Html[C]]]): Seq[Html[C]] =
    nodes.flatMap:
      case seq: Seq[?] => seq
      case node        => Seq(node)
    .asInstanceOf[Seq[Html[C]]]

object Html extends Node["html"]:
  def label: Text = t"html"
  def attributes: Attributes = Map()
  def children: Seq[Html[?]] = Nil
  def inline = false
  def unclosed = false
  def verbatim = false

  def apply(head: Node["head"], body: Node["body"]): Element["html"] =
    Element(label.s, unclosed, inline, verbatim, Map(), Seq(head, body))

object TagType:
  given clairvoyant.CssSelection[TagType[?, ?, ?]] = _.labelString

case class TagType[+Name <: Label, Children <: Label, Atts <: Label]
              (labelString: Name, unclosed: Boolean = false, inline: Boolean = false,
                   verbatim: Boolean = false)
extends Node[Name], Dynamic:
  def attributes: Attributes = Map()
  def children: Seq[Html[?]] = Nil
  def label: Text = labelString.show

  type ChildElements = Children

  inline def applyDynamicNamed(method: "apply")
                              (inline attributes: (Atts, Any)*): StartTag[Name, Children] =
    ${Macro.read[Name, Children, Children]('labelString, 'unclosed, 'inline, 'verbatim,
        'attributes)}

  def applyDynamic(method: "apply")
                  (children: (Html[Children] | Seq[Html[Children]])*): Element[Name] =
    Element(labelString, unclosed, inline, verbatim, Map(), children)

object TransTagType:
  given clairvoyant.CssSelection[TransTagType[?, ?, ?]] = _.labelString

case class TransTagType[+Name <: Label, Children <: Label, Atts <: Label]
                   (labelString: Name, unclosed: Boolean = false, inline: Boolean = false,
                        verbatim: Boolean = false)
extends Node[Name], Dynamic:
  def attributes: Attributes = Map()
  def children: Seq[Html[?]] = Nil
  def label: Text = labelString.show

  inline def applyDynamicNamed(method: "apply")
                              (inline attributes: (Atts, Any)*): StartTag[Name, Children] =
    ${Macro.read[Name, Children, Children]('labelString, 'unclosed, 'inline, 'verbatim,
        'attributes)}

  def applyDynamic[Return <: Label]
                  (method: "apply")
                  (children: (Html[Return] | Seq[Html[Return]])*): Element[Return] =
    Element(labelString, unclosed, inline, verbatim, Map(), children)

object StartTag:
  given clairvoyant.CssSelection[StartTag[?, ?]] = elem => elem.label+elem.attributes.map:
    case (key, value: Text) => t"[$key=$value]"
    case (key, Unset)       => t"[$key]"
    case _                  => throw Impossible("should never match")
  .join(t"")

case class StartTag[+Name <: Label, Children <: Label]
                   (labelString: Name, unclosed: Boolean, inline: Boolean, verbatim: Boolean,
                        attributes: Attributes)
extends Node[Name]:
  def children = Nil
  def label: Text = labelString.show
  def apply(children: (Html[Children] | Seq[Html[Children]])*): Element[Name] =
    Element(labelString, unclosed, inline, verbatim, attributes, children)

case class Element[+Name <: Label](labelString: String, unclosed: Boolean, tagInline: Boolean,
                                    verbatim: Boolean, attributes: Map[String, Maybe[Text]],
                                    children: Seq[Html[?]]) extends Node[Name]:

  def label: Text = labelString.show

  lazy val inline: Boolean = tagInline && children.forall:
    case item: Node[?] => item.inline
    case text: Text    => true
    case int: Int      => true
    case _             => throw Impossible("should never match")

case class HtmlDoc(root: Node["html"])

object HtmlDoc:
  given clairvoyant.HttpResponse[HtmlDoc, Text] with
    def mimeType: String = "text/html; charset=utf-8"
    def content(value: HtmlDoc): Text = HtmlDoc.serialize(value)

  def serialize[T](doc: HtmlDoc, maxWidth: Int = -1)(using HtmlSerializer[T]): T =
    summon[HtmlSerializer[T]].serialize(doc, maxWidth)
  
  def simple[Stylesheet](title: Text, stylesheet: Stylesheet = false)
                        (content: (Html[Flow] | Seq[Html[Flow]])*)
                        (using att: Attribute["href", Stylesheet, ?]): HtmlDoc =
    val link = att.convert(stylesheet) match
      case Unset      => Nil
      case text: Text => Seq(Link(rel = t"stylesheet", href = text))
      case _          => throw Impossible("should never match")

    HtmlDoc(Html(Head(Title(title), link), Body(content*)))
