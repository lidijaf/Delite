package ppl.delite.framework.ops

import scala.virtualization.lms.internal.GenerationFailedException
import scala.virtualization.lms.common._
import scala.reflect.{SourceContext, RefinedManifest}
import ppl.delite.framework.datastructures._
import ppl.delite.framework.Config

trait DeliteFileStream

trait DeliteFileReaderOps extends Base with DeliteArrayBufferOps {
  
  object DeliteFileReader {
    def readLines[A:Manifest](paths: Rep[String]*)(f: Rep[String] => Rep[A])(implicit pos: SourceContext) = dfr_readLines(paths, f)
    def readLinesFlattened[A:Manifest](paths: Rep[String]*)(f: Rep[String] => Rep[DeliteCollection[A]])(implicit pos: SourceContext) = dfr_readLinesFlattened(paths, f)
    //def readBytes[A:Manifest](paths: Rep[String]*)(delimiter: Rep[DeliteArray[Byte]])(f: Rep[DeliteArray[Byte]] => Rep[A])(implicit pos: SourceContext) = dfr_readBytes(paths, delimiter, f)
    //def readBytes[A:Manifest](paths: Rep[String]*)(delimiter: Rep[DeliteArray[Byte]])(f: Rep[DeliteArray[Byte]] => Rep[DeliteCollection[A]])(implicit pos: SourceContext) = dfr_readBytesFlattened(paths, delimiter, f)
  }
  def dfr_readLines[A:Manifest](paths: Seq[Rep[String]], f: Rep[String] => Rep[A])(implicit pos: SourceContext): Rep[DeliteArray[A]]
  def dfr_readLinesFlattened[A:Manifest](paths: Seq[Rep[String]], f: Rep[String] => Rep[DeliteCollection[A]])(implicit pos: SourceContext): Rep[DeliteArray[A]]
  //def dfr_readBytes[A:Manifest](paths: Seq[Rep[String]], delimiter: Rep[Array[Byte]], f: Rep[DeliteArray[Byte]] => Rep[A])(implicit pos: SourceContext): Rep[DeliteArray[A]]
  //def dfr_readBytesFlattened[A:Manifest](paths: Seq[Rep[String]], delimiter: Rep[Array[Byte]], f: Rep[DeliteArray[Byte]] => Rep[DeliteCollection[A]])(implicit pos: SourceContext): Rep[DeliteArray[A]]

  object DeliteFileStream {
    def apply(paths: Seq[Rep[String]])(implicit pos: SourceContext) = dfs_new(paths)
    //def apply(paths: Seq[Rep[String]], delimiter: Rep[DeliteArray[Byte]])(implicit pos: SourceContext) = dfs_new(paths,delimiter)
  }
  def dfs_new(paths: Seq[Rep[String]])(implicit pos: SourceContext): Rep[DeliteFileStream]

}

trait DeliteFileReaderOpsExp extends DeliteFileReaderOps with DeliteArrayOpsExpOpt with DeliteArrayBufferOpsExp with DeliteOpsExp with DeliteMapOpsExp {

  //Note: DeliteFileStream cannot currently be used outside of a MultiLoop because openAtNewLine() is not exposed
  case class DeliteFileStreamNew(paths: Seq[Exp[String]]) extends Def[DeliteFileStream]
  def dfs_new(paths: Seq[Exp[String]])(implicit pos: SourceContext) = reflectPure(DeliteFileStreamNew(paths))

  case class DeliteFileStreamReadLine(stream: Exp[DeliteFileStream], idx: Exp[Long]) extends Def[String]
  def dfs_readLine(stream: Exp[DeliteFileStream], idx: Exp[Long]): Exp[String] = reflectPure(DeliteFileStreamReadLine(stream, idx))

  case class DeliteFileStreamSize(stream: Exp[DeliteFileStream]) extends Def[Long]
  def dfs_size(stream: Exp[DeliteFileStream]): Exp[Long] = reflectPure(DeliteFileStreamSize(stream))

  def dfr_readLines[A:Manifest](paths: Seq[Exp[String]], f: Exp[String] => Exp[A])(implicit pos: SourceContext) = reflectPure(DeliteOpFileReader(paths, f))
  def dfr_readLinesFlattened[A:Manifest](paths: Seq[Exp[String]], f: Exp[String] => Exp[DeliteCollection[A]])(implicit pos: SourceContext) = reflectPure(DeliteOpFileReaderFlat(paths, f))

  case class DeliteOpFileReader[A:Manifest](paths: Seq[Exp[String]], func: Exp[String] => Exp[A]) extends DeliteOpFileReaderI[A,DeliteArray[A],DeliteArray[A]] {
    val inputStream = DeliteFileStream(paths)
    val size = copyTransformedOrElse(_.size)(dfs_size(inputStream))
    def finalizer(x: Exp[DeliteArray[A]]) = x
    override def alloc(len: Exp[Long]) = DeliteArray[A](len)
  }

  abstract class DeliteOpFileReaderI[A:Manifest, I<:DeliteCollection[A]:Manifest, CA<:DeliteCollection[A]:Manifest]
    extends DeliteOpMapLike[A,I,CA] {
    type OpType <: DeliteOpFileReaderI[A,I,CA]

    val inputStream: Exp[DeliteFileStream]
    def func: Exp[String] => Exp[A]

    lazy val body: Def[CA] = copyBodyOrElse(DeliteCollectElem[A,I,CA](
      func = reifyEffects(func(dfs_readLine(inputStream, v))),
      par = dc_parallelization(allocVal, true),
      buf = this.buf
    ))

    val dmA = manifest[A]
    val dmI = manifest[I]
    val dmCA = manifest[CA]
  }

  case class DeliteOpFileReaderFlat[A:Manifest](paths: Seq[Exp[String]], func: Exp[String] => Exp[DeliteCollection[A]]) extends DeliteOpFileReaderFlatI[A,DeliteArray[A],DeliteArray[A]] {
    val inputStream = DeliteFileStream(paths)
    val size = copyTransformedOrElse(_.size)(dfs_size(inputStream))
    def finalizer(x: Exp[DeliteArray[A]]) = x
    override def alloc(len: Exp[Long]) = DeliteArray[A](len)
  }

  abstract class DeliteOpFileReaderFlatI[A:Manifest, I<:DeliteCollection[A]:Manifest, CA<:DeliteCollection[A]:Manifest]
    extends DeliteOpMapLike[A,I,CA] {
    type OpType <: DeliteOpFileReaderFlatI[A,I,CA]

    val inputStream: Exp[DeliteFileStream]
    def func: Exp[String] => Exp[DeliteCollection[A]]

    final lazy val iFunc: Exp[DeliteCollection[A]] = copyTransformedOrElse(_.iFunc)(func(dfs_readLine(inputStream,v)))
    final lazy val iF: Sym[Long] = copyTransformedOrElse(_.iF)(fresh[Long]).asInstanceOf[Sym[Long]]
    final lazy val eF: Sym[DeliteCollection[A]] = copyTransformedOrElse(_.eF)(fresh[DeliteCollection[A]](iFunc.tp)).asInstanceOf[Sym[DeliteCollection[A]]]

    lazy val body: Def[CA] = copyBodyOrElse(DeliteCollectElem[A,I,CA](
      iFunc = Some(reifyEffects(this.iFunc)),
      iF = Some(this.iF),
      sF = Some(reifyEffects(dc_size(eF))), //note: applying dc_size directly to iFunc can lead to iFunc being duplicated (during mirroring?)
      eF = Some(this.eF),
      func = reifyEffects(dc_apply(eF,iF)),
      par = dc_parallelization(allocVal, true),
      buf = this.buf
    ))

    val dmA = manifest[A]
    val dmI = manifest[I]
    val dmCA = manifest[CA]
  }

  override def mirror[A:Manifest](e: Def[A], f: Transformer)(implicit ctx: SourceContext): Exp[A] = (e match {
    case e@DeliteOpFileReader(paths, func) => reflectPure(new { override val original = Some(f,e) } with DeliteOpFileReader(f(paths),f(func))(e.dmA))(mtype(manifest[A]),implicitly[SourceContext])
    case Reflect(e@DeliteOpFileReader(paths,func), u, es) => reflectMirrored(Reflect(new { override val original = Some(f,e) } with DeliteOpFileReader(f(paths),f(func))(e.dmA), mapOver(f,u), f(es)))(mtype(manifest[A]), ctx)
    case e@DeliteOpFileReaderFlat(paths, func) => reflectPure(new { override val original = Some(f,e) } with DeliteOpFileReaderFlat(f(paths),f(func))(e.dmA))(mtype(manifest[A]),implicitly[SourceContext])
    case Reflect(e@DeliteOpFileReaderFlat(paths,func), u, es) => reflectMirrored(Reflect(new { override val original = Some(f,e) } with DeliteOpFileReaderFlat(f(paths),f(func))(e.dmA), mapOver(f,u), f(es)))(mtype(manifest[A]), ctx)
    case DeliteFileStreamNew(paths) => dfs_new(f(paths))
    case Reflect(DeliteFileStreamNew(paths), u, es) => reflectMirrored(Reflect(DeliteFileStreamNew(f(paths)), mapOver(f,u), f(es)))(mtype(manifest[A]), ctx)
    case DeliteFileStreamReadLine(stream,idx) => dfs_readLine(f(stream),f(idx))
    case Reflect(DeliteFileStreamReadLine(stream,idx), u, es) => reflectMirrored(Reflect(DeliteFileStreamReadLine(f(stream), f(idx)), mapOver(f,u), f(es)))(mtype(manifest[A]), ctx)
    case DeliteFileStreamSize(stream) => dfs_size(f(stream))
    case Reflect(DeliteFileStreamSize(stream), u, es) => reflectMirrored(Reflect(DeliteFileStreamSize(f(stream)), mapOver(f,u), f(es)))(mtype(manifest[A]), ctx)
    case _ => super.mirror(e,f)
  }).asInstanceOf[Exp[A]]

}

trait ScalaGenDeliteFileReaderOps extends ScalaGenFat {
  val IR: DeliteFileReaderOpsExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case DeliteFileStreamNew(paths) =>
      emitValDef(sym, "generated.scala.io.FileStreamImpl(" + paths.map(quote).mkString(",") + ")")
    case DeliteFileStreamReadLine(stream,idx) =>
      emitValDef(sym, quote(stream) + "_stream.readLine()")
    case DeliteFileStreamSize(stream) =>
      emitValDef(sym, quote(stream) + ".size")
    case _ => super.emitNode(sym, rhs)
  }

  override def remap[A](m: Manifest[A]): String = m.erasure.getSimpleName match {
    case "DeliteFileStream" => "generated.scala.io.FileStreamImpl"
    case _ => super.remap(m)
  }

}

trait CGenDeliteFileReaderOps extends CGenFat {
  val IR: DeliteFileReaderOpsExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case DeliteFileStreamNew(paths) =>
      // C++ variable length args does not allow string types, so use underlying char *
      emitValDef(sym, "new cppFileStream(" + paths.length + "," + paths.map(quote(_) + ".c_str()").mkString(",") + ")")
    case DeliteFileStreamReadLine(stream,idx) =>
      emitValDef(sym, quote(stream) + "_stream->readLine(" + quote(idx) + ")")
    case DeliteFileStreamSize(stream) =>
      emitValDef(sym, quote(stream) + "->size")
    case _ => super.emitNode(sym, rhs)
  }

  override def remap[A](m: Manifest[A]): String = m.erasure.getSimpleName match {
    case "DeliteFileStream" => "cppFileStream"
    case _ => super.remap(m)
  }

  override def getDataStructureHeaders(): String = {
    val out = new StringBuilder
    out.append("#include \"cppFileStream.h\"\n")
    super.getDataStructureHeaders() + out.toString
  }

}
