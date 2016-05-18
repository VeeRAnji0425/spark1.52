package org.apache.spark.examples.mllib

import org.apache.spark.mllib.linalg.distributed.IndexedRowMatrix
import org.apache.spark.SparkConf
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Matrix
import org.apache.spark.mllib.linalg.distributed.IndexedRow
import org.apache.spark.mllib.linalg.Vectors

object IndexRowMatrixDemo {
  def main(args: Array[String]) {
    //val sparkConf = new SparkConf().setMast("local[2]").setAppName("SparkHdfsLR")

    val conf = new SparkConf().setAppName("test").setMaster("local")
    val sc = new SparkContext(conf)
    //����һ����ʽת������
    implicit def double2long(x: Double) = x.toLong
    //�����еĵ�һ��Ԫ��ΪIndexedRow�е�index��ʣ���ӳ�䵽vector
    //f.take(1)(0)��ȡ����һ��Ԫ�ز��Զ�������ʽת����ת����Long����
    val rdd1 = sc.parallelize(
      Array(
        Array(1.0, 2.0, 3.0, 4.0),
        Array(2.0, 3.0, 4.0, 5.0),
        Array(3.0, 4.0, 5.0, 6.0))).map(f => IndexedRow(f.take(1)(0), Vectors.dense(f.drop(1))))
    val indexRowMatrix = new IndexedRowMatrix(rdd1)
    //������ķ����
    var gramianMatrix: Matrix = indexRowMatrix.computeGramianMatrix()
    //ת�����о���RowMatrix
    var rowMatrix: RowMatrix = indexRowMatrix.toRowMatrix()
    //������������computeSVD��������ֵ��multiply������˵Ȳ���������ʹ����RowMaxtrix��ͬ
  }
}