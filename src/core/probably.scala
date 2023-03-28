/*
    Probably, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package probably

import eucalyptus.*
import gossamer.*
import rudiments.*
import deviation.*

import scala.collection.mutable as scm

given realm: Realm = Realm(t"probably")

extension [T](inline value: T)(using inline test: TestContext)
  inline def inspect(using Debug[T]): T = ${ProbablyMacros.inspect('value, 'test)}

package testContexts:
  given threadLocal: TestContext = new TestContext():
    private val delegate: Option[TestContext] = Option(Runner.testContextThreadLocal.get()).map(_.nn).flatten
    
    override def capture[T](name: Text, value: T)(using Debug[T]): T =
      delegate.map(_.capture[T](name, value)).getOrElse(value)

@annotation.capability
class TestContext():
  private[probably] val captured: scm.ArrayBuffer[(Text, Text)] = scm.ArrayBuffer()
  
  def capture[T](name: Text, value: T)(using Debug[T]): T =
    captured.append(name -> value.debug)
    value

object TestId:
  given Ordering[TestId] = math.Ordering.Implicits.seqOrdering[List, Text].on(_.ids.reverse)

case class TestId(name: Text, suite: Maybe[TestSuite], codepoint: Codepoint):
  import textWidthCalculation.uniform
  lazy val id: Text = Integer.toHexString(suite.hashCode ^ name.hashCode).nn.show.pad(6, Rtl, '0').take(6, Rtl)
  lazy val ids: List[Text] =  id :: suite.mm(_.id.ids).or(Nil)
  def apply[T](ctx: TestContext ?=> T): Test[T] = Test[T](this, ctx(using _))
  def depth: Int = suite.mm(_.id.depth).or(0) + 1

class TestSuite(val name: Text, val parent: Maybe[TestSuite] = Unset)(using codepoint: Codepoint):
  override def equals(that: Any): Boolean = that.matchable(using Unsafe) match
    case that: TestSuite => name == that.name && parent == that.parent
    case _               => false
  
  override def hashCode: Int = name.hashCode + parent.hashCode

  val id: TestId = TestId(name, parent, codepoint)

enum Outcome:
  case Pass(duration: Long)
  case Fail(duration: Long)
  case Throws(exception: Exception, duration: Long)
  case CheckThrows(exception: Exception, duration: Long)

  def duration: Long

enum TestRun[+T]:
  case Returns(result: T, duration: Long, context: Map[Text, Text])
  case Throws(exception: () => T, duration: Long, context: Map[Text, Text])

  def get: T = this match
    case Returns(result, _, _)   => result
    case Throws(exception, _, _) => exception()

object Runner:
  private[probably] val testContextThreadLocal: ThreadLocal[Option[TestContext]] = ThreadLocal()

class Runner[ReportType]()(using reporter: TestReporter[ReportType]):
  def skip(id: TestId): Boolean = false
  val report: ReportType = reporter.make()

  def maybeRun[T, S](test: Test[T]): Maybe[TestRun[T]] =
    if skip(test.id) then Unset else run[T, S](test)

  def run[T, S](test: Test[T]): TestRun[T] =
    val ctx = TestContext()
    Runner.testContextThreadLocal.set(Some(ctx))
    val ns0 = System.nanoTime
    
    try
      val ns0: Long = System.nanoTime
      val result: T = test.action(ctx)
      val ns: Long = System.nanoTime - ns0
      TestRun.Returns(result, ns, ctx.captured.to(Map))
    
    catch case err: Exception =>
      val ns: Long = System.nanoTime - ns0
      
      val lazyException = () =>
        import unsafeExceptions.canThrowAny
        throw err

      TestRun.Throws(lazyException, ns, ctx.captured.to(Map))
    finally Runner.testContextThreadLocal.set(None)

  def suite(suite: TestSuite, fn: TestSuite ?=> Unit): Unit =
    if !skip(suite.id) then
      reporter.declareSuite(report, suite)
      fn(using suite)
  
  def complete(): Unit = reporter.complete(report)

case class Test[+Return](id: TestId, action: TestContext => Return)

def test[ReportType](name: Text)(using suite: TestSuite, codepoint: Codepoint): TestId =
  TestId(name, suite, codepoint)

def suite
    [ReportType](name: Text)(using suite: TestSuite, runner: Runner[ReportType])
    (fn: TestSuite ?=> Unit): Unit =
  runner.suite(TestSuite(name, suite), fn)

extension [TestType](test: Test[TestType])
  inline def aspire[ReportType]
      (inline pred: TestType => Boolean)
      (using runner: Runner[ReportType], inc: Inclusion[ReportType, Outcome],
          inc2: Inclusion[ReportType, DebugInfo])
      : Unit =
    ${ProbablyMacros.aspire[TestType, ReportType]('test, 'runner, 'inc, 'inc2)}
  
  inline def assert[ReportType]
      (inline pred: TestType => Boolean)
      (using runner: Runner[ReportType], inc: Inclusion[ReportType, Outcome],
          inc2: Inclusion[ReportType, DebugInfo])
      : Unit =
    ${ProbablyMacros.assert[TestType, ReportType]('test, 'pred, 'runner, 'inc, 'inc2)}
  
  inline def check[ReportType]
      (inline pred: TestType => Boolean)
      (using runner: Runner[ReportType], inc: Inclusion[ReportType, Outcome],
          inc2: Inclusion[ReportType, DebugInfo])
      : TestType =
    ${ProbablyMacros.check[TestType, ReportType]('test, 'pred, 'runner, 'inc, 'inc2)}

  inline def assert[ReportType]()
      (using runner: Runner[ReportType], inc: Inclusion[ReportType, Outcome],
          inc2: Inclusion[ReportType, DebugInfo])
      : Unit =
    ${ProbablyMacros.assert[TestType, ReportType]('test, '{ProbablyMacros.succeed}, 'runner, 'inc, 'inc2)}
  
  inline def check[ReportType]()
      (using runner: Runner[ReportType], inc: Inclusion[ReportType, Outcome],
          inc2: Inclusion[ReportType, DebugInfo])
      : TestType =
    ${ProbablyMacros.check[TestType, ReportType]('test, '{ProbablyMacros.succeed}, 'runner, 'inc, 'inc2)}
  
  inline def matches[ReportType]
      (inline pf: PartialFunction[TestType, Any])
      (using runner: Runner[ReportType], inc: Inclusion[ReportType, Outcome],
          inc2: Inclusion[ReportType, DebugInfo])
      : Unit =
    assert[ReportType](pf.isDefinedAt(_))
  
case class UnexpectedSuccessError(value: Any)
extends Error(err"the expression was expected to throw an exception, but instead returned $value")

transparent inline def capture
    [ExceptionType <: Exception](inline fn: => CanThrow[ExceptionType] ?=> Any)
    : ExceptionType throws UnexpectedSuccessError =
  try
    val result = fn(using unsafeExceptions.canThrowAny)
    throw UnexpectedSuccessError(result)
  catch
    case error: ExceptionType          => error
    case error: UnexpectedSuccessError => throw error
