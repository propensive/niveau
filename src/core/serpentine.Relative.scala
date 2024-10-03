package serpentine

import anticipation.*
import prepositional.*
import gossamer.*
import symbolism.*
import spectacular.*
import rudiments.*
import vacuous.*

import scala.compiletime.*

object Relative:
  given [ElementType, RootType: Navigable by ElementType] => Encoder[Relative by ElementType] =
    relative =>
      if relative.textDescent.isEmpty
      then
        if relative.ascent == 0 then RootType.selfText
        else List.fill(relative.ascent)(RootType.parentElement).join(RootType.separator)
      else relative
       .textDescent
       .reverse
       .join(RootType.ascent*relative.ascent, RootType.separator, t"")

  given [ElementType, RootType: Navigable by ElementType] => (Relative by ElementType) is Showable =
    _.encode

  given [ElementType](using Navigable by ElementType)
      => Decoder[Relative by ElementType] as decoder =
    parse(_)

  def parse[ElementType](using navigable: Navigable by ElementType)(text: Text)
          : Relative by ElementType =
    def recur(start: Int, ascent: Int, elements: List[ElementType]): Relative by ElementType =
      if start >= text.length then Relative(ascent, elements)
      else
        val end = text.s.indexOf(navigable.separator.s, start).puncture(-1).or(text.length)
        val element = text.s.substring(start, end).nn.tt
        val start2 = end + navigable.separator.length

        if element == navigable.parentElement then
          if elements.isEmpty then recur(start2, ascent + 1, Nil)
          else recur(start2, ascent, elements.tail)
        else recur(start2, ascent, navigable.element(element) :: elements)
    
    if text == navigable.selfText then Relative(0, Nil) else recur(0, 0, Nil)

  def apply[ElementType](using navigable: Navigable by ElementType)
      (ascent0: Int, descent0: List[ElementType])
          : Relative by ElementType =
    Relative.from[ElementType](ascent0, descent0.map(navigable.makeElement(_)), navigable.separator)

  private def from[ElementType](ascent0: Int, descent0: List[Text], separator: Text)
          : Relative by ElementType =
    new Relative(ascent0, descent0, separator):
      type Operand = ElementType

  given [ElementType] => (Relative by ElementType) is Addable by (Relative by ElementType) into
          (Relative by ElementType) =
    (left, right) =>
      def recur(ascent: Int, descent: List[Text], ascent2: Int): Relative by ElementType =
        if ascent2 > 0 then
          if descent.isEmpty then recur(ascent + 1, Nil, ascent - 1)
          else recur(ascent, descent.tail, ascent - 1)
        else Relative.from(ascent, right.textDescent ++ descent, left.separator)

      recur(left.ascent, left.textDescent, right.ascent)
        
abstract class Relative(val ascent: Int, val textDescent: List[Text], val separator: Text)
extends Pathlike:
  type Operand

  def delta: Int = textDescent.length - ascent

  def parent: Relative =
    if textDescent.isEmpty then Relative.from(ascent + 1, Nil, separator)
    else Relative.from(ascent, textDescent.tail, separator)

  override def equals(that: Any): Boolean = that.asMatchable match
    case that: Relative => that.ascent == ascent && that.textDescent == textDescent
    case _              => false
  
  override def hashCode: Int = ascent*31 + textDescent.hashCode

  @targetName("child")
  infix def / (element: Operand)(using navigable: Navigable by Operand): Relative by Operand =
    Relative.from(ascent, navigable.makeElement(element) :: textDescent, separator)
