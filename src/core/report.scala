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

import rudiments.*
import deviation.*
import gossamer.*
import chiaroscuro.*
import escritoire.*
import escapade.*
import turbulence.*
import iridescence.*

import scala.collection.mutable as scm

object Benchmark:
  given Inclusion[TestReport, Benchmark] with
    def include(report: TestReport, testId: TestId, benchmark: Benchmark): TestReport =
      report.addBenchmark(testId, benchmark)

case class Benchmark(total: Long, count: Int, mean: Long, sd: Long, confidence: Double):
  def confidenceInterval: Long = (confidence*sd/math.sqrt(count.toDouble)).toLong
  def throughput: Long = (1000000000.0/mean).toLong

enum DebugInfo:
  case Throws(stack: StackTrace)
  case CheckThrows(stack: StackTrace)
  case Captures(values: Map[Text, Text])
  case Compare(expected: Text, found: Text, comparison: Comparison)
  case Message(message: Text)

trait Inclusion[ReportType, DataType]:
  def include(report: ReportType, testId: TestId, data: DataType): ReportType

trait TestReporter[ReportType]:
  def make(): ReportType
  def declareSuite(report: ReportType, suite: TestSuite): Unit
  def complete(report: ReportType): Unit

object TestReporter:
  given (using Stdio): TestReporter[TestReport] with
    def make(): TestReport = TestReport()
    def declareSuite(report: TestReport, suite: TestSuite): Unit = report.declareSuite(suite)
    def complete(report: TestReport): Unit = report.complete()

object TestReport:
  given Inclusion[TestReport, Outcome] with
    def include(report: TestReport, testId: TestId, outcome: Outcome): TestReport = 
      val report2 = report.addOutcome(testId, outcome)
      outcome match
        case Outcome.Pass(_) =>
          report2
        case Outcome.Fail(_) =>
          report2
        case Outcome.Throws(error, _) =>
          report2.addDebugInfo(testId, DebugInfo.Throws(StackTrace(error)))
        case Outcome.CheckThrows(error, _) =>
          report2.addDebugInfo(testId, DebugInfo.CheckThrows(StackTrace(error)))
  
  given Inclusion[TestReport, DebugInfo] = _.addDebugInfo(_, _)

class TestReport():

  enum ReportLine:
    case Suite(suite: Maybe[TestSuite], tests: scm.ListMap[TestId, ReportLine] = scm.ListMap())
    case Test(test: TestId, outcomes: scm.ArrayBuffer[Outcome] = scm.ArrayBuffer())
    case Bench(test: TestId, benchmark: Benchmark)

    def summaries: List[Summary] = this match
      case Suite(suite, tests)  =>
        val rest = tests.values.flatMap(_.summaries).to(List)
        if suite.unset then rest else Summary(Status.Suite, suite.option.get.id, 0, 0, 0, 0, 0, 0) :: rest
      
      case Bench(testId, bench@Benchmark(total, count, mean, _, _)) =>
        List(Summary(Status.Suite, testId, count, 0, 0, mean, bench.confidenceInterval, bench.throughput))

      case Test(testId, buf) =>
        val status =
          if buf.forall(_.is[Outcome.Pass]) then Status.Pass
          else if buf.forall(_.is[Outcome.Fail]) then Status.Fail
          else if buf.forall(_.is[Outcome.Throws]) then Status.Throws
          else if buf.forall(_.is[Outcome.CheckThrows]) then Status.CheckThrows
          else Status.Mixed
      
        val min: Long = buf.map(_.duration).min
        val max: Long = buf.map(_.duration).max
        val avg: Long = buf.foldLeft(0L)(_ + _.duration)/buf.length
          
        List(Summary(status, testId, buf.length, min, max, avg, 0, 0))

  private val lines: ReportLine.Suite = ReportLine.Suite(Unset)
  
  def resolve(suite: Maybe[TestSuite]): ReportLine.Suite =
    suite.option.map: suite =>
      resolve(suite.parent).tests(suite.id) match
        case suite@ReportLine.Suite(_, _) => suite
        case _                            => throw Mistake("should never occur")
    .getOrElse(lines)

  private val details: scm.SortedMap[TestId, scm.ArrayBuffer[DebugInfo]] =
    scm.TreeMap[TestId, scm.ArrayBuffer[DebugInfo]]().withDefault(_ => scm.ArrayBuffer[DebugInfo]())

  def declareSuite(suite: TestSuite): TestReport = this.tap: _ =>
    resolve(suite.parent).tests(suite.id) = ReportLine.Suite(suite, scm.ListMap())

  def addBenchmark(testId: TestId, benchmark: Benchmark): TestReport = this.tap: _ =>
    val benchmarks = resolve(testId.suite).tests
    benchmarks.getOrElseUpdate(testId, ReportLine.Bench(testId, benchmark))
  
  def addOutcome(testId: TestId, outcome: Outcome): TestReport = this.tap: _ =>
    val tests = resolve(testId.suite).tests
    
    tests.getOrElseUpdate(testId, ReportLine.Test(testId, scm.ArrayBuffer[Outcome]())) match
      case ReportLine.Test(_, buf) => buf.append(outcome)
      case _                       => throw Mistake("should never match")
  
  def addDebugInfo(testId: TestId, info: DebugInfo): TestReport =
    this.tap: _ =>
      details(testId) = details(testId).append(info)

  enum Status:
    case Pass, Fail, Throws, CheckThrows, Mixed, Suite, Bench

    def color: Rgb24 = this match
      case Pass        => rgb"#8abd00"
      case Fail        => colors.Tomato
      case Throws      => colors.DarkOrange
      case CheckThrows => rgb"#dd40a0"
      case Mixed       => rgb"#ddd700"
      case Suite       => colors.SlateBlue
      case Bench       => colors.CadetBlue

    def symbol: AnsiText = this match
      case Pass        => ansi"${Bg(rgb"#8abd00")}( $Bold(${colors.Black}(✓)) )"
      case Fail        => ansi"${Bg(colors.Tomato)}( $Bold(${colors.Black}(✗)) )"
      case Throws      => ansi"${Bg(colors.DarkOrange)}( $Bold(${colors.Black}(!)) )"
      case CheckThrows => ansi"${Bg(rgb"#dd40a0")}( $Bold(${colors.Black}(‼)) )"
      case Mixed       => ansi"${Bg(rgb"#ddd700")}( $Bold(${colors.Black}(?)) )"
      case Suite       => ansi"   "
      case Bench       => ansi"${Bg(colors.CadetBlue)}( $Bold(${colors.Black}(*)) )"

  case class Summary
      (status: Status, id: TestId, count: Int, min: Long, max: Long, avg: Long, conf: Long, throughput: Long):
    def indentedName: AnsiText =
      val depth = id.suite.mm(_.id.depth).or(0) + 1
      
      val title =
        if status == Status.Suite then ansi"${colors.Silver}($Bold(${id.name}))"
        else ansi"${id.name}"
      
      ansi"${t"  "*(depth - 1)}$title"

    val unitsSeq: List[AnsiText] = List(
      ansi"${colors.BurlyWood}(µs)",
      ansi"${colors.Goldenrod}(ms)",
      ansi"${colors.Sienna}(s) "
    )
    
    protected def time(n: Long, units: List[AnsiText] = unitsSeq): AnsiText = units match
      case Nil =>
        n.show.ansi
      
      case unit :: rest =>
        if n > 100000L then time(n/1000L, rest) else
          val sig = (n/1000L).show
          val frac = (n%1000).show.pad(3, Rtl, '0')(using textWidthCalculation.uniform)
          ansi"${colors.Silver}(${sig}.$frac) ${unit}"
    
    private def ops(n: Long): AnsiText = ansi"${colors.Silver}($n) ops/s"

    def minTime: AnsiText = if min == 0L then ansi"" else time(min)
    def maxTime: AnsiText = if max == 0L then ansi"" else time(max)
    def avgTime: AnsiText = if avg == 0L then ansi"" else time(avg)
    def confInt: AnsiText = if conf == 0.0 then ansi"" else ansi"±${time(conf)}"
    def iterations: AnsiText = if count == 0 then ansi"" else count.ansi
    def opsPerS: AnsiText = if throughput == 0 then ansi"" else ops(throughput)

  def complete()(using Stdio): Unit =
    import textWidthCalculation.uniform
    
    val table: Table[Summary] =
      val showStats = !lines.summaries.forall(_.count < 2)
      val showBench = lines.summaries.exists(_.throughput > 0)
      val timeTitle = if showStats then t"Avg" else t"Time"
      
      Table(
        Column(ansi"")(_.status.symbol),
        
        Column(ansi"$Bold(Hash)"): s =>
          ansi"${colors.CadetBlue}(${s.id.id})",
        
        Column(ansi"$Bold(Test)")(_.indentedName),
        
        Column(ansi"$Bold(Count)", align = Alignment.Right, hide = !showStats): s =>
          ansi"${colors.SteelBlue}(${s.iterations})",
        
        Column(ansi"$Bold(Min)", align = Alignment.Right, hide = !showStats): s =>
          if s.count < 2 then ansi"" else s.minTime,
        
        Column(ansi"$Bold($timeTitle)", align = Alignment.Right)(_.avgTime),
        
        Column(ansi"$Bold(Max)", align = Alignment.Right, hide = !showStats): s =>
          if s.count < 2 then ansi"" else s.maxTime,
        
        Column(ansi"$Bold(Confidence)", align = Alignment.Right, hide = !showBench): s =>
          if s.throughput == 0 then ansi"" else s.confInt,
        
        Column(ansi"$Bold(T/put)", align = Alignment.Right, hide = !showBench)(_.opsPerS)
      )
      
    import tableStyles.rounded
    table.tabulate(lines.summaries, 120).map(_.render).foreach(Io.println(_))

    details.foreach: (id, info) =>
      val ribbon = Ribbon(colors.DarkRed.srgb, colors.FireBrick.srgb, colors.Tomato.srgb)
      Io.println(ribbon.fill(ansi"$Bold(${id.id})", id.codepoint.text.ansi, id.name.ansi).render)
      
      info.foreach: debugInfo =>
        Io.println(t"")
        debugInfo match
          case DebugInfo.Throws(err) =>
            val name = ansi"$Italic(${colors.White}(${err.component}.${err.className}))"
            Io.println(ansi"${colors.Silver}(An exception was thrown while running test:)".render)
            Io.println(err.crop(t"probably.Runner", t"run()").ansi.render)
          
          case DebugInfo.CheckThrows(err) =>
            val name = ansi"$Italic(${colors.White}(${err.component}.${err.className}))"
            Io.println(ansi"${colors.Silver}(An exception was thrown while checking the test predicate:)".render)
            Io.println(err.crop(t"probably.Outcome#", t"apply()").dropRight(1).ansi.render)
          
          case DebugInfo.Compare(expected, found, cmp) =>
            val expected2: AnsiText = ansi"$Italic(${colors.White}($expected))"
            val found2: AnsiText = ansi"$Italic(${colors.White}($found))"
            val nl = if expected.contains(t"\n") || found.contains(t"\n") then '\n' else ' '
            val instead = ansi"but instead it returned$nl$found2$nl"
            Io.println(ansi"${colors.Silver}(The test was expected to return$nl$expected2$nl$instead)".render)
            Io.println(cmp.ansi.render)
          
          case DebugInfo.Captures(map) =>
            Table[(Text, Text)](
              Column(ansi"Expression", align = Alignment.Right)(_(0)),
              Column(ansi"Value")(_(1)),
            ).tabulate(map.to(List), 140).map(_.render).foreach(Io.println(_))
          
          case DebugInfo.Message(text) =>
            Io.println(text)
      
      Io.println()
