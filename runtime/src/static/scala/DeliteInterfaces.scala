package generated.scala


/**
 * Delite
 */

abstract class DeliteOpMultiLoop[A] {
  def size: Long
  var loopStart: Long
  var loopSize: Long
  def alloc: A
  def processRange(__act: A, start: Long, end: Long, tid: Int): A //init+process
  def combine(__act: A, rhs: A, tid: Int): Unit
  def postCombine(__act: A, rhs: A, tid: Int): Unit
  def postProcInit(__act: A, tid: Int): Unit
  def postProcess(__act: A, tid: Int): Unit
  def finalize(__act: A, tid: Int): Unit
  def initAct: A
}

/**
 * Ref
 */

class Ref[@specialized T](v: T) {
  private[this] var _v = v

  def get = _v
  def set(v: T) = _v = v
}
