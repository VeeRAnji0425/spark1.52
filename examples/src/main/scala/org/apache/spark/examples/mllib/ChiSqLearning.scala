package org.apache.spark.examples.mllib
import org.apache.spark.mllib.linalg.{ Matrix, Matrices, Vectors }
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.{
  SparkConf,
  SparkContext

}
/**
 * �����������ͳ��������ʵ�ʹ۲�ֵ�������ƶ�ֵ֮���ƫ��̶ȣ�ʵ�ʹ۲�ֵ�������ƶ�ֵ֮���ƫ��̶Ⱦ;�������ֵ�Ĵ�С��
 * ����ֵԽ��Խ�����ϣ�����ֵԽС��ƫ��ԽС��Խ���ڷ��ϣ�
 * ������ֵ��ȫ���ʱ������ֵ��Ϊ0����������ֵ��ȫ����
 * ���ɶ�v=������-1��������-1��=1
 */
object ChiSqLearning {
  def main(args: Array[String]) {
    val vd = Vectors.dense(1, 2, 3, 4, 5)
    val vdResult = Statistics.chiSqTest(vd)
    println(vd)
    println(vdResult)
    println("-------------------------------")
    val mtx = Matrices.dense(3, 2, Array(1, 3, 5, 2, 4, 6))
    val mtxResult = Statistics.chiSqTest(mtx)
    println(mtx)
    println(mtxResult)
    
    //print :���������ɶȡ�������ͳ������pֵ,���۷���ĸ���p
    println("-------------------------------")
    val mtx2 = Matrices.dense(2, 2, Array(19.0, 34, 24, 10.0))
    printChiSqTest(mtx2)
    printChiSqTest(Matrices.dense(2, 2, Array(26.0, 36, 7, 2.0)))
    //    val mtxResult2 = Statistics.chiSqTest(mtx2)
    //    println(mtx2)
    //    println(mtxResult2)
  }

  def printChiSqTest(matrix: Matrix): Unit = {
    println("-------------------------------")
    val mtxResult2 = Statistics.chiSqTest(matrix)
    println(matrix)
    println(mtxResult2)
  }

}