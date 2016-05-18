package org.apache.spark.examples

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

object SparkRDDAPIExamples {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setMaster("local").setAppName("Spark Exercise: Spark Version Word Count Program");
    val sc = new SparkContext(conf);
    //��������
    sc.setCheckpointDir("my_directory_name")
    val a = sc.parallelize(1 to 4)
    a.checkpoint //�������,�洢Ŀ¼
    a.count
   //CollectAsMap����
    val a1 = sc.parallelize(List(1, 2, 1, 3), 1)
    //zip�������������Ĳ�������Ӧλ���ϵ�Ԫ�����һ��pair���顣
    //�������һ������Ԫ�رȽϳ�����ô����Ĳ����ᱻɾ��
    val b = a1.zip(a1)
    b.collectAsMap //Map(2 -> 2, 1 -> 1, 3 -> 3)
    println(b.collectAsMap)
    //sortBy ����
    val data = List(3,1,90,3,5,12)

    val rdd = sc.parallelize(data)
    rdd.collect
    //Ĭ������,Array(1, 3, 3, 5, 12, 90)
   rdd.sortBy(x => x).collect
   //ʹ�ý���,Array(90, 12, 5, 3, 3, 1)
   rdd.sortBy(x => x, false).collect.toString()
 //


  }
}