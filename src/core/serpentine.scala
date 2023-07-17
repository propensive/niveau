/*
    Serpentine, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package serpentine

import rudiments.*
import spectacular.*
import gossamer.*
import kaleidoscope.*

import scala.compiletime.*
import scala.quoted.*

import language.experimental.captureChecking

object Serpentine:
  opaque type PathName[NameType <: Label] = String

  object PathName:
    given [NameType <: Label]: Show[PathName[NameType]] = Text(_)

    inline def apply[NameType <: Label](text: Text): PathName[NameType] =
      ${runtimeParse[NameType]('text)}

  extension [NameType <: Label](pathName: PathName[NameType])
    def render: Text = Text(pathName)
    def widen[NameType2 <: NameType]: PathName[NameType2] = pathName
    inline def narrow[NameType2 >: NameType <: Label]: PathName[NameType2] = PathName(render)
  
  def runtimeParse
      [NameType <: Label: Type]
      (text: Expr[Text])(using Quotes)
      : Expr[PathName[NameType]] =
    import quotes.reflect.*

    val checks: List[String] = patterns(TypeRepr.of[NameType])
    
    def recur(patterns: List[String], statements: Expr[Unit]): Expr[Unit] = patterns match
      case pattern :: tail =>
        import PathError.Reason.*
        
        def reasonExpr: Expr[PathError.Reason] = pattern match
          case r"\.\*\\?$char(.)\.\*"       => '{InvalidChar(${Expr(char.head)})}
          case r"$prefix([a-zA-Z0-9]*)\.\*" => '{InvalidPrefix(Text(${Expr(prefix.toString)}))}
          case r"\.\*$suffix([a-zA-Z0-9]*)" => '{InvalidSuffix(Text(${Expr(suffix.toString)}))}
          case other                        => '{InvalidName(Text(${Expr(pattern)}))}
        
        recur(tail, '{
          $statements
          if $text.s.matches(${Expr(pattern)}) then
            given CanThrow[PathError] = unsafeExceptions.canThrowAny
            throw PathError($reasonExpr)
        })
      
      case _ =>
        statements

    '{
      ${recur(checks, '{()})}
      $text.asInstanceOf[PathName[NameType]]
    }

  private[serpentine] def patterns
      (using quotes: Quotes)(repr: quotes.reflect.TypeRepr)
      : List[String] =
    import quotes.reflect.*
    
    (repr.dealias.asMatchable: @unchecked) match
      case OrType(left, right)                   => patterns(left) ++ patterns(right)
      case ConstantType(StringConstant(pattern)) => List(pattern)

  @targetName("Root")
  object `%`:
    erased given hierarchy
        [PathType <: Matchable, LinkType <: Matchable]
        (using erased hierarchy: Hierarchy[PathType, LinkType])
        : Hierarchy[%.type, LinkType] = ###

    override def equals(other: Any): Boolean = other.asMatchable match
      case anyRef: AnyRef         => (anyRef eq %) || {
        anyRef match
          case other: PathEquality[?] => other.equals(this)
          case other                  => false
      }
      case _                      => false
    
    override def hashCode: Int = 0

    def precedes
        [PathType <: Matchable]
        (using erased hierarchy: Hierarchy[PathType, ?])
        (path: PathType)
        : Boolean =
      true

    given reachable
        [PathType <: Matchable, LinkType <: Matchable, NameType <: Label, RootType]
        (using erased hierarchy: Hierarchy[PathType, LinkType])
        (using reachable: Reachable[PathType, NameType, RootType])
        (using mainRoot: MainRoot[PathType])
        : Reachable[%.type, NameType, RootType] =
      new Reachable[%.type, NameType, RootType]:
        def separator(path: %.type): Text = reachable.separator(mainRoot.empty())
        def prefix(root: RootType): Text = reachable.prefix(reachable.root(mainRoot.empty()))
        def root(path: %.type): RootType = reachable.root(mainRoot.empty())
        def descent(path: %.type): List[PathName[NameType]] = Nil
    
    given show
        [PathType <: Matchable]
        (using hierarchy: Hierarchy[PathType, ?])
        (using mainRoot: MainRoot[PathType], show: Show[PathType]): Show[%.type] = root =>
      mainRoot.empty().show
      

    @targetName("child")
    def /
        [PathType <: Matchable, NameType <: Label, AscentType]
        (using hierarchy: Hierarchy[PathType, ?])
        (using mainRoot: MainRoot[PathType])
        (using pathlike: Pathlike[PathType, NameType, AscentType])
        (name: PathName[NameType])
        (using creator: PathCreator[PathType, NameType, AscentType])
        : PathType =
      mainRoot.empty() / name

export Serpentine.%

object SerpentineMacro:
  def parse
      [NameType <: Label: Type](context: Expr[StringContext])(using Quotes)
      : Expr[PExtractor[NameType]] =
    import quotes.reflect.*
    
    val (element: String, pos: Position) = (context: @unchecked) match
      case '{StringContext(${Varargs(Seq(str))}*)} => (str.value.get, str.asTerm.pos)
    
    Serpentine.patterns(TypeRepr.of[NameType]).foreach: pattern =>
      if element.matches(pattern) then pattern match
        case r"\.\*\\?$char(.)\.\*" =>
          fail(msg"a path element may not contain the character $char", pos)

        case r"$start([a-zA-Z0-9]*)\.\*" =>
          fail(msg"a path element may not start with $start", pos)

        case r"\.\*$end([a-zA-Z0-9]*)" =>
          fail(msg"a path element may not end with $end", pos)

        case pattern@r"[a-zA-Z0-9]*" =>
          fail(msg"a path element may not be $pattern", pos)

        case other =>
          fail(msg"a path element may not match the pattern $other")

    '{
      new PExtractor[NameType]():
        def apply(): PathName[NameType] = ${Expr(element)}.asInstanceOf[PathName[NameType]]
        def unapply(name: PathName[NameType]): Boolean = name.render.s == ${Expr(element)}
    }
