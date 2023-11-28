package perforate

import fulminate.*
import rudiments.*

import scala.quoted.*

//import language.experimental.captureChecking

transparent inline def mitigate
    [ErrorType <: Error]
    (inline mitigation: PartialFunction[Throwable, ErrorType])
    : Mitigator[ErrorType] =
  ${Macros.mitigate[ErrorType]('mitigation)}

trait Mitigator[+ErrorType]:
  type Context[+ResultType]

  def within[ResultType](block: Context[ResultType]): ResultType

object Macros:
  def mitigate
      [ErrorType <: Error: Type]
      (handlers: Expr[PartialFunction[Throwable, ErrorType]])
      (using Quotes)
      : Expr[Mitigator[ErrorType]] =
    import quotes.reflect.*

    type Q = (Int, Int)

    def exhaustive(pattern: Tree, patternType: TypeRepr): Boolean =
     pattern match
      case Wildcard()          => true
      case Typed(_, matchType) => patternType <:< matchType.tpe
      case Bind(_, pattern)    => exhaustive(pattern, patternType)

      case TypedOrTest(Unapply(Select(target, method), _, params), _) =>
        params.zip(patternType.typeSymbol.caseFields.map(_.info.typeSymbol.typeRef)).forall(exhaustive) ||
            fail(msg"this pattern will not match every ${patternType.show}")
        
      case Unapply(Select(target, method), _, params) =>
        params.zip(patternType.typeSymbol.caseFields.map(_.info.typeSymbol.typeRef)).forall(exhaustive) ||
            fail(msg"this pattern will not match every ${patternType.show}")
      
      case other => fail(msg"this pattern will not match every ${patternType.show}")
        

    def patternType(pattern: Tree): List[TypeRepr] = pattern match
      case Wildcard()          => Nil
      case Typed(_, matchType) => List(matchType.tpe)
      case Bind(_, pattern)    => patternType(pattern)
      
      case TypedOrTest(Unapply(Select(target, method), _, _), typeTree) =>
        target.tpe.typeSymbol.methodMember(method).head.info match
          case MethodType(_, _, unapplyType) =>
            if exhaustive(pattern, typeTree.tpe) then List(typeTree.tpe) else Nil
          case _ =>
            Nil
      case other =>
        Nil

    val patternTypes: List[(TypeRepr, TypeRepr)] = handlers.asTerm match
      case Inlined(_, _, Block(List(defDef), term)) => defDef match
        case DefDef(ident, scrutineeType, returnType, Some(Match(matchId, caseDefs))) => caseDefs.flatMap:
          case CaseDef(pattern, None, rhs) =>
            rhs.asExpr match
              case '{$rhs: rhsType} => patternType(pattern).map((_, TypeRepr.of[rhsType]))
          case _ => Nil
        case _ =>
          Nil
      case _ =>
        Nil
    
    patternTypes.map: (left, right) =>
      println(s"${left.show} -> ${right.show}")

    val raiseTypes = patternTypes.map(_(0)).map(_.asType).map:
      case '[type errorType <: Error; errorType] => TypeRepr.of[Raises[errorType]]

    TypeLambda(List("T"), _ => List(TypeBounds.empty), lambda =>
      AppliedType(defn.FunctionClass(raiseTypes.length, true).typeRef, raiseTypes ::: List(lambda.param(0)))
    ).asType match
      case '[type contextType[+_]; contextType] => '{
        new Mitigator[ErrorType]:
          type Context[+ResultType] = contextType[ResultType]
          def within[ResultType](block: contextType[ResultType]): ResultType = ???
      }







    // TypeLambda(List("T"), _ => List(TypeBounds.empty), lambda =>
    //   AppliedType(defn.FunctionClass(raises.length, true).typeRef, raises ::: List(lambda.param(0)))
    // ).asType match
    //   case '[type contextType[+_]; contextType] => '{
    //     new Mitigator[ErrorType]:
    //       type Context[+ResultType] = contextType[ResultType]

    //       def within[ResultType](block: contextType[ResultType]): ResultType = ${
    //         def nestedContexts(patternTypes: List[(TypeRepr, TypeRepr)]): Expr[contextType[ResultType]] =
    //           patternTypes match
    //             case Nil =>
    //               '{
    //                 block
    //               }
    //             case (left, right) :: tail =>
    //               right.asType match
    //                 case '[type targetType <: Error; targetType] => left.asType match
    //                   case '[type sourceType <: Error; sourceType] =>
    //                     val raises = Expr.summon[Raises[targetType]].getOrElse:
    //                       fail(msg"can't raise a ${right.show}")
                            
    //                       $raises.contraMap[sourceType] { error => $handlers(error).asInstanceOf[targetType] }

    //                     '{
    //                       given Raises[sourceType] =
    //                         $raises.contraMap[sourceType] { error => $handlers(error).asInstanceOf[targetType] }
    //                       ${nestedContexts(tail)}
    //                     }
    //         '{${nestedContexts(patternTypes)}.asInstanceOf[ResultType]}
    //       }
            
    //   }.asExprOf[Mitigator[ErrorType] { type Context[+ResultType] = contextType[ResultType] }]
