package org.apache.spark.examples.mllib
import org.apache.spark.mllib.linalg.{ Vector, Vectors }
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
/**
 *2006����ձ���ͬɳ̲��֩��ķֲ���һЩ�о�
 * ����֩������ȴ�С���ִ�״̬������
 * 0��ʾ��ʧ,1��ʾ����
 */
object LogisticRegressionWithLBFGSExample {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("CountVectorizerExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
    /**
     * ��֩��Ĵ��ڻ���ʧ��Ϊ��Ǵ���һ��LabledPoint����
     */
    val points = Array(
      LabeledPoint(0.0,Vectors.dense(0.245)),
      LabeledPoint(0.0,Vectors.dense(0.247)),
      LabeledPoint(1.0,Vectors.dense(0.285)),
      LabeledPoint(1.0,Vectors.dense(0.299)),
      LabeledPoint(1.0,Vectors.dense(0.327)),
      LabeledPoint(1.0,Vectors.dense(0.347)),
      LabeledPoint(0.0,Vectors.dense(0.356)),
      LabeledPoint(1.0,Vectors.dense(0.36)),
      LabeledPoint(0.0,Vectors.dense(0.363)),
      LabeledPoint(1.0,Vectors.dense(0.364)),
      LabeledPoint(0.0,Vectors.dense(0.398)),
      LabeledPoint(1.0,Vectors.dense(0.4)),
      LabeledPoint(0.0,Vectors.dense(0.409)),
      LabeledPoint(1.0,Vectors.dense(0.421)),
      LabeledPoint(0.0,Vectors.dense(0.432)),
      LabeledPoint(1.0,Vectors.dense(0.473)),
      LabeledPoint(1.0,Vectors.dense(0.509)),
      LabeledPoint(1.0,Vectors.dense(0.529)),
      LabeledPoint(0.0,Vectors.dense(0.561)),
      LabeledPoint(0.0,Vectors.dense(0.569)),
      LabeledPoint(1.0,Vectors.dense(0.594)),
      LabeledPoint(1.0,Vectors.dense(0.638)),
      LabeledPoint(1.0,Vectors.dense(0.656)),
      LabeledPoint(1.0,Vectors.dense(0.816)),
      LabeledPoint(1.0,Vectors.dense(0.853)),
      LabeledPoint(1.0,Vectors.dense(0.938)),
      LabeledPoint(1.0,Vectors.dense(1.036)),
      LabeledPoint(1.0,Vectors.dense(1.045)))
    //����֮ǰ���ݵ�RDD
    val spiderRDD = sc.parallelize(points)
    //ʹ������ѵ��ģ��(������Ԥ��ֵΪ0��ʱ��,�������������)
    val lr = new LogisticRegressionWithLBFGS().setIntercept(true)
    val model = lr.run(spiderRDD)
    //Ԥ��0.938�߶ȵ�֩�����״
    val predict = model.predict(Vectors.dense(0.938))
 
  }
}