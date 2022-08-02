/*
    Guillotine, version 0.4.0. Copyright 2018-22 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package guillotine

import contextual.*
import rudiments.*
import tetromino.*
import turbulence.*
import gossamer.*
import eucalyptus.*
import escapade.*
import iridescence.*

import scala.jdk.StreamConverters.StreamHasToScala
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.jdk.CollectionConverters.MapHasAsScala

import annotation.targetName
import java.io as ji

enum Context:
  case Awaiting, Unquoted, Quotes2, Quotes1

case class State(current: Context, esc: Boolean, args: List[Text])

object Executor:
  given stream: Executor[LazyList[Text]] = proc =>
    ji.BufferedReader(ji.InputStreamReader(proc.getInputStream)).lines().nn.toScala(LazyList).map(_.show)
  
  given text: Executor[Text] = stream.map(_.foldLeft(t"") { (acc, line) => t"$acc\n$line" }.trim)

  given dataStream(using Allocator): Executor[DataStream] = proc =>
    Util.readInputStream(proc.getInputStream.nn)
  
  given exitStatus: Executor[ExitStatus] = _.waitFor() match
    case 0     => ExitStatus.Ok
    case other => ExitStatus.Fail(other)
  
  given unit: Executor[Unit] = exitStatus.map(_ => ())
 
trait Executor[T]:
  def interpret(process: java.lang.Process): T
  def map[S](fn: T => S): Executor[S] = process => fn(interpret(process))

class Process[T](process: java.lang.Process, executor: Executor[T]):
  def pid: Pid = Pid(process.pid)
  def stdout(rubrics: Rubric*)(using Allocator): DataStream =
    Util.readInputStream(process.getInputStream.nn, rubrics*)
  
  def stderr(rubrics: Rubric*)(using Allocator): DataStream =
    Util.readInputStream(process.getErrorStream.nn, rubrics*)
  
  def stdin(in: DataStream)(using Allocator): Unit throws StreamCutError = Util.write(in, process.getOutputStream.nn)
  def await(): T = executor.interpret(process)
  
  def exitStatus(): ExitStatus = process.waitFor() match
    case 0     => ExitStatus.Ok
    case other => ExitStatus.Fail(other)
  
  def abort()(using Log): Unit =
    Log.info(ansi"The process with PID $pid was aborted")
    process.destroy()
  
  def kill()(using Log): Unit =
    Log.warn(ansi"The process with PID $pid was killed")
    process.destroyForcibly()

sealed trait Executable:
  def fork[T]()(using env: Environment, exec: Executor[T] = Executor.text)(using Log): Process[T]
  
  def exec[T]()(using env: Environment, exec: Executor[T] = Executor.text)(using Log): T =
    fork[T]().await()
  
  def apply(cmd: Executable): Pipeline = cmd match
    case Pipeline(cmds*) => this match
      case Pipeline(cmds2*) => Pipeline((cmds ++ cmds2)*)
      case cmd: Command     => Pipeline((cmds :+ cmd)*)
    case cmd: Command    => this match
      case Pipeline(cmds2*) => Pipeline((cmd +: cmds2)*)
      case cmd2: Command    => Pipeline(cmd, cmd2)
  
  @targetName("pipeTo")
  infix def |(cmd: Executable): Pipeline = cmd(this)

object Command:

  private def formattedArgs(args: Seq[Text]): Text =
    args.map:
      arg =>
        if arg.contains(t"\"") && !arg.contains(t"'") then t"""'$arg'"""
        else if arg.contains(t"'") && !arg.contains(t"\"") then t""""$arg""""
        else if arg.contains(t"'") && arg.contains(t"\"")
          then t""""${arg.rsub(t"\\\"", t"\\\\\"")}""""
        else if arg.contains(t" ") || arg.contains(t"\t") || arg.contains(t"\\") then t"'$arg'"
        else arg
    .join(t" ")

  given DebugString[Command] = cmd =>
    val cmdString: Text = formattedArgs(cmd.args)
    if cmdString.contains(t"\"") then t"sh\"\"\"$cmdString\"\"\"" else t"sh\"$cmdString\""

  given AnsiShow[Command] = cmd => ansi"${colors.LightSeaGreen}(${formattedArgs(cmd.args)})"

case class Command(args: Text*) extends Executable:
  def fork[T]()(using env: Environment, exec: Executor[T] = Executor.text)(using Log): Process[T] =
    val processBuilder = ProcessBuilder(args.ss*)
    val dir = ji.File(env(t"PWD").get.s)
    processBuilder.directory(dir)
    val t0 = System.currentTimeMillis
    Log.info(ansi"Starting process ${this.ansi} in directory ${dir.getAbsolutePath.nn}")
    new Process[T](processBuilder.start().nn, summon[Executor[T]])

object Pipeline:
  given DebugString[Pipeline] = _.cmds.map(summon[DebugString[Command]].show(_)).join(t" | ")
  given AnsiShow[Pipeline] =
    _.cmds.map(summon[AnsiShow[Command]].ansiShow(_))
        .join(ansi" ${colors.PowderBlue}(|) ")

case class Pipeline(cmds: Command*) extends Executable:
  def fork[T]()(using env: Environment, exec: Executor[T] = Executor.text)(using Log): Process[T] =
    val dir = ji.File(env(t"PWD").get.s)
    Log.info(ansi"Starting pipelined processes ${this.ansi} in directory ${dir.getAbsolutePath.nn}")

    val processBuilders = cmds.map: cmd =>
      val pb = ProcessBuilder(cmd.args.ss*)
      pb.directory(dir)
      pb.nn

    new Process[T](ProcessBuilder.startPipeline(processBuilders.asJava).nn.asScala.to(List).last, exec)

case class ExecError(command: Command, stdout: DataStream, stderr: DataStream)
extends Error(err"execution of the command $command failed")

object Sh:
  case class Params(params: Text*)

  object Prefix extends Interpolator[Params, State, Command]:
    import Context.*
  
    def complete(state: State): Command =
      val args = state.current match
        case Quotes2        => throw InterpolationError(t"the double quotes have not been closed")
        case Quotes1        => throw InterpolationError(t"the single quotes have not been closed")
        case _ if state.esc => throw InterpolationError(t"cannot terminate with an escape character")
        case _              => state.args
      
      Command(args*)

    def initial: State = State(Awaiting, false, Nil)

    def skip(state: State): State = insert(state, Params(t"x"))

    def insert(state: State, value: Params): State =
      value.params.to(List) match
        case h :: t =>
          if state.esc then throw InterpolationError(txt"""escaping with '\\' is not allowed
                                                         immediately before a substitution""")
          
          state match
            case State(Awaiting, false, args) =>
              State(Unquoted, false, args ++ (h :: t))

            case State(Unquoted, false, args :+ last) =>
              State(Unquoted, false, args ++ (t"$last$h" :: t))
            
            case State(Quotes1, false, args :+ last) =>
              State(Quotes1, false, args :+ (t"$last$h" :: t).join(t" "))
            
            case State(Quotes2, false, args :+ last) =>
              State(Quotes2, false, args :+ (t"$last$h" :: t).join(t" "))
            
            case _ =>
              throw Mistake("impossible parser state")
        case _ =>
          state
          
    def parse(state: State, next: Text): State = next.chars.foldLeft(state):
      case (State(Awaiting, esc, args), ' ')          => State(Awaiting, false, args)
      case (State(Quotes1, false, rest :+ cur), '\\') => State(Quotes1, false, rest :+ t"$cur\\")
      case (State(ctx, false, args), '\\')            => State(ctx, true, args)
      case (State(Unquoted, esc, args), ' ')          => State(Awaiting, false, args)
      case (State(Quotes1, esc, args), '\'')          => State(Unquoted, false, args)
      case (State(Quotes2, false, args), '"')         => State(Unquoted, false, args)
      case (State(Unquoted, false, args), '"')        => State(Quotes2, false, args)
      case (State(Unquoted, false, args), '\'')       => State(Quotes1, false, args)
      case (State(Awaiting, false, args), '"')        => State(Quotes2, false, args :+ t"")
      case (State(Awaiting, false, args), '\'')       => State(Quotes1, false, args :+ t"")
      case (State(Awaiting, esc, args), char)         => State(Unquoted, false, args :+ t"$char")
      case (State(ctx, esc, Nil), char)               => State(ctx, false, List(t"$char"))
      case (State(ctx, esc, rest :+ cur), char)       => State(ctx, false, rest :+ t"$cur$char")
      case _                                          => throw Mistake("impossible parser state")

  given Insertion[Params, Text] = value => Params(value)
  given Insertion[Params, List[Text]] = xs => Params(xs*)
  given Insertion[Params, Command] = cmd => Params(cmd.args*)
  given [T: Show]: Insertion[Params, T] = value => Params(summon[Show[T]].show(value))

given realm: Realm = Realm(t"guillotine")
