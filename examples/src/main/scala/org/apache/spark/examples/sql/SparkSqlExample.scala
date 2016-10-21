package org.apache.spark.examples.sql
import scala.collection.mutable.{ ListBuffer, Queue }

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext

case class Person(name: String, age: Int)

/**
 * SQL ���� 
 */
object SparkSqlExample {
  def main(args: Array[String]) {
    val conf = sys.env.get("SPARK_AUDIT_MASTER") match {
      case Some(master) => new SparkConf().setAppName("Simple Sql App").setMaster(master)
      case None         => new SparkConf().setAppName("Simple Sql App").setMaster("local")
    }
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    import sqlContext.implicits._
    import sqlContext._
    //����RDD,ת���ɶ���
    val people = sc.makeRDD(1 to 100, 10).map(x => Person(s"Name$x", x)).toDF()
    people.registerTempTable("people")//ע����� 
    //����DataFrame
    val teenagers = sql("SELECT name,age FROM people WHERE age >= 13 AND age <= 19")
    //���� teenagerNames: Array[String]
    val teenagerNames = teenagers.map(t => "Name: " + t(0)+ " age:"+t(1)).collect()
    teenagerNames.foreach(println)

    def test(f: => Boolean, failureMsg: String) = {
      if (!f) {
        println(failureMsg)
        System.exit(-1)
      }
    }
    //����������ѡ��Ԫ��
    test(teenagerNames.size == 7, "Unexpected number of selected elements: " + teenagerNames)
    println("Test succeeded")
    sc.stop()
  }

}