package serpentine

import rudiments.*
import kaleidoscope.*
import anticipation.*
import gossamer.*

import scala.quoted.*
import scala.compiletime.*

object SerpentineMacro:
  def parse
      [NameType <: Label: Type](context: Expr[StringContext])(using Quotes)
      : Expr[PExtractor[NameType]] =
    import quotes.reflect.*
    
    val (element: String, pos: Position) = (context: @unchecked) match
      case '{StringContext(${Varargs(Seq(str))}*)} => (str.value.get, str.asTerm.pos)
    
    patterns(TypeRepr.of[NameType]).foreach: pattern =>
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
  
  private[serpentine] def patterns
      (using quotes: Quotes)(repr: quotes.reflect.TypeRepr)
      : List[String] =
    import quotes.reflect.*
    
    (repr.dealias.asMatchable: @unchecked) match
      case OrType(left, right)                   => patterns(left) ++ patterns(right)
      case ConstantType(StringConstant(pattern)) => List(pattern)

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
