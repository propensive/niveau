/*
    Jovian, version 0.11.0. Copyright 2020-22 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package jovian

import kaleidoscope.*
import gastronomy.*
import slalom.*
import rudiments.*
import eucalyptus.*
import gossamer.*

import scala.util.*
import scala.collection.generic.CanBuildFrom

import java.net.URI

import java.nio.file as jnf
import java.io as ji

import jnf.{FileSystems, FileVisitResult, Files, Paths, SimpleFileVisitor, StandardCopyOption,
    DirectoryNotEmptyException, Path as JavaPath}, jnf.StandardCopyOption.*,
    jnf.attribute.BasicFileAttributes

import ji.{Closeable, InputStream, File as JavaFile}
enum Recursion:
  case Recursive, Nonrecursive

type Majuscule = 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'G' | 'H' | 'I' | 'J' | 'K' | 'L' | 'M' | 'N' |
    'O' | 'P' | 'Q' | 'R' | 'S' | 'T' | 'U' | 'V' | 'W' | 'X' | 'Y' | 'Z'

case class ClasspathRefError(classpath: Classpath)(path: classpath.ClasspathRef) extends Error:
  def message: Text = t"the resource $path could not be accessed"

case class PwdError() extends Error:
  def message: Text = t"the current working directory cannot be determined"

object Fifo:
  given Sink[Fifo] with
    type E = IoError
    def write(value: Fifo, stream: DataStream): Unit throws E | StreamCutError =
      try Util.write(stream, value.out)
      catch case e => throw IoError(IoError.Op.Write, IoError.Reason.AccessDenied, value.file.path)

case class Fifo(file: File):
  val out = ji.FileOutputStream(file.javaFile, false)
  def close(): Unit = out.close()

trait Inode:
  def javaPath: jnf.Path
  def javaFile: ji.File
  def name: Text
  def fullname: Text
  def uriString: Text
  def exists(): Boolean
  def parent: Directory throws RootParentError
  def lastModified: Long
  def copyTo(dest: DiskPath): Inode throws IoError
  def delete(): Unit throws IoError
  def readable: Boolean = Files.isReadable(javaPath)
  def writable: Boolean = Files.isWritable(javaPath)

object File:
  given Show[File] = _.fullname
  
  given Sink[File] with
    type E = IoError
    def write(value: File, stream: DataStream): Unit throws E | StreamCutError =
      val out = ji.FileOutputStream(value.javaFile, false)
      try Util.write(stream, out)
      catch case e => throw IoError(IoError.Op.Write, IoError.Reason.AccessDenied, value.path)
      finally try out.close() catch _ => ()
  
  given Source[File] with
    type E = IoError
    def read(file: File): DataStream throws E =
      try Util.readInputStream(ji.FileInputStream(file.javaFile), 64.kb)
      catch case e: ji.FileNotFoundException =>
        if e.getMessage.nn.contains("(Permission denied)")
        then throw IoError(IoError.Op.Read, IoError.Reason.AccessDenied, file.path)
        else throw IoError(IoError.Op.Read, IoError.Reason.DoesNotExist, file.path)

trait File extends Inode:
  def path: DiskPath
  def executable: Boolean = Files.isExecutable(javaPath)
  def copyTo(dest: DiskPath): File throws IoError
  def modified: Long
  
  def read[T](limit: ByteSize = 64.kb)(using readable: Readable[T])
             : T throws readable.E | IoError | StreamCutError
  
  def touch(): Unit throws IoError =
    try
      if !exists() then ji.FileOutputStream(javaFile).close()
      else javaFile.setLastModified(System.currentTimeMillis())
    catch case e => throw IoError(IoError.Op.Write, IoError.Reason.NotSupported, path)
  
  def size(): ByteSize = javaFile.length.b

trait Symlink extends Inode

object DiskPath:
  given Show[DiskPath] = _.fullname

trait DiskPath:
  def javaPath: jnf.Path
  def javaFile: ji.File
  def exists(): Boolean
  def parent: DiskPath throws RootParentError
  def file: File throws IoError
  def parts: List[Text]
  def directory(creation: Creation = Creation.Ensure): Directory throws IoError
  def symlink: Symlink throws IoError
  def separator: Text
  def fullname: Text
  def rename(fn: Text => Text): DiskPath
  def name: Text
  def prefix: Text
  def createFile(overwrite: Boolean = false): File throws IoError
  def filesystem: Filesystem
  def +(relative: Relative): DiskPath throws RootParentError
  
  @targetName("access")
  def /(child: Text): DiskPath throws RootParentError
  
  def descendantFiles(descend: (Directory => Boolean) = _ => true): LazyList[File] throws IoError

object Directory:
  given Show[Directory] = _.path.show
  
trait Directory extends Inode:
  def path: DiskPath
  def files: List[File] throws IoError
  def children: List[Inode] throws IoError
  def subdirectories: List[Directory] throws IoError
  def copyTo(dest: DiskPath): Directory throws IoError
  
  @targetName("access")
  def /(child: Text): DiskPath throws RootParentError

object IoError:
  object Reason:
    given Show[Reason] =
      case NotFile              => t"the path does not refer to a file"
      case NotDirectory         => t"the path does not refer to a directory"
      case NotSymlink           => t"the path does not refer to a symlink"
      case DoesNotExist         => t"no node exists at this path"
      case AlreadyExists        => t"a node already exists at this path"
      case AccessDenied         => t"the operation is not permitted on this path"
      case DifferentFilesystems => t"the source and destination are on different filesystems"
      case NotSupported         => t"the filesystem does not support it"

  enum Reason:
    case NotFile, NotDirectory, NotSymlink, DoesNotExist, AlreadyExists, AccessDenied,
        DifferentFilesystems, NotSupported

  object Op:
    given Show[Op] = Showable(_).show.lower

  enum Op:
    case Read, Write, Access, Create, Delete

case class IoError(operation: IoError.Op, reason: IoError.Reason, path: DiskPath) extends Error:
  def message: Text = t"the $operation operation at ${path.show} failed because $reason"

case class InotifyError() extends Error:
  def message: Text = t"the limit on the number of paths that can be watched has been exceeded"

open class Classpath(classLoader: ClassLoader = getClass.nn.getClassLoader.nn)
extends Root(t"/", t""):
  protected inline def classpath: this.type = this

  type AbsolutePath = ClasspathRef

  def makeAbsolute(parts: List[Text]): ClasspathRef = ClasspathRef(parts)

  @targetName("access")
  def /(child: Text): ClasspathRef = ClasspathRef(List(child))

  object ClasspathRef:
    given Show[ClasspathRef] = _.parts.join(t"classpath:", t"/", t"")

  case class ClasspathRef(elements: List[Text]) extends Path.Absolute(elements):
    def resource: Resource = Resource(makeAbsolute(parts))

  case class Resource(path: ClasspathRef):
    def read[T](limit: ByteSize = 64.kb)(using readable: Readable[T])
            : T throws ClasspathRefError | StreamCutError | readable.E =
      val resource = classLoader.getResourceAsStream(path.show.s)
      if resource == null then throw ClasspathRefError(classpath)(path)
      val stream = Util.readInputStream(resource.nn, limit)
      readable.read(stream)
    
    def name: Text = path.parts.lastOption.getOrElse(prefix)
    def parent: Resource throws RootParentError = Resource(path.parent)

enum Creation:
  case Expect, Create, Ensure

export Creation.{Expect, Create, Ensure}

class Filesystem(pathSeparator: Text, fsPrefix: Text) extends Root(pathSeparator, fsPrefix):
  fs =>
  type AbsolutePath = DiskPath

  def makeAbsolute(parts: List[Text]): DiskPath = DiskPath(parts)

  def unapply(path: jnf.Path): Some[DiskPath] =
    Some(DiskPath((0 until path.getNameCount).map(path.getName(_).toString.show).to(List)))

  def parse(value: Text, pwd: Maybe[AbsolutePath] = Unset): Option[AbsolutePath] =
    if value.startsWith(prefix)
    then
      val parts: List[Text] = value.drop(prefix.length).cut(separator)
      Some(DiskPath(List(parts*)))
    else
      try pwd.option.map:
        path =>
          (path + Relative.parse(value)) match
            case p: fs.DiskPath => p
      catch case err: RootParentError => None
  
  @targetName("access")
  def /(child: Text): Path.Absolute = Path.Absolute(List(child))

  case class DiskPath(elements: List[Text]) extends Path.Absolute(elements), jovian.DiskPath:
    lazy val javaFile: ji.File = ji.File(fullname.s)
    lazy val javaPath: jnf.Path = javaFile.toPath.nn
    def filesystem: Filesystem = fs
    def prefix: Text = fsPrefix
    def separator: Text = pathSeparator
    def name: Text = elements.last
    def fullname: Text = elements.join(fsPrefix, separator, t"")

    def file: File throws IoError =
      if !exists() then throw IoError(IoError.Op.Access, IoError.Reason.DoesNotExist, this)
      if isDirectory then throw IoError(IoError.Op.Access, IoError.Reason.NotFile, this)
      
      File(this)

    def +(relative: Relative): DiskPath throws RootParentError =
      if relative.ascent == 0 then DiskPath(elements ++ relative.parts)
      else parent + relative.copy(ascent = relative.ascent - 1)


    def directory(creation: Creation = Creation.Ensure): Directory throws IoError = fs.synchronized:
      import IoError.*
      creation match
        case Creation.Create if exists() =>
          throw IoError(Op.Access, Reason.DoesNotExist, this)
        
        case Creation.Expect if !exists() =>
          throw IoError(Op.Access, Reason.AlreadyExists, this)
        
        case Creation.Ensure if !exists() =>
          if !javaFile.mkdirs() then throw IoError(Op.Create, Reason.AccessDenied, this)
        
        case _ =>
          ()
      
      Directory(this)
      
      if !exists() && creation == Creation.Expect
      then throw IoError(IoError.Op.Access, IoError.Reason.DoesNotExist, this)
      
      if !isDirectory then throw IoError(IoError.Op.Access, IoError.Reason.NotDirectory, this)
      
      Directory(this)
  
    def symlink: Symlink throws IoError =
      if !javaFile.exists()
      then throw IoError(IoError.Op.Access, IoError.Reason.DoesNotExist, this)
      
      if !Files.isSymbolicLink(javaFile.toPath)
      then throw IoError(IoError.Op.Access, IoError.Reason.NotSymlink, this)
      
      Symlink(this, parse(Showable(Files.readSymbolicLink(Paths.get(fullname.s))).show).get)

    def isFile: Boolean = javaFile.exists() && !javaFile.isDirectory
    def isDirectory: Boolean = javaFile.exists() && javaFile.isDirectory
    def isSymlink: Boolean = javaFile.exists() && Files.isSymbolicLink(javaFile.toPath)
    
    def descendantFiles(descend: (jovian.Directory => Boolean) = _ => true)
                       : LazyList[File] throws IoError =
      if javaFile.isDirectory
      then directory(Expect).files.to(LazyList) #::: directory(Expect).subdirectories.filter(
          descend).to(LazyList).flatMap(_.path.descendantFiles(descend))
      else LazyList(file)

    def inode: Inode throws IoError =
      
      if !javaFile.exists()
      then throw IoError(IoError.Op.Access, IoError.Reason.DoesNotExist, this)
      
      if isDirectory then Directory(this)
      else if isFile
      then
        try Symlink(this, parse(Showable(Files.readSymbolicLink(javaPath)).show).get)
        catch NoSuchElementException => File(this)
      else File(this)
    
    def exists(): Boolean = javaFile.exists()
  
    def createFile(overwrite: Boolean = false): File throws IoError =
      if !overwrite && exists()
      then throw IoError(IoError.Op.Create, IoError.Reason.AlreadyExists, this)
      
      try ji.FileOutputStream(javaFile).close()
      catch case e =>
        throw IoError(IoError.Op.Create, IoError.Reason.AccessDenied, this)
  
      File(this)
    
  sealed trait Inode(val path: DiskPath) extends jovian.Inode:
    lazy val javaFile: ji.File = ji.File(fullname.s)
    lazy val javaPath: jnf.Path = javaFile.toPath.nn

    def name: Text = path.parts.lastOption.getOrElse(prefix)
    def fullname: Text = javaFile.getAbsolutePath.nn.show
    def uriString: Text = Showable(javaFile.toURI).show
    def exists(): Boolean = Files.exists(javaPath)
    def parent: Directory throws RootParentError = Directory(path.parent)
    def directory: Option[Directory]
    def file: Option[File]
    def symlink: Option[Symlink]
    def lastModified: Long = javaFile.lastModified
    def copyTo(dest: jovian.DiskPath): jovian.Inode throws IoError
    def delete(): Unit throws IoError

  case class File(filePath: DiskPath) extends Inode(filePath), jovian.File:
    def directory: Option[Directory] = None
    def file: Option[File] = Some(this)
    def symlink: Option[Symlink] = None
    def modified: Long = javaFile.lastModified
    
    def delete(): Unit throws IoError =
      try javaFile.delete()
      catch e => throw IoError(IoError.Op.Delete, IoError.Reason.AccessDenied, filePath)
    
    def read[T](limit: ByteSize = 64.kb)(using readable: Readable[T])
        : T throws readable.E | IoError | StreamCutError =
      val stream = Util.readInputStream(ji.FileInputStream(javaFile), limit)
      try readable.read(stream) catch case e =>
        throw IoError(IoError.Op.Read, IoError.Reason.AccessDenied, path)

    def copyTo(dest: jovian.DiskPath): jovian.File throws IoError =
      if dest.exists()
      then throw IoError(IoError.Op.Create, IoError.Reason.AlreadyExists, dest)
      
      try Files.copy(javaPath, dest.javaPath)
      catch IOException =>
        throw IoError(IoError.Op.Write, IoError.Reason.AccessDenied, dest)
      
      dest.file

    def hardLinkCount(): Int throws IoError =
      try Files.getAttribute(javaPath, "unix:nlink") match
        case i: Int => i
        case _      => throw Impossible("Should never match")
      catch e => throw IoError(IoError.Op.Read, IoError.Reason.NotSupported, path)
    
    def hardLinkTo(dest: DiskPath): File throws IoError =
      if dest.exists()
      then throw IoError(IoError.Op.Create, IoError.Reason.AlreadyExists, dest)
      
      try Files.createLink(javaPath, Paths.get(fullname.s))
      catch
        case e: jnf.NoSuchFileException =>
          throw IoError(IoError.Op.Write, IoError.Reason.DoesNotExist, path)
        case e: jnf.FileAlreadyExistsException =>
          throw IoError(IoError.Op.Write, IoError.Reason.AlreadyExists, dest)
        case e: jnf.AccessDeniedException =>
          throw IoError(IoError.Op.Write, IoError.Reason.AccessDenied, dest)

      dest.file

  case class Symlink(symlinkPath: DiskPath, target: jovian.DiskPath)
  extends Inode(symlinkPath), jovian.Symlink:
    def apply(): jovian.DiskPath = target
    
    def hardLinkTo(dest: DiskPath): Inode throws IoError =
      Files.createSymbolicLink(dest.javaPath, target.javaPath)

      Symlink(path, dest)
    
    def directory: Option[Directory] = None
    def file: Option[File] = None
    def symlink: Option[Symlink] = Some(this)
    
    def delete(): Unit throws IoError =
      try javaFile.delete()
      catch e => throw IoError(IoError.Op.Delete, IoError.Reason.AccessDenied, path)
    
    def copyTo(dest: jovian.DiskPath): jovian.Symlink throws IoError =
      Files.createSymbolicLink(Paths.get(dest.show.s), Paths.get(target.fullname.s))

      Symlink(path, dest)

  enum FileEvent:
    case NewFile(file: File)
    case NewDirectory(directory: Directory)
    case Modify(file: File)
    case Delete(file: DiskPath)
    case NoChange

  case class Watcher(startStream: () => LazyList[FileEvent], stop: () => Unit):
    def stream: LazyList[FileEvent] = startStream()

  case class Directory(dirPath: DiskPath) extends Inode(dirPath), jovian.Directory:
    def directory: Option[Directory] = Some(this)
    def file: Option[File] = None
    def symlink: Option[Symlink] = None

    def delete(): Unit throws IoError =
      def recur(file: JavaFile): Boolean =
        if Files.isSymbolicLink(file.toPath) then file.delete()
        else if file.isDirectory
        then file.listFiles.nn.map(_.nn).forall(recur(_)) && file.delete()
        else file.delete()

      try recur(javaFile).unit
      catch e => throw IoError(IoError.Op.Delete, IoError.Reason.AccessDenied, dirPath)

    def watch(recursive: Boolean = true, interval: Int = 100)(using Log)
             : Watcher throws IoError | InotifyError =
      import java.nio.file.*, StandardWatchEventKinds.*
      import collection.JavaConverters.*
      var continue: Boolean = true
      
      val dirs: Set[Directory] =
        if recursive then deepSubdirectories.to(Set) + this else Set(this)
      
      val svc = javaPath.getFileSystem.nn.newWatchService().nn
      
      def watchKey(dir: Directory): WatchKey =
        // Calls to `Log.fine` seem to result in an AssertionError at compiletime
        //Log.fine(t"Started monitoring for changes in ${dir.path.show}")
        dir.javaPath.register(svc, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE).nn

      def poll(index: Map[WatchKey, Directory]): LazyList[FileEvent] =
        erased given CanThrow[RootParentError] = compiletime.erasedValue
        val events = index.map:
          (key, dir) => key.pollEvents().nn.iterator.nn.asScala.to(List).flatMap:
            event =>
              val path: DiskPath = event.context match
                case path: jnf.Path =>
                  (dir.path + Relative.parse(Showable(path).show)) match
                    case path: fs.DiskPath => path
                
                case _ =>
                  throw Impossible("Watch service should always return a Path")
              
              event.kind match
                case ENTRY_CREATE => if path.isDirectory
                                     then List(FileEvent.NewDirectory(path.directory(Expect)))
                                     else List(FileEvent.NewFile(path.file))
                case ENTRY_MODIFY => List(FileEvent.Modify(path.file))
                case ENTRY_DELETE => List(FileEvent.Delete(path))
                case _            => Nil
        .flatten
      
        if continue then
          val newIndex = events.foldLeft(index):
            case (index, FileEvent.NewDirectory(dir)) =>
              // Calls to `Log.fine` seem to result in an AssertionError at compiletime
              //Log.fine(t"Starting monitoring new directory ${dir.path.show}")
              index.updated(watchKey(dir), dir)
            
            case (index, FileEvent.Delete(path)) =>
              // Calls to `Log.fine` seem to result in an AssertionError at compiletime
              //if path.isDirectory then Log.fine(t"Stopping monitoring of deleted directory $path")
              val deletions = index.filter(_(1).path == path)
              deletions.keys.foreach(_.cancel())
              index -- deletions.keys
            
            case _ =>
              index
          
          def sleepPoll(): LazyList[FileEvent] =
            Thread.sleep(interval)
            poll(newIndex)

          if events.isEmpty then FileEvent.NoChange #:: sleepPoll()
          else events.to(LazyList) #::: sleepPoll()
        else
          index.keys.foreach(_.cancel())
          LazyList()

      Watcher(() => poll(dirs.mtwin.map(watchKey(_) -> _).to(Map)), () => continue = false)

    def children: List[Inode] throws IoError =
      Option(javaFile.list).fold(Nil):
        files =>
          files.nn.unsafeImmutable.to(List).map(_.nn).map(Text(_)).map(path.parts :+ _).map(
              makeAbsolute(_)).map(_.inode)
    
    def descendants: LazyList[Inode] throws IoError =
      children.to(LazyList).flatMap(_.directory).flatMap { f => f +: f.descendants }
    
    def subdirectories: List[Directory] throws IoError =
      children.collect:
        case dir: Directory => dir
    
    def deepSubdirectories: LazyList[Directory] throws IoError =
      val subdirs = subdirectories.to(LazyList).filter(!_.name.startsWith(t"."))
      subdirs #::: subdirs.flatMap(_.deepSubdirectories)

    def files: List[File] throws IoError = children.collect:
      case file: File => file

    def copyTo(dest: jovian.DiskPath): jovian.Directory throws IoError =
      if dest.exists()
      then throw IoError(IoError.Op.Write, IoError.Reason.AlreadyExists, dest)
      
      try Files.copy(javaPath, Paths.get(dest.show.s))
      catch e => throw IoError(IoError.Op.Write, IoError.Reason.AccessDenied, dirPath)
      
      dest.directory(Expect)

    @targetName("access")
    def /(child: Text): DiskPath throws RootParentError = makeAbsolute((path / child).parts)


// object OldPath:
//   def apply(jpath: JavaPath): Path = Path(jpath.show.s match
//     case ""    => "."
//     case other => other
//   )
  
//   def apply(file: JavaFile): Path = Path(file.getAbsolutePath)
//   def apply(uri: URI): Path = Path(Paths.get(uri))

//   def unapply(str: String): Option[Path] = str match
//     case r"""${dir: String}@([^*?:;,&|"\%<>]*)""" => Some(Path(dir))
//     case _                              => None

//   private class CopyFileVisitor(sourcePath: JavaPath, targetPath: JavaPath) extends SimpleFileVisitor[JavaPath]:

//     override def preVisitDirectory(dir: JavaPath, attrs: BasicFileAttributes): FileVisitResult =
//       targetPath.resolve(sourcePath.relativize(dir)).toFile.mkdirs()
//       FileVisitResult.CONTINUE

//     override def visitFile(file: JavaPath, attrs: BasicFileAttributes): FileVisitResult =
//       Files.copy(file, targetPath.resolve(sourcePath.relativize(file)), REPLACE_EXISTING)
//       FileVisitResult.CONTINUE


//   def apply(input: String): Path =
//     if input == "/" then ji.File.separator
//     else
//       def canonicalize(str: List[String], drop: Int = 0): List[String] = str match
//         case ".." :: tail => canonicalize(tail, drop + 1)
//         case head :: tail => if drop > 0 then canonicalize(tail, drop - 1) else head :: canonicalize(tail)
//         case Nil          => List.fill(drop)("..")
      
//       canonicalize((input.cut("/").to(List) match
//         case "" :: xs => "" :: xs.filter { p => p != "." && p != "" }
//         case xs       => xs.filter { p => p != "." && p != "" }
//       ).reverse).reverse match
//         case Nil => "."
//         case xs  => xs.mkString("/")

  // extension (path: Path)
  //   def filename: String = path
  //   def name: String = Showable(javaPath.getFileName).show

  //   def setReadOnly(recursion: Recursion): Unit throws UnchangeablePermissions =
  //     if !javaFile.setWritable(false) then throw UnchangeablePermissions(path)
  //     if recursion == Recursion.Recursive then children.foreach(_.setWritable(recursion))

  //   def setWritable(recursion: Recursion): Unit throws UnchangeablePermissions =
  //     if !javaFile.setWritable(true) then throw UnchangeablePermissions(path)
  //     if recursion == Recursion.Recursive then children.foreach(_.setWritable(recursion))

  //   def uniquify(): Path =
  //     if !exists() then path else LazyList.from(2).map { i => rename(_+"-"+i) }.find(!_.exists()).get

  //   def directory: Boolean = Files.isDirectory(javaPath)

  //   def extantParents(): Path =
  //     parent.mkdir()
  //     path

  //   def executable: Boolean = Files.isExecutable(javaPath)
  //   def readable: Boolean = Files.isReadable(javaPath)
  //   def writable: Boolean = Files.isWritable(javaPath)

  //   def setExecutable(exec: Boolean): Unit throws UnchangeablePermissions =
  //     try javaFile.setExecutable(exec).unit catch e => throw UnchangeablePermissions(path)

  //   def moveTo(dest: Path): Unit throws NotWritable =
  //     try
  //       path.parent.extant()
  //       Files.move(javaPath, dest.javaPath, StandardCopyOption.REPLACE_EXISTING).unit
  //     catch
  //       case e: DirectoryNotEmptyException =>
  //         copyTo(dest)
  //         delete().unit
  //       case e =>
  //         throw NotWritable(dest)

  //   def relativeSubdirsContaining(pred: String => Boolean): Set[Path] =
  //     findSubdirsContaining(pred).map { p => Path(p.filename.drop(path.length + 1)) }

  //   def unlink(): Unit throws NotSymbolicLink | NotWritable =
  //     try if Files.isSymbolicLink(javaPath) then Files.delete(javaPath) else throw NotSymbolicLink(path)
  //     catch e => throw NotWritable(path)

  //   def rename(fn: String => String): Path = parent / fn(name)
    
  //   def symlinkTo(target: Path): Unit throws NotWritable =
  //     try Files.createSymbolicLink(target.javaPath, javaPath)
  //     catch case e: ji.IOException => throw NotWritable(path)

object Filesystem:
  lazy val roots: Set[Filesystem] =
    Option(ji.File.listRoots).fold(Set())(_.nn.unsafeImmutable.to(Set)).map(_.nn.getAbsolutePath.nn)
        .collect:
      case "/" =>
        Unix
      
      case s"""$drive:\""" if drive.length == 1 =>
        val letter = try drive.charAt(0) catch case e: OutOfRangeError => throw Impossible(e)
        letter.toUpper match
          case ch: Majuscule =>
            WindowsRoot(ch)
          
          case ch =>
            throw Impossible(s"a drive letter with an unexpected name was found: '$ch'")
    .to(Set)
 
  def defaultSeparator: "/" | "\\" = if ji.File.separator == "\\" then "\\" else "/"

object Unix extends Filesystem(t"/", t"/"):
  def Pwd: DiskPath throws PwdError =
    val dir = try Sys.user.dir().show catch case e: KeyNotFoundError => throw PwdError()
    makeAbsolute(parse(dir).get.parts)

case class WindowsRoot(drive: Majuscule) extends Filesystem(t"\\", t"${drive}:\\")
object windows:
  object DriveC extends WindowsRoot('C')
  object DriveD extends WindowsRoot('D')
  object DriveE extends WindowsRoot('E')
  object DriveF extends WindowsRoot('F')

given realm: Realm = Realm(t"jovian")
