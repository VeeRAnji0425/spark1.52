package org.apache.spark.examples.mllib
import org.apache.spark.mllib.linalg.{ Vector, Vectors }
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.mllib.regression.LassoWithSGD
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
/**
 * Lasso(����)�㷨�����Իع���һ��������ѡ�񷽷�,������С��ͨ����ƽ����֮��
 * ��Ҫ����:����������Ϊû���õ�Ԥ������,��������ǵ����ϵ����Ϊ0�Ӷ������Ǵӷ���ʽ��ɾ��
 */
object LassoExample {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("CountVectorizerExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
    /**
     * �����Բ�̬��������Ϊ��ǵ�LabledPoint����
     */
    val points = Array(
    //LabeledPoint��ǵ��Ǿֲ�����,�����������ܼ��ͻ���ϡ����,ÿ�������������һ����ǩ(label)
      LabeledPoint(1, Vectors.dense(5, 3, 1, 2, 1, 3, 2, 2, 1)),
      LabeledPoint(2, Vectors.dense(9, 8, 8, 9, 7, 9, 8, 7, 9)))
    //����֮ǰ���ݵ�RDD
    val rdd = sc.parallelize(points)
    //ʹ�����ݵ���100��ѵ��ģ��,��ʱ���������򻯲����Ѿ��ֶ����ú���
    val model = LassoWithSGD.train(rdd, 100, 0.02, 2.0)
    /**
     * 9��Ԥ����������6����ϵ���������0,����LassoW��Ҫ����:����������Ϊû���õ�Ԥ������,
     * ��������ǵ����ϵ����Ϊ0�Ӷ������Ǵӷ���ʽ��ɾ��
     * [0.13455106581619633,0.0224732644670294,0.0,0.0,0.0,0.01360995990267153,0.0,0.0,0.0]
     */
    //����ж�Ԥ�����ӵ�ϵ������Ϊ0,
    model.weights
  }
}