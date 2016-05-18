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
    val observations: RDD[Vector] = null // an RDD of Vectors
    // Compute column summary statistics.
    //����һ��MultivariateStatisticalSummaryʵ��������������е����ֵ
    //��Сֵ,��ֵ,����,����ֵ����������
    val summary: MultivariateStatisticalSummary = Statistics.colStats(observations)
    println(summary.mean) // a dense vector containing the mean value for each column
    println(summary.variance) // column-wise variance
    println(summary.numNonzeros) // number of nonzeros in each column

    /***�ṩ�м������**/
    val sc: SparkContext = null
    val seriesX: RDD[Double] = null // a series
    val seriesY: RDD[Double] = null // must have the same number of partitions and cardinality as seriesX

    // compute the correlation using Pearson's method. Enter "spearman" for Spearman's method. If a 
    // method is not specified, Pearson's method will be used by default. 
    //pearsonƤ��ɭ�����
    val correlation: Double = Statistics.corr(seriesX, seriesY, "pearson")

    val data: RDD[Vector] = null // note that each Vector is a row and not a column
     //spearman ˹Ƥ���������
    // calculate the correlation matrix using Pearson's method. Use "spearman" for Spearman's method.
    // If a method is not specified, Pearson's method will be used by default. 
    val correlMatrix: Matrix = Statistics.corr(data, "pearson")


  }
}