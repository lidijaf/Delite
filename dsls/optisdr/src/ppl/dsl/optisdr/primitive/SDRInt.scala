package ppl.dsl.optisdr.primitive

import scala.reflect.SourceContext
import scala.virtualization.lms.common._

import ppl.dsl.optisdr._

trait SDRIntOps extends Variables {
  this: OptiSDR =>
  
  implicit def repToSDRIntOps(x: Rep[Int]) = new SDRIntOpsCls(x)
  implicit def varToSDRIntOps(x: Var[SDRInt]) = new SDRIntOpsCls(readVar(x))
  
  // Objects methods
  class SDRIntOpsCls(x: Rep[Int]) {
    def <<(b: Rep[Int])(implicit ctx: SourceContext) = sdrint_lshift(a, b)
    def <<<(b: Rep[Int])(implicit ctx: SourceContext) = sdrint_lshift(a, b)
    def >>(b: Rep[Int])(implicit ctx: SourceContext) = sdrint_rshift(a, b)
    def >>>(b: Rep[Int])(implicit ctx: SourceContext) = sdrint_rashift(a, b)
  }

  def sdrint_lshift(a: Rep[Int], b: Rep[Int])(implicit ctx: SourceContext) : Rep[Int]
  def sdrint_rshift(a: Rep[Int], b: Rep[Int])(implicit ctx: SourceContext) : Rep[Int]
  def sdrint_rashift(a: Rep[Int], b: Rep[Int])(implicit ctx: SourceContext) : Rep[Int]
}

trait SDRIntOpsExp extends SDRIntOps {
  this: OptiSDRExp =>

  case class SDRIntLShift(a: Exp[Int], b: Rep[Int]) extends Def[Int]
  case class SDRIntRShift(a: Exp[Int], b: Rep[Int]) extends Def[Int]
  case class SDRIntRAShift(a: Exp[Int], b: Rep[Int]) extends Def[Int]
  
  def sdrint_lshift(a: Rep[Int], b: Rep[Int])(implicit ctx: SourceContext) = reflectPure(SDRIntLShift(a,b))
  def sdrint_rshift(a: Rep[Int], b: Rep[Int])(implicit ctx: SourceContext) = reflectPure(SDRIntRShift(a,b))
  def sdrint_rashift(a: Rep[Int], b: Rep[Int])(implicit ctx: SourceContext) = reflectPure(SDRIntRAShift(a,b))
}


trait SDRIntOpsExpOpt extends SDRIntOpsExp {
  this: OptiSDRExp =>
}