package org.apache.spark.examples.mllib

import org.apache.spark.mllib.stat.MultivariateStatisticalSummary
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Matrix
/**
 * ����ͳ�ƹ���
 */
object BasicStatistics {

  def main(args: Array[String]) {
    val observations: RDD[Vector] = null // an RDD of Vectors һ��������RDD
    // Compute column summary statistics. �����л���ͳ��
    //����һ��MultivariateStatisticalSummaryʵ��������������е����ֵ
    //��Сֵ,��ֵ,����,����ֵ����������
    val summary: MultivariateStatisticalSummary = Statistics.colStats(observations)
    //һ������ÿ���е�ƽ��ֵ�ĳ�������
    println(summary.mean) // a dense vector containing the mean value for each column
    println(summary.variance) // column-wise variance �еķ���
    println(summary.numNonzeros) // number of nonzeros in each column ��ÿһ�еľ���ķ���Ԫ����

    /***�ṩ�м������**/
    val sc: SparkContext = null
    val seriesX: RDD[Double] = null // a series һϵ��
    //��������ͬ�����ķ����ͻ���������
    val seriesY: RDD[Double] = null // must have the same number of partitions and cardinality as seriesX

    // compute the correlation using Pearson's method. Enter "spearman" for Spearman's method. If a 
    // method is not specified, Pearson's method will be used by default. 
    //pearsonƤ��ɭ�����
    val correlation: Double = Statistics.corr(seriesX, seriesY, "pearson")
    println("pearson:"+correlation)
    //��ע��,ÿ��������һ����,������һ����
    val data: RDD[Vector] = null // note that each Vector is a row and not a column 
     //spearman ˹Ƥ���������
    // calculate the correlation matrix using Pearson's method. Use "spearman" for Spearman's method.
    //��Ƥ��ɭ��������ؾ���,�á�˹Ƥ��������˹Ƥ��������
    // If a method is not specified, Pearson's method will be used by default. 
    //���û��ָ������,Ƥ��ɭ�ķ�������Ĭ��ʹ��
    val correlMatrix: Matrix = Statistics.corr(data, "pearson")
    println("correlMatrix:"+correlMatrix.toString())


  }
}