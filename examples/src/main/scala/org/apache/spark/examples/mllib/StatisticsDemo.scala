package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.stat.MultivariateStatisticalSummary
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.stat.Statistics

object StatisticsDemo {
  def main(args: Array[String]) {
    //val sparkConf = new SparkConf().setMast("local[2]").setAppName("SparkHdfsLR")

    val conf = new SparkConf().setAppName("test").setMaster("local")
    val sc = new SparkContext(conf)

    val rdd1 = sc.parallelize(
      Array(
        Array(1.0, 2.0, 3.0),
        Array(2.0, 3.0, 4.0))).map(f =>Vectors.dense(f))
    //����1.2.3.4.5 ���������ƽ������3
    val mss  = Statistics.colStats(rdd1)
    //�����Ǹ���������ƽ����֮���ƽ������ٳ��Ը���
    //����ԽСԽ�ȶ�,��ʾ���ݼ���С
    println("��ֵ:" + mss.mean);
    println("��������:" + mss.variance); //���������Ǹ���������ƽ����֮���ƽ������ٳ���(����-1)
    println("����ͳ��������:" + mss.numNonzeros);
    println("����:" + mss.count);
    println("���ֵ:" + mss.max);
    println("��Сֵ:" + mss.min);
    //����normL2��ͳ����Ϣ
    /**
     * �������������أ�ͨ�����������������俪�컨�ı����Ƿ���ͬ��
     * 	����һ�� ���컨:1000��������:1856
     * 	���ض��� ���컨:400.��������:560
     */
    val land1 = Vectors.dense(1000.0, 1856.0)
    val land2 = Vectors.dense(400, 560)
    val c1 = Statistics.chiSqTest(land1, land2)

  }
}