package ppl.delite.framework.analysis

import scala.virtualization.lms.internal.FatBlockTraversal
import ppl.delite.framework.ops.{DeliteOpsExp, DeliteFileReaderOpsExp, DeliteCollection}
import ppl.delite.framework.datastructures.{DeliteArrayOpsExp, DeliteStructsExp}

trait DistributedArrayAnalysis extends FatBlockTraversal {
  val IR: DeliteOpsExp with DeliteArrayOpsExp with DeliteStructsExp with DeliteFileReaderOpsExp
  import IR._

  val distributedMode = true //TODO: config flag

  //checks for allowed IR nodes on distributed collections and tags them as distributed (forwards tags through IR)
  //any other IR node that consumes a distributed collection is considered illegal: throws error to user
  override def traverseStm(stm: Stm) = stm match {   
    //only multiloops can consume and produce distributed collections
    case TP(sym, d@Loop(_,_,_)) => markPartitioned(sym,d)
    //whitelist: the following ops are always allowed because necessary metadata is available on master
    case TP(sym, DeliteArrayLength(_)) =>
    case TP(sym, Field(_,_)) =>
    case TP(sym, Struct(_,_)) => 
    //disallow everything else
    case TP(sym, d) => checkIllegalUsage(sym,d)
    case s => throw new RuntimeException("unhandled Stm type in distributed array analysis: " + s)
  } 

  def markPartitioned[T](sym: Sym[T], d: Def[T]) {
    def inputIsPartitioned = syms(d).exists(s => s match {
      case Partitionable(t) => t.partition
      case _ => false
    })

    def setPartitioned() = sym match {
      case Partitionable(t) => t.partition = true
      case _ => throw new RuntimeException("tried to set " + d + " as partitioned by no PartitionTag exists") 
    }

    if (inputIsPartitioned) {
      checkAccessStencil(sym,d)
      d match {
        case Loop(_,_,_:DeliteCollectElem[_,_,_]) => setPartitioned()
        case _ => //other loop types (Reduce) will produce result on master
      }
    }
  }

  def isFileReaderLoop[T](d: Def[T]) = {
    syms(d).exists(_.tp == manifest[DeliteFileStream])
  }

  def checkAccessStencil[T](sym: Sym[T], d: Def[T]) {
    //TODO: integrate stencil analysis here (and again during codegen)
    //phase ordering issues? fusion + other optimizations could change stencil
    //TODO: need to special case the stencil for FileReader nodes + special logic in runtime scheduler... tag and/or look for Stream type?
  }

  def checkIllegalUsage[T](sym: Sym[T], d: Def[T]) {
    if (distributedMode) { //allow arbitrary consumers for numa but not clusters
      syms(d).foreach(s => s match {
        case Partitionable(t) if t.partition => throw new RuntimeException("Illegal Operation at " + sym.pos.head + ": " + d.toString + " cannot be applied to distributed collection at " + s.pos.head)
        case _ => //ignore
      })
    }
  }

  object Partitionable {
    def unapply[T](e: Exp[T]): Option[PartitionTag[T]] = unapplyPartionable(e)
  }

  def unapplyPartionable[T](e: Exp[T]): Option[PartitionTag[T]] = e match {
    case Def(Struct(tag,_)) => tag match {
      case p: PartitionTag[_] => Some(p)
      case _ => None
    }
    case Def(DeliteArrayNew(_,_,tag)) => Some(tag)
    case Def(Reflect(DeliteArrayNew(_,_,tag),_,_)) => Some(tag)
    case Def(Loop(_,_,body:DeliteCollectElem[_,_,_])) => unapplyPartionable(getBlockResult(body.buf.alloc))
    case Def(Loop(_,_,body:DeliteHashReduceElem[_,_,_,_])) => unapplyPartionable(getBlockResult(body.buf.alloc))
    case _ => None
  }

}
