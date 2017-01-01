package org.apache.spark.examples.futuePromise
import scala.concurrent._
import ExecutionContext.Implicits.global
/**
 * Future ��ʾһ�����ܻ�û��ʵ����ɵ��첽����Ľ��
 * scala��Future��ʾһ���첽�����Ľ��״̬,������һ��ֵ,��δ����ĳ��֮������,��ֵ���첽�����Ľ��,
 * ���첽����û�����,��ôFuture��isCompletedΪfalse,���첽����������ҷ�����ֵ,
 * ��ôFuture��isCompleted����true����success,
 * ����첽����û����ɻ����쳣��ֹ,��ôFuture��isCompletedҲ����true������failed.
 * Future��ֵ��֪��ʲôʱ�����,������Ҫһ�ֻ�������ȡ�첽�����Ľ��,
 * һ���ǲ�ͣ�Ĳ鿴Future�����״̬,��һ�����������ķ�ʽ,
 * scala�ṩ�˵ڶ��ַ�ʽ��֧��,ʹ��scala.concurrent.Await,
 * ������������,һ����Await.ready,��Future��״̬Ϊ���ʱ����,һ����Await.result,ֱ�ӷ���Future���еĽ����
 * Future���ṩ��һЩmap,filter,foreach�Ȳ���
 */
object FutureTestextendsApp extends App {
  val s = "Hello"
  val f: Future[String] = future {
    s + " future!"
  }
  f onSuccess {
    case msg => println(msg)
  }
  println(s) //�������, f onSuccess�Ͳ�ִ��
}