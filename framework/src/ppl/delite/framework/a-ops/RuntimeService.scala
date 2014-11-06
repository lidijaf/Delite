package ppl.delite.framework.ops

import java.io.PrintWriter
import scala.virtualization.lms.common._
import scala.reflect.{SourceContext, RefinedManifest}
import scala.virtualization.lms.internal.{GenerationFailedException, GenericFatCodegen}
import ppl.delite.framework.ops._
import ppl.delite.framework.Util._
import ppl.delite.framework.Config

trait RuntimeServiceOps extends Base {
  lazy val DELITE_NUM_THREADS = runtime_query_numthreads()
  lazy val DELITE_NUM_SOCKETS = runtime_query_numsockets()
  lazy val DELITE_THREADS_PER_SOCKET = runtime_query_threadspersocket()

  def runtime_query_numthreads()(implicit ctx: SourceContext): Rep[Int]
  def runtime_query_numsockets()(implicit ctx: SourceContext): Rep[Int]
  def runtime_query_threadspersocket()(implicit ctx: SourceContext): Rep[Int]
}

trait RuntimeServiceOpsExp extends RuntimeServiceOps with EffectExp {
  this: DeliteOpsExp =>

  case class RuntimeQueryNumThreads() extends Def[Int]
  case class RuntimeQueryNumSockets() extends Def[Int]
  case class RuntimeQueryThreadsPerSocket() extends Def[Int]

  def runtime_query_numthreads()(implicit ctx: SourceContext) = reflectPure(RuntimeQueryNumThreads())
  def runtime_query_numsockets()(implicit ctx: SourceContext) = reflectPure(RuntimeQueryNumSockets())
  def runtime_query_threadspersocket()(implicit ctx: SourceContext) = reflectPure(RuntimeQueryThreadsPerSocket())

  override def mirror[A:Manifest](e: Def[A], f: Transformer)(implicit pos: SourceContext): Exp[A] = (e match {
    case RuntimeQueryNumThreads() => runtime_query_numthreads()(pos)
    case RuntimeQueryNumSockets() => runtime_query_numsockets()(pos)
    case RuntimeQueryThreadsPerSocket() => runtime_query_threadspersocket()(pos)

    case Reflect(RuntimeQueryNumThreads(), u, es) => reflectMirrored(Reflect(RuntimeQueryNumThreads(), mapOver(f,u), f(es)))(mtype(manifest[A]), pos)
    case Reflect(RuntimeQueryNumSockets(), u, es) => reflectMirrored(Reflect(RuntimeQueryNumSockets(), mapOver(f,u), f(es)))(mtype(manifest[A]), pos)
    case Reflect(RuntimeQueryThreadsPerSocket(), u, es) => reflectMirrored(Reflect(RuntimeQueryThreadsPerSocket(), mapOver(f,u), f(es)))(mtype(manifest[A]), pos)
    case _ => super.mirror(e,f)
  }).asInstanceOf[Exp[A]]
}

trait CGenRuntimeServiceOps extends CGenEffect {
  val IR: RuntimeServiceOpsExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case RuntimeQueryNumThreads() => emitValDef(sym, "config->numThreads")
    case RuntimeQueryNumSockets() => emitValDef(sym, "config->numSockets")
    case RuntimeQueryThreadsPerSocket() => emitValDef(sym, "config->numCoresPerSocket")
    case _ => super.emitNode(sym, rhs)
  }
}

trait ScalaGenRuntimeServiceOps extends ScalaGenEffect {
  val IR: RuntimeServiceOpsExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case RuntimeQueryNumThreads() => stream.println("println(\"NUMA-aware don't work in scala\")")
    case RuntimeQueryNumSockets() => stream.println("println(\"NUMA-aware don't work in scala\")")
    case RuntimeQueryThreadsPerSocket() => stream.println("println(\"NUMA-aware don't work in scala\")")
    case _ => super.emitNode(sym, rhs)
  }
}
