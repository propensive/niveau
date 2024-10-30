package scintillate

import anticipation.*
import contingency.*
import denominative.*
import fulminate.*
import gossamer.*
import rudiments.*
import spectacular.*
import hieroglyph.*
import prepositional.*
import vacuous.*
import turbulence.*

import scala.reflect.*
import scala.collection.mutable as scm

object MultipartError:
  enum Reason:
    case Expected(char: Char)
    case StreamContinues, BadBoundaryEnding, MediaType, BadDisposition

  given Reason is Communicable =
    case Reason.Expected(char: Char) => m"the character '$char' was expected"
    case Reason.StreamContinues      => m"the stream continues"
    case Reason.BadBoundaryEnding    => m"unexpected content followed the boundary"
    case Reason.MediaType            => m"the media type is invalid"
    case Reason.BadDisposition       => m"the `Content-Disposition` header has the wrong format"

import MultipartError.Reason

case class MultipartError(reason: MultipartError.Reason)(using Diagnostics)
extends Error(m"The multipart data could not be read because $reason")

object Multipart:

  enum Disposition:
    case Inline, Attachment, FormData

  def parse(stream: LazyList[Bytes], boundary0: Optional[Text] = Unset)
          : Multipart raises MultipartError =
    val conduit = Conduit(stream)
    conduit.mark()
    conduit.next()
    if conduit.datum != '-' then raise(MultipartError(Reason.Expected('-')))
    conduit.next()
    if conduit.datum != '-' then raise(MultipartError(Reason.Expected('-')))
    conduit.seek('\r')
    val boundary = conduit.save()
    conduit.next()
    if conduit.datum != '\n' then raise(MultipartError(Reason.Expected('\n')))
    conduit.next()

    def headers(list: List[(Text, Text)]): Map[Text, Text] =
      conduit.datum match
        case '\r' =>
          conduit.next()
          if conduit.datum != '\n' then raise(MultipartError(Reason.Expected('\n')))
          conduit.break()
          conduit.cue()
          list.to(Map)

        case other =>
          conduit.mark()
          conduit.seek(':')
          val key = Text.ascii(conduit.save())
          conduit.next()
          if conduit.datum != ' ' then raise(MultipartError(Reason.Expected(' ')))
          conduit.next()
          conduit.mark()
          conduit.seek('\r')
          val value = Text.ascii(conduit.save())
          conduit.next()
          if conduit.datum != '\n' then raise(MultipartError(Reason.Expected('\n')))
          conduit.next()
          headers((key, value) :: list)

    def body(): LazyList[Bytes] = conduit.step() match
      case Conduit.State.Clutch => conduit.block #:: body()
      case Conduit.State.Data   => conduit.datum match
        case '\r' =>
          if conduit.lookahead:
            conduit.next() && conduit.datum == '\n' && boundary.forall: char =>
              conduit.next() && conduit.datum == char
          then
            conduit.breakBefore()
            LazyList(conduit.block).also:
              conduit.skip(boundary.length + 3)
          else body()
        case other =>
          body()

    def parsePart(headers: Map[Text, Text], stream: LazyList[Bytes]): Part =
      headers.at(t"Content-Disposition").let: disposition =>
        val parts = disposition.cut(t";").map(_.trim)

        val params: Map[Text, Text] =
          parts.drop(1).map: param =>
            param.cut(t"=", 2) match
              case List(key, value) => key -> value
              case _                => raise(MultipartError(Reason.BadDisposition)) yet (t"", t"")
          .to(Map)

        val dispositionValue = parts.prim match
          case t"inline"     => Multipart.Disposition.Inline
          case t"form-data"  => Multipart.Disposition.FormData
          case t"attachment" => Multipart.Disposition.Attachment
          case _ =>
            raise(MultipartError(Reason.BadDisposition)) yet Multipart.Disposition.FormData

        val filename = params.at(t"filename")
        val name = params.at(t"name")

        Part(dispositionValue, headers, name, filename, stream)
      .or(Part(Multipart.Disposition.FormData, Map(), Unset, Unset, stream))

    def parts(): LazyList[Part] =
      val part = parsePart(headers(Nil), body())

      conduit.datum match
        case '\r' =>
          if !conduit.next() || conduit.datum != '\n' then raise(MultipartError(Reason.Expected('\n')))
          part #:: { part.body.strict; conduit.next(); parts() }

        case '-' =>
          if !conduit.next() || conduit.datum != '-' then raise(MultipartError(Reason.Expected('-')))
          if !conduit.next() || conduit.datum != '\r' then raise(MultipartError(Reason.Expected('\r')))
          if !conduit.next() || conduit.datum != '\n' then raise(MultipartError(Reason.Expected('\n')))
          //if conduit.next() then raise(MultipartError(Reason.StreamContinues))
          LazyList(part)
        case other =>
          raise(MultipartError(Reason.Expected('-')))
          LazyList()

    Multipart(parts())

case class Multipart(parts: LazyList[Part]):
  def at(name: Text): Optional[Part] = parts.find(_.name == name).getOrElse(Unset)


object Part:
  given Part is Readable by Bytes as readable = _.body

case class Part
    (disposition: Multipart.Disposition,
     headers:     Map[Text, Text],
     name:        Optional[Text],
     filename:    Optional[Text],
     body:        LazyList[Bytes])
