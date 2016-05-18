package org.apache.spark.examples

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.util._

object SparkWordCount {
  def FILE_NAME: String = "word_count_results_";
  def main(args: Array[String]) {
    /* if (args.length < 1) {
      println("Usage:SparkWordCount FileName");
      System.exit(1);
    }*/

    /*    
    val creationSite: CallSite = Utils.getCallSite()

  println(">>>>>>>>>>>"+creationSite.longForm)*/

    val conf = new SparkConf().setMaster("local").setAppName("Spark Exercise: Spark Version Word Count Program");
    val sc = new SparkContext(conf);
    val textFile = sc.textFile("people.txt");//textFile������һ��HadoopRDD.map,������һ��MapPartitionsRDD
    //flatMap��ǰ�����ɵ�MapPartitionsRDD[String]������flatMap������������MapPartitionsRDD[String] 
    val wordCounts = textFile.flatMap(line => line.split(" ")).map(
      word => (word, 1))
    /**
     * wordCounts.collect().foreach(e => {
     * val (k,v) = e
     * println(k+"="+v)
     * });
     *
     *
     * val wordcount = textFile.flatMap(_.split(' ')).map((_,1)).reduceByKey(_+_)
     * .map(x => (x._2, x._1))//K V����λ��
     * .sortByKey(false)//K��ʾ��������
     * .map(x => (x._2, x._1))//K V����λ��
     */
    val tsets = wordCounts.reduceByKey((a, b) => a + b) //reduceByKey(_+_)����һ�� 
    //print the results,for debug use.
    //println("Word Count program running results:");
    /* 
 tsets.collect().foreach(e => {
    val (k,v) = e
     println(k+"="+v)
    });
    */

   wordCounts.saveAsTextFile(FILE_NAME + System.currentTimeMillis());
    println("Word Count program running results are successfully saved.");
  }
}