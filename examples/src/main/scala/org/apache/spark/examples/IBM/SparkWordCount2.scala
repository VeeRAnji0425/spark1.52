package org.apache.spark.examples.IBM
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

object SparkWordCount2 {
  def FILE_NAME: String = "word_count_results_";
  def main(args: Array[String]) {
   /* if (args.length < 1) {
      println("Usage:SparkWordCount FileName");
      System.exit(1);
    }*/
    
    
    
    val conf = new SparkConf().setMaster("local").setAppName("Spark Exercise: Spark Version Word Count Program");
    val sc = new SparkContext(conf);
    
    val textFile = sc.textFile("people.txt");
    
    //flatMap��map���ƣ�������ԭRDD�е�Ԫ�ؾ�map�����ֻ������һ��Ԫ�أ���ԭRDD�е�Ԫ�ؾ�flatmap���������ɶ��Ԫ����������RDD��
    val wordCounts = textFile.flatMap(line => line.split(" ")).map(
      word => (word, 1))
       wordCounts.foreach(e => {
    //val (k,v) = e
     println(e._1+"="+e._2)
    });
    
    /**
     * scala> val a = sc.parallelize(List((1,2),(3,4),(3,6)))
		 * scala> a.reduceByKey((x,y) => x + y).collect
		 * res7: Array[(Int, Int)] = Array((1,2), (3,10))
     */
    
     ///educeByKey���Ƕ�Ԫ��ΪKV�Ե�RDD��Key��ͬ��Ԫ�ص�Value����reduce��
     //Key��ͬ�Ķ��Ԫ�ص�ֵ��reduceΪһ��ֵ��Ȼ����ԭRDD�е�Key���һ���µ�KV��
  
    val tsets=  wordCounts.reduceByKey(_+_)//educeByKey��Key��ͬ��Ԫ�ص�ֵ���
    //print the results,for debug use.
    //println("Word Count program running results:");
    //collect��RDDת��Scala���飬������
   tsets.collect().foreach(e => {
    val (k,v) = e
     println(k+"="+v)
    });
    
   wordCounts.saveAsTextFile(FILE_NAME + System.currentTimeMillis());
    println("Word Count program running results are successfully saved.");
  }
}