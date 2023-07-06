/*
    Telekinesis, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package telekinesis

import serpentine.*
import gossamer.*
import rudiments.*
import digression.*
import escapade.*
import iridescence.*
import anticipation.*
import contextual.*
import spectacular.*

object Host:
  given Show[Host] = _.parts.join(t".")
  
  def parse(str: Text): Host =
    val parts: List[Text] = str.cut(t".").map(_.punycode)
    Host(parts*)

case class Host(parts: Text*) extends Shown[Host]

object Scheme:
  given Show[Scheme] = _.name
  object Http extends Scheme(t"http")
  object Https extends Scheme(t"https")

case class Scheme(name: Text)

case class Raw(text: Text)

object UrlInput:
  given Substitution[UrlInput, Text, "x"] = UrlInput.Textual(_)
  given Substitution[UrlInput, Raw, "x"] = raw => UrlInput.RawTextual(raw.text)
  given Substitution[UrlInput, Int, "80"] = UrlInput.Integral(_)

enum UrlInput:
  case Integral(value: Int)
  case Textual(value: Text)
  case RawTextual(value: Text)

object UrlInterpolator extends contextual.Interpolator[UrlInput, Text, Url]:
  def complete(value: Text): Url =
    try Url.parse(value) catch case err: UrlError =>
      throw InterpolationError(msg"the URL ${err.text} is not valid: expected ${err.expected}", err.offset)
  
  def initial: Text = t""
  
  def insert(state: Text, value: UrlInput): Text =
    value match
      case UrlInput.Integral(port) =>
        if !state.ends(t":") then throw InterpolationError(msg"a port number must be specified after a colon")
        
        try Url.parse(state+port.show)
        catch case err: UrlError => throw InterpolationError(Message(err.message.text))
        
        state+port.show
      
      case UrlInput.Textual(txt) =>
        if !state.ends(t"/") then throw InterpolationError(msg"a substitution may only be made after a slash")
        
        try Url.parse(state+txt.urlEncode)
        catch case err: UrlError => throw InterpolationError(Message(err.message.text))
        
        state+txt.urlEncode
      
      case UrlInput.RawTextual(txt) =>
        if !state.ends(t"/") then throw InterpolationError(msg"a substitution may only be made after a slash")

        try Url.parse(state+txt.urlEncode)
        catch case err: UrlError => throw InterpolationError(Message(err.message.text))
        
        state+txt
  
  override def substitute(state: Text, sub: Text): Text = state+sub

  def parse(state: Text, next: Text): Text =
    // FIXME: Should use `.empty` instead of `.s.isEmpty`
    if !state.s.isEmpty && !(next.starts(t"/") || next.s.isEmpty)
    then throw InterpolationError(msg"a substitution must be followed by a slash")
    
    state+next
  
  def skip(state: Text): Text = state+t"1"

object Url:
  given (using CanThrow[UrlError]): GenericUrl[Url] = new GenericUrl[Url]:
    def readUrl(url: Url): String = url.show.s
    def makeUrl(value: String): Url = Url.parse(value.show)

  given GenericHttpRequestParam["location", Url] = show(_).s

  // given (using CanThrow[UrlError]): Canonical[Url] = Canonical(parse(_), _.show)

  given Reachable[Url, "", (Scheme, Maybe[Authority])] with
    def separator(url: Url): Text = t"/"
    def descent(url: Url): List[PathName[""]] = url.path
    def root(url: Url): (Scheme, Maybe[Authority]) = (url.scheme, url.authority)
    
    def prefix(root: (Scheme, Maybe[Authority])): Text =
      t"${root(0).name}:${root(1).mm(t"//"+_.show).or(t"")}"
    
  given PathCreator[Url, "", (Scheme, Maybe[Authority])] with
    def path(ascent: (Scheme, Maybe[Authority]), descent: List[PathName[""]]): Url =
      Url(ascent(0), ascent(1), descent.reverse.map(_.render).join(t"/"))
    

  // There's not a convenient way to have this in scope if the HTTP client is defined separately from the URL
  // given (using Internet, Log, CanThrow[HttpError]): Streamable[Url] =
  //   url => LazyList(url.get().as[Bytes])
  
  given show: Show[Url] = url =>
    val auth = url.authority.fm(t"")(t"//"+_.show)
    val rest = t"${url.query.fm(t"")(t"?"+_)}${url.fragment.fm(t"")(t"#"+_)}"
    t"${url.scheme}:$auth${url.pathText}$rest"
  
  given ansiShow: Display[Url] = url => out"$Underline(${colors.DeepSkyBlue}(${show(url)}))"

  given action: GenericHtmlAttribute["action", Url] with
    def name: String = "action"
    def serialize(url: Url): String = url.show.s
  
  given codebase: GenericHtmlAttribute["codebase", Url] with
    def name: String = "codebase"
    def serialize(url: Url): String = url.show.s
  
  given cite: GenericHtmlAttribute["cite", Url] with
    def name: String = "cite"
    def serialize(url: Url): String = url.show.s
  
  given data: GenericHtmlAttribute["data", Url] with
    def name: String = "data"
    def serialize(url: Url): String = url.show.s

  given formaction: GenericHtmlAttribute["formaction", Url] with
    def name: String = "formaction"
    def serialize(url: Url): String = url.show.s
 
  given poster: GenericHtmlAttribute["poster", Url] with
    def name: String = "poster"
    def serialize(url: Url): String = url.show.s

  given src: GenericHtmlAttribute["src", Url] with
    def name: String = "src"
    def serialize(url: Url): String = url.show.s
  
  given href: GenericHtmlAttribute["href", Url] with
    def name: String = "href"
    def serialize(url: Url): String = url.show.s
  
  given manifest: GenericHtmlAttribute["manifest", Url] with
    def name: String = "manifest"
    def serialize(url: Url): String = url.show.s

  def parse(value: Text): Url throws UrlError =
    import UrlError.Expectation.*

    safely(value.where(_ == ':')) match
      case Unset =>
        throw UrlError(value, value.length, Colon)
      
      case colon: Int =>
        val scheme = Scheme(value.take(colon))
        
        val (pathStart, auth) =
          if value.slice(colon + 1, colon + 3) == t"//" then
            val authEnd = safely(value.where(_ == '/', colon + 3)).or(value.length)
            authEnd -> Authority.parse(value.slice(colon + 3, authEnd))
          else
            (colon + 1) -> Unset
        
        safely(value.where(_ == '?', pathStart)) match
          case Unset => safely(value.where(_ == '#', pathStart)) match
            case Unset     => Url(scheme, auth, value.drop(pathStart), Unset, Unset)
            case hash: Int => Url(scheme, auth, value.slice(pathStart, hash), Unset, value.drop(hash + 1))

          case qmark: Int =>
            safely(value.where(_ == '#', qmark + 1)) match
              case Unset     => Url(scheme, auth, value.slice(pathStart, qmark), value.drop(qmark + 1), Unset)
              case hash: Int => Url(scheme, auth, value.slice(pathStart, qmark), value.slice(qmark + 1, hash),
                                    value.drop(hash + 1))

object Authority:
  given Show[Authority] = auth =>
    t"${auth.userInfo.fm(t"")(_+t"@")}${auth.host}${auth.port.mm(_.show).fm(t"")(t":"+_)}"

  def parse(value: Text): Authority throws UrlError =
    import UrlError.Expectation.*
    
    safely(value.where(_ == '@')) match
      case Unset => safely(value.where(_ == ':')) match
        case Unset =>
          Authority(Host.parse(value))
        
        case colon: Int =>
          safely(value.drop(colon + 1).s.toInt).match
            case port: Int if port >= 0 && port <= 65535 => port
            case port: Int                               => throw UrlError(value, colon + 1, PortRange)
            case Unset                                   => throw UrlError(value, colon + 1, Number)
          .pipe(Authority(Host.parse(value.take(colon)), Unset, _))
      
      case arobase: Int => safely(value.where(_ == ':', arobase + 1)) match
        case Unset =>
          Authority(Host.parse(value.drop(arobase + 1)), value.take(arobase))

        case colon: Int =>
          safely(value.drop(colon + 1).s.toInt).match
            case port: Int if port >= 0 && port <= 65535 => port
            case port: Int                               => throw UrlError(value, colon + 1, PortRange)
            case Unset                                   => throw UrlError(value, colon + 1, Number)
          .pipe(Authority(Host.parse(value.slice(arobase + 1, colon)), value.take(arobase), _))

case class Authority(host: Host, userInfo: Maybe[Text] = Unset, port: Maybe[Int] = Unset)

object Link:
  given Followable[Link, "", "..", "."] with
    def separators: Set[Char] = Set('/')
    def descent(link: Link): List[PathName[""]] = link.descent
    def separator(link: Link): Text = t"/"
    def ascent(link: Link): Int = link.ascent
    
  given PathCreator[Link, "", Int] with
    def path(ascent: Int, descent: List[PathName[""]]): Link = Link(ascent, descent)

case class Link(ascent: Int, descent: List[PathName[""]])

case class Url(scheme: Scheme, authority: Maybe[Authority], pathText: Text, query: Maybe[Text] = Unset,
                   fragment: Maybe[Text] = Unset):
  
  lazy val path: List[PathName[""]] =
    pathText.drop(1).cut(t"/").reverse.map(_.urlDecode).map(PathName(_))

object UrlError:
  enum Expectation:
    case Colon, More, LowerCaseLetter, PortRange, Number

  object Expectation:
    given AsMessage[Expectation] =
      case Colon           => msg"a colon"
      case More            => msg"more characters"
      case LowerCaseLetter => msg"a lowercase letter"
      case PortRange       => msg"a port range"
      case Number          => msg"a number"

case class UrlError(text: Text, offset: Int, expected: UrlError.Expectation)
extends Error(msg"the URL $text is not valid: expected $expected at $offset")
