package org.apache.spark.examples.futuePromise

import java.util.Timer
import java.util.TimerTask
import scala.concurrent._
import ExecutionContext.Implicits.global
/**
 * Scala��Promise���Ա���Ϊһ���ǿ�д��,��̬����ֵ,
 * �����Դ���һ��Future�����һ��Future(success complete��failed complete)promise������˼�ǳ�ŵ,
 * ��Ϊ�����Կ����첽�����Ľ��
 */
object PromiseTimedEventTestApp extends App {
  val timer = new Timer
  /** Return a Future which completes successfully with the supplied value after secs seconds. */
  def delayedSuccess[T](secs: Int, value: T): Future[T] = {
    val result = Promise[T]
    timer.schedule(new TimerTask() {
      def run() = {
        result.success(value)
      }
    }, secs * 10)
    result.future
  }
  /**
   * Return a Future which completes failing with an IllegalArgumentException after secs
   * seconds.
   */
  def delayedFailure(secs: Int, msg: String): Future[Int] = {
    val result = Promise[Int]
    timer.schedule(new TimerTask() {
      def run() = {
        result.failure(new IllegalArgumentException(msg))
      }
    }, secs * 10)
    result.future
  }
  delayedSuccess(1,timer)
  delayedFailure(2,"delayedFailure")
}