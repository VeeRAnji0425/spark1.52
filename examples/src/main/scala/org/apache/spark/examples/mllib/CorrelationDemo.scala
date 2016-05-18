package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Matrix
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.rdd.RDD
/**
 * Correlation ����Է���
 * PearsonCorrelation��Spark����˽�г�Ա������ֱ�ӷ��ʣ�ʹ��ʱ��Ȼ��ͨ��Statistics�������
 */
object CorrelationDemo {
  def main(args: Array[String]) {
    //val sparkConf = new SparkConf().setMast("local[2]").setAppName("SparkHdfsLR")

    val conf = new SparkConf().setAppName("test").setMaster("local")
    val sc = new SparkContext(conf)
    val rdd1: RDD[Double] = sc.parallelize(Array(11.0, 21.0, 13.0, 14.0))
    val rdd2: RDD[Double] = sc.parallelize(Array(11.0, 20.0, 13.0, 16.0))
    //����rdd��������
    //����ֵ��correlation: Double = 0.959034501397483
    //[-1, 1]��ֵԽ�ӽ���1������ض�Խ��
    val correlation: Double = Statistics.corr(rdd1, rdd2, "pearson")

    val rdd3 = sc.parallelize(
      Array(
        Array(1.0, 2.0, 3.0, 4.0),
        Array(2.0, 3.0, 4.0, 5.0),
        Array(3.0, 4.0, 5.0, 6.0))).map(f => Vectors.dense(f))
    //correlation3: org.apache.spark.mllib.linalg.Matrix = 
    //1.0  1.0  1.0  1.0  
    //1.0  1.0  1.0  1.0  
    //1.0  1.0  1.0  1.0  
    //1.0  1.0  1.0  1.0  
    val correlation3: Matrix = Statistics.corr(rdd3, "pearson")
    /**
     * ����ĳ����ͨ����������õ����Գɼ������֮��Ĺ�ϵ�������£�
     * ����дͼƬ����
     * ֱ�۵ؿ����ɼ�Խ�߲���Խ�ߣ����ʹ��pearson���ϵ�������õ����½����
     */
    val rdd4: RDD[Double] = sc.parallelize(Array(50.0, 60.0, 70.0, 80.0, 90.0, 95.0))
    val rdd5: RDD[Double] = sc.parallelize(Array(500.0, 510.0, 530.0, 580.0, 560, 1000))
    //ִ�н��Ϊ:
    //correlation4: Double = 0.6915716600436548
    //����ʵ�����ǹ۲����������������Ӧ���Ǹ߶���صģ���Ȼ0.69Ҳһ���̶ȵط�Ӧ�����ݼ�������
    val correlation4: Double = Statistics.corr(rdd4, rdd5, "pearson")
    /**
     * *
     * ����еĵ��ġ����������ݣ�ͨ�����ɼ��Ͳ����滻�ɵȼ���������֮�����ضȻ�������ߣ������Ļ����������ǿ�������д�����ʾ��
     */
    //����spearman���ϵ��
    //ִ�н����
    //correlation5: Double = 0.9428571428571412
    val correlation5: Double = Statistics.corr(rdd4, rdd5, "spearman")
    //�������ִ�н������������Դ�pearson��ֵ0.6915716600436548��ߵ���0.9428571428571412���������õĵȼ���أ�
    //���spearman����Է���Ҳ��Ϊspearman�ȼ���ط�����ȼ�������������Ҫע�����spearman����Է��������漰���ȼ����������⣬
    //�ڷֲ�ʽ�����µ�������ܻ��漰������������IO�������㷨Ч�ʲ����ر�ߡ�
  }
}