package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
/**
 * �ֲ������Stratified sampling��
 * ��С��ʹ��spark�Դ���README.md�ļ�������Ӧ����ʾ����
 */
object StratifiedSampleDemo {
  def main(args: Array[String]) {
    //val sparkConf = new SparkConf().setMast("local[2]").setAppName("SparkHdfsLR")

    val conf = new SparkConf().setAppName("test").setMaster("local")
    val sc = new SparkContext(conf)
    //��ȡHDFS�ϵ�README.md�ļ�
    val textFile = sc.textFile("/README.md")
    //wordCount����,���أ�K,V)���ܽ��
    val wordCounts = textFile.flatMap(line => line.split(" ")).map(word => (word, 1)).reduceByKey((a, b) => a + b)

    //����keyΪspark,��������Ϊ0.5
    val fractions: Map[String, Double] = Map("Spark" -> 0.5)

    //ʹ��sampleByKey�������в���
    val approxSample = wordCounts.sampleByKey(false, fractions)
    //ʹ��sampleByKeyExact�������в���,�÷�����Դ���Ľ�sampleByKey����
    //��������Ĵ�С��Ԥ�ڴ�С���ӽ�,���Ŷȴﵽ99.99%
    val exactSample = wordCounts.sampleByKeyExact(false, fractions)
  }
}