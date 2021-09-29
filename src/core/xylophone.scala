/*
    Xylophone, version 0.1.0. Copyright 2018-21 Jon Pretty, Propensive OÜ.

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

import wisteria.*
import rudiments.*
import gossamer.*
import contextual.*

import scala.annotation.StaticAnnotation

import annotation.targetName
import language.dynamics

case class Namespace(id: String, uri: String)

case class XmlName(name: String, namespace: Maybe[Namespace] = Unset):
  override def toString(): String = namespace match
    case Unset                => name
    case Namespace(prefix, _) => str"$prefix:$name"

sealed trait Xml:
  def pointer: List[Int | String | Unit]
  def root: Ast.Root
  
  @targetName("add")
  infix def +(other: Xml): Doc throws XmlAccessError

  def string(using XmlPrinter[String]): String throws XmlAccessError =
    summon[XmlPrinter[String]].print(Doc(Ast.Root(Xml.normalize(this)*)))

  override def toString(): String =
    try printers.compact.print(Doc(Ast.Root(Xml.normalize(this)*)))
    catch case error@XmlAccessError(_, _) => "undefined"

type XmlPath = List[String | Int | Unit]

object Xml:
  given (using XmlPrinter[String]): clairvoyant.HttpResponse[Xml, String] with
    def mimeType: String = "application/xml"
    def content(xml: Xml): String = summon[XmlPrinter[String]].print(xml)

  def print(xml: Xml)(using XmlPrinter[String]): String = summon[XmlPrinter[String]].print(xml)

  def pathString(path: XmlPath): String = if path.isEmpty then "/" else path.map {
    case idx: Int      => str"[$idx]"
    case label: String => str"/$label"
    case unit: Unit    => str"/*"
  }.join

  def parse(content: String): Doc throws XmlParseError =
    import org.w3c.dom as owd, owd.Node.*
    import org.xml.sax as oxs
    import javax.xml.parsers.*
    import java.io.*

    val factory = DocumentBuilderFactory.newInstance().nn
    factory.setNamespaceAware(true)
    val builder = factory.newDocumentBuilder().nn

    val root = 
      try builder.parse(ByteArrayInputStream(content.bytes.unsafeMutable)).nn
      catch case e: oxs.SAXParseException =>
        throw XmlParseError(e.getLineNumber - 1, e.getColumnNumber - 1)

    def getNamespace(node: owd.Node): Maybe[Namespace] =
      Option(node.getPrefix) match
        case None         => Unset
        case Some(prefix) => Option(node.getNamespaceURI) match
                               case None      => Unset
                               case Some(uri) => Namespace(prefix.nn, uri.nn)

    def readNode(node: owd.Node): Ast = node.getNodeType match
      case CDATA_SECTION_NODE =>
        Ast.CData(node.getTextContent.nn)

      case COMMENT_NODE =>
        Ast.Comment(node.getTextContent.nn)

      case ELEMENT_NODE =>
        val xmlName = XmlName(node.getLocalName.nn, getNamespace(node))
        val childNodes = node.getChildNodes
        
        val children =
          (0 until childNodes.nn.getLength).map(childNodes.nn.item(_).nn).map(readNode(_))
        
        val atts = (0 until node.getAttributes.nn.getLength).map(node.getAttributes.nn.item(_).nn)
        val attributes = atts.map { att =>
          val alias: Maybe[Namespace] = getNamespace(att)
          XmlName(att.getLocalName.nn, alias) -> att.getTextContent.nn
        }.to(Map)
        
        Ast.Element(xmlName, children, attributes)

      case PROCESSING_INSTRUCTION_NODE =>
        val name = node.getNodeName.nn
        val content = node.getTextContent.nn
        Ast.ProcessingInstruction(name, content)

      case TEXT_NODE =>
        Ast.Text(node.getTextContent.nn)
      
      case id =>
        Ast.Comment(str"unrecognized node $id")

    readNode(root.getDocumentElement.nn) match
      case elem@Ast.Element(_, _, _, _) => Doc(Ast.Root(elem))
      case _                            => throw Impossible("xylophone: malformed XML")

  def normalize(xml: Xml): Seq[Ast] throws XmlAccessError =
    def recur(path: XmlPath, current: Seq[Ast]): Seq[Ast] = path match
      case Nil =>
        current

      case (idx: Int) :: tail =>
        if current.length <= idx then throw XmlAccessError(idx, path)
        recur(tail, Seq(current(idx)))

      case (unit: Unit) :: tail =>
        val next = current
          .collect { case e@Ast.Element(_, children, _, _) => children }
          .flatten
          .collect { case e: Ast.Element => e }

        recur(tail, next)

      case (label: String) :: tail =>
        val next = current
          .collect { case e@Ast.Element(_, children, _, _) => children }
          .flatten.collect { case e: Ast.Element if e.name.name == label => e }

        recur(tail, next)

    try recur(xml.pointer, xml.root.content)
    catch case XmlAccessError(idx, tail) =>
      throw XmlAccessError(idx, xml.pointer.dropRight(tail.length))


case class Fragment(head: String | Unit, path: XmlPath, root: Ast.Root)
extends Xml, Dynamic:
  def apply(idx: Int = 0): XmlNode = XmlNode(idx, head :: path, root)
  def attribute(key: String): Attribute = apply(0).attribute(key)
  def pointer: XmlPath = (head :: path).reverse
  def selectDynamic(tagName: String): Fragment = Fragment(tagName, head :: path, root)
  def applyDynamic(tagName: String)(idx: Int = 0): XmlNode = selectDynamic(tagName).apply(idx)
  
  @targetName("all")
  def * : Fragment = Fragment((), head :: path, root)
  
  @targetName("add")
  infix def +(other: Xml): Doc throws XmlAccessError =
    Doc(Ast.Root(Xml.normalize(this) ++ Xml.normalize(other)*))
  
  def as[T](using reader: XmlReader[T]): T throws XmlAccessError | XmlReadError = apply().as[T]

case class XmlNode(head: Int, path: XmlPath, root: Ast.Root) extends Xml, Dynamic:
  def selectDynamic(tagName: String): Fragment = Fragment(tagName, head :: path, root)
  def applyDynamic(tagName: String)(idx: Int = 0): XmlNode = selectDynamic(tagName).apply(idx)
  def attribute(attribute: String): Attribute = Attribute(this, attribute)
  def pointer: XmlPath = (head :: path).reverse
  
  @targetName("all")
  def * : Fragment = Fragment((), head :: path, root)
  
  @targetName("add")
  infix def +(other: Xml): Doc throws XmlAccessError =
    Doc(Ast.Root(Xml.normalize(this) ++ Xml.normalize(other)*))

  def as[T: XmlReader]: T throws XmlReadError | XmlAccessError =
    summon[XmlReader[T]].read(Xml.normalize(this)).getOrElse(throw XmlReadError())

case class Doc(root: Ast.Root) extends Xml, Dynamic:
  def pointer: XmlPath = Nil
  def selectDynamic(tagName: String): Fragment = Fragment(tagName, Nil, root)
  def applyDynamic(tagName: String)(idx: Int = 0): XmlNode = selectDynamic(tagName).apply(idx)
  
  @targetName("all")
  def * : Fragment = Fragment((), Nil, root)
  
  @targetName("add")
  infix def +(other: Xml): Doc throws XmlAccessError =
    Doc(Ast.Root(Xml.normalize(this) ++ Xml.normalize(other)*))

  def as[T: XmlReader]: T throws XmlAccessError | XmlReadError =
    summon[XmlReader[T]].read(Xml.normalize(this)).getOrElse(throw XmlReadError())

case class Attribute(node: XmlNode, attribute: String):
  def as[T: XmlReader]: T throws XmlReadError | XmlAccessError =
    val attributes = Xml.normalize(node).headOption match
      case Some(Ast.Element(_, _, attributes, _)) => attributes
      case _                                      => throw XmlReadError()

    summon[XmlReader[T]]
      .read(Seq(Ast.Element(XmlName("empty"), Seq(Ast.Text(attributes(XmlName(attribute)))))))
      .getOrElse(throw XmlReadError())

case class xmlAttribute() extends StaticAnnotation

extension [T](value: T)
  def xml(using writer: XmlWriter[T]): Doc = Doc(Ast.Root(writer.write(value)))
