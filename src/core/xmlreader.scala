/*
    Xylophone, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

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
import anticipation.*
import gossamer.*
import spectacular.*

trait XmlDeserializer[T]:
  def read(xml: Seq[Ast]): Option[T]
  def map[S](fn: T => Option[S]): XmlDeserializer[S] = read(_).flatMap(fn(_))

object XmlDeserializer extends Derivation[XmlDeserializer]:
  given txt: XmlDeserializer[Text] =
    childElements(_).collect { case Ast.Textual(txt) => txt }.headOption
  
  given [T](using decoder: Decoder[T]): XmlDeserializer[T] = value => (value: @unchecked) match
    case Ast.Element(_, Ast.Textual(text) +: _, _, _) +: _ => Some(text.decodeAs[T])
  
  def join[T](caseClass: CaseClass[XmlDeserializer, T]): XmlDeserializer[T] = seq =>
    val elems = childElements(seq)
    
    Some:
      caseClass.construct: param =>
        elems
          .collect { case e: Ast.Element => e }
          .find(_.name.name.s == param.label)
          .flatMap { e => param.typeclass.read(Seq(e)) }.get
  
  def split[T](sealedTrait: SealedTrait[XmlDeserializer, T]): XmlDeserializer[T] = seq =>
    seq.headOption match
      case Some(Ast.Element(_, children, attributes, _)) =>
        attributes
          .get(XmlName(t"type"))
          .flatMap { t => sealedTrait.subtypes.find(_.typeInfo.short == t.s) }
          .flatMap(_.typeclass.read(seq))
      case _ =>
        None
  
  private def childElements(seq: Seq[Ast]): Seq[Ast] =
    seq.collect { case e@Ast.Element(_, children, _, _) => children }.flatten
