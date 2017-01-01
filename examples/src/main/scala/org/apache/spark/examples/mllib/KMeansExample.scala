package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.linalg.Vectors
/**
 * �����㷨:�ǰ����ݻ��ֳɶ����,����һ��������������������������
 * �ලѧϰ�ñ�Ǻõ�����ȥѵ���㷨,�޼ලѧϰ���㷨�Լ�ȥ���ڲ��ṹ
 * ʹ�ü����еķ�������ռ������ͷ��ݼ۸�
 * ռ�����|���ݼ۸�
 * 12839	 |2405
 * 10000	 |2200
 * 8040		 |1400
 * 13104	 |1800
 * 10000	 |2351
 * 3049		 |795
 * 38768	 |2725
 * 16250	 |2150
 * 43026	 |2724
 * 44431	 |2675
 */
object KMeansExample {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("KMeansClustering")
    val sc = new SparkContext(sparkConf)
    //����saratoga��RDD
    val data = sc.textFile("../data/mllib/saratoga.csv")
    //������ת�����ܼ�������RDD
   val parsedData = data.map( line => Vectors.dense(line.split(',').map(_.toDouble)))
   //��4���غ�5�ε���ѵ��ģ��
   val kmmodel= KMeans.train(parsedData,4,5)
   //��parsedData�����ռ��������ݼ�
   val houses = parsedData.collect
   //Ԥ���1��Ԫ�صĴ�,KMeans�㷨���0�����ص�ID,
   val prediction1 = kmmodel.predict(houses(0))
   //Ԥ��houses(18)������,ռ�����876,�۸�66.5�����Ǹ���
   val prediction2 = kmmodel.predict(houses(18))
   //Ԥ��houses(35)������,ռ�����15750,�۸�112�����Ǹ���
   val prediction3 = kmmodel.predict(houses(35))
   //Ԥ��houses(6)������,ռ�����38768,�۸�272�����Ǹ���
   val prediction4 = kmmodel.predict(houses(6))
   //Ԥ��houses(15)������,ռ�����69696,�۸�275�����Ǹ���
   val prediction5 = kmmodel.predict(houses(15))
    
  }
 
}