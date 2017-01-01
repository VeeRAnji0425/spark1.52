package org.apache.spark.examples

import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket
import java.net.URI
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import scala.util.Try
import scala.util.Failure
import scala.util.Success
/**
 * �����Ƿ����ӵ����������
 */
object SparkAvailabilityCheck {
  def main(args: Array[String]) {
    val b39=isSparkOnline(URI.create("spark://192.168.0.39:7079"))
    val b27=isSparkOnline(URI.create("spark://192.168.0.27:7079"))
    println("Connect-> b39:"+b39+"\t b27:"+b27)
    //local[*]
     SparkUtility.Connect(SparkUtility.Conf("spark://mzhy:7079", "appname")) match {
      case Success(sc) => println("Success")
      case Failure(f) => println("Failure")
    }
  } 
  
  /**
   * ����TCP��������
   */
  def isSparkOnline(masterLocation: URI): Boolean = {
    try {
      val host = InetAddress.getByName(masterLocation.getHost)
      val socket = new Socket(host, masterLocation.getPort)
      socket.close()
      true
    } catch {
      case ex: ConnectException =>false       
    }
  }
}
 /**
  * Spark ������������
  */
object SparkUtility {
  def Conf(url: String, app: String): SparkConf =
    new SparkConf().setMaster(url).setAppName(app)

  def Connect(conf: SparkConf): Try[SparkContext] =
    Try(new SparkContext(conf))
}