/*
    Probably, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

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

import gossamer.*
import fulminate.*
import anticipation.*
import rudiments.*
import hypotenuse.*
import vacuous.*
import digression.*
import spectacular.*
import hieroglyph.*

import scala.collection.mutable as scm

given realm: Realm = realm"probably"

extension [ValueType](inline value: ValueType)(using inline test: TestContext)
  inline def debug: ValueType = ${Probably.debug('value, 'test)}

package testContexts:
  given threadLocal: TestContext = new TestContext():
    private val delegate: Option[TestContext] = Option(Runner.testContextThreadLocal.get()).map(_.nn).flatten

    override def capture[ValueType: Inspectable](name: Text, value: ValueType): ValueType =
      delegate.map(_.capture[ValueType](name, value)).getOrElse(value)

@annotation.capability
class TestContext():
  private[probably] val captured: scm.ArrayBuffer[(Text, Text)] = scm.ArrayBuffer()

  def capture[ValueType: Inspectable](name: Text, value: ValueType): ValueType =
    captured.append(name -> value.inspect)
    value

object TestId:
  given Ordering[TestId] = math.Ordering.Implicits.seqOrdering[List, Text].on(_.ids.reverse)

case class TestId(name: Text, suite: Optional[TestSuite], codepoint: Codepoint):
  val timestamp: Long = System.currentTimeMillis
  import textMetrics.uniform
  lazy val id: Text = (suite.hashCode ^ name.hashCode).hex.pad(6, Rtl, '0').keep(6, Rtl)
  lazy val ids: List[Text] =  id :: suite.let(_.id.ids).or(Nil)

  def apply[ResultType](ctx: TestContext ?=> ResultType): Test[ResultType] =
    Test[ResultType](this, ctx(using _))

  def depth: Int = suite.let(_.id.depth).or(0) + 1

class TestSuite(val name: Text, val parent: Optional[TestSuite] = Unset)(using codepoint: Codepoint):
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

  val timestamp: Long = System.currentTimeMillis
  def duration: Long

enum TestRun[+T]:
  case Returns(result: T, duration: Long, context: Map[Text, Text])
  case Throws(exception: () => Nothing, duration: Long, context: Map[Text, Text])

  def get: T = this match
    case Returns(result, _, _)   => result
    case Throws(exception, _, _) => exception()

object Runner:
  private[probably] val testContextThreadLocal: ThreadLocal[Option[TestContext]] = ThreadLocal()

class Runner[ReportType]()(using reporter: TestReporter[ReportType]):
  private var active: Set[TestId] = Set()
  def skip(id: TestId): Boolean = false
  val report: ReportType = reporter.make()

  def maybeRun[T, S](test: Test[T]): Optional[TestRun[T]] = if skip(test.id) then Unset else run[T, S](test)

  def run[T, S](test: Test[T]): TestRun[T] =
    synchronized { active += test.id }
    val ctx = TestContext()
    Runner.testContextThreadLocal.set(Some(ctx))
    val ns0 = System.nanoTime

    try
      val ns0: Long = System.nanoTime
      val result: T = test.action(ctx)
      val ns: Long = System.nanoTime - ns0
      TestRun.Returns(result, ns, ctx.captured.to(Map)).also:
        synchronized { active -= test.id }

    catch case err: Exception =>
      val ns: Long = System.nanoTime - ns0

      def lazyException(): Nothing =
        given CanThrow[Exception] = unsafeExceptions.canThrowAny
        throw err

      TestRun.Throws(lazyException, ns, ctx.captured.to(Map)).also:
        synchronized { active -= test.id }

    finally
      Runner.testContextThreadLocal.set(None)

  def suite(suite: TestSuite, block: TestSuite ?=> Unit): Unit =
    if !skip(suite.id) then
      reporter.declareSuite(report, suite)
      block(using suite)

  def terminate(error: Throwable): Unit = synchronized:
    reporter.fail(report, error, active)

  def complete(): Unit = reporter.complete(report)

case class Test[+ReturnType](id: TestId, action: TestContext => ReturnType)

def test[ReportType](name: Text)(using suite: TestSuite, codepoint: Codepoint): TestId =
  TestId(name, suite, codepoint)

def suite[ReportType](name: Text)(using suite: TestSuite, runner: Runner[ReportType])
   (block: TestSuite ?=> Unit)
        : Unit =

  runner.suite(TestSuite(name, suite), block)

extension [TestType](test: Test[TestType])
  inline def aspire[ReportType](inline predicate: TestType => Boolean)
     (using runner: Runner[ReportType],
            inc: Inclusion[ReportType, Outcome],
            inc2: Inclusion[ReportType, DebugInfo])
      : Unit =
    ${Probably.aspire[TestType, ReportType]('test, 'runner, 'inc, 'inc2)}

  inline def assert[ReportType]
     (inline predicate: TestType => Boolean)
     (using runner:     Runner[ReportType],
            inclusion:  Inclusion[ReportType, Outcome],
            inclusion2: Inclusion[ReportType, DebugInfo])
          : Unit =
    ${Probably.assert[TestType, ReportType]('test, 'predicate, 'runner, 'inclusion, 'inclusion2)}

  inline def check[ReportType]
     (inline predicate: TestType => Boolean)
     (using runner:     Runner[ReportType],
            inclusion:  Inclusion[ReportType, Outcome],
            inclusion2: Inclusion[ReportType, DebugInfo])
          : TestType =
    ${Probably.check[TestType, ReportType]('test, 'predicate, 'runner, 'inclusion, 'inclusion2)}

  inline def assert[ReportType]()
     (using runner:     Runner[ReportType],
            inclusion:  Inclusion[ReportType, Outcome],
            inclusion2: Inclusion[ReportType, DebugInfo])
          : Unit =
    ${Probably.assert[TestType, ReportType]('test, '{Probably.succeed}, 'runner, 'inclusion, 'inclusion2)}

  inline def check[ReportType]()
     (using runner:     Runner[ReportType],
            inclusion:  Inclusion[ReportType, Outcome],
            inclusion2: Inclusion[ReportType, DebugInfo])
          : TestType =

    ${Probably.check[TestType, ReportType]('test, '{Probably.succeed}, 'runner, 'inclusion, 'inclusion2)}

  inline def matches[ReportType](inline pf: TestType ~> Any)
     (using runner: Runner[ReportType],
            inc:    Inclusion[ReportType, Outcome],
            inc2:   Inclusion[ReportType, DebugInfo])
          : Unit =

    assert[ReportType](pf.isDefinedAt(_))
