package org.apache.spark.examples.mllib
import org.apache.spark.mllib.linalg.{ Vector, Vectors }
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.mllib.regression.RidgeRegressionWithSGD
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
/**
 * Lasso(����)�㷨�����Իع���һ��������ѡ�񷽷�,������С��ͨ����ƽ����֮��
 * ��Ҫ����:����������Ϊû���õ�Ԥ������,��������ǵ����ϵ����Ϊ0�Ӷ������Ǵӷ���ʽ��ɾ��
 */
object RidgeRegressionWithSGDExample {
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
    val model = RidgeRegressionWithSGD.train(rdd, 100, 0.02, 2.0)
    /**
     * ��ع鲻���Ԥ������ϵ����Ϊ0,�����������ǽ�����0
     * [0.049805969577244584,0.029883581746346748,0.009961193915448916,0.019922387830897833,
     *  0.009961193915448916,0.029883581746346748,0.019922387830897833,0.019922387830897833,
     *  0.009961193915448916]
     */
    //����ж�Ԥ�����ӵ�ϵ������Ϊ0,
    model.weights
  }
}