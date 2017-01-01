package org.apache.spark.examples.mllib
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.SVMWithSGD
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.SparkConf
/**
 * Spark cookbook p127
 */
object SVMWithSGDExample {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("SVMWithSGDExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
    //�����ݼ��س�RDD
    val svmData = MLUtils.loadLibSVMFile(sc, "../data/mllib/sample_libsvm_data.txt")
    //�����¼����Ŀ
    svmData.count
    //�����ݼ��ֳ�����,һ��ѵ�����ݺ�һ���������
    val trainingAndTest = svmData.randomSplit(Array(0.5, 0.5))
    //ѵ�����ݺͲ������ݸ�ֵ
    val trainingData = trainingAndTest(0)
    val testData = trainingAndTest(1)
    //ѵ���㷨��������100�ε�������ģ��
    val model = SVMWithSGD.train(trainingData, 100)
    //��ģ��ȥΪ�������ݼ�Ԥ���ǩ,ʹ�ò��������еĵ�һ������Ա�ǩ
    val label = model.predict(testData.first.features)
    //����һ��Ԫ��,���е�һ��Ԫ���ǲ������ݵ�Ԥ���ǩ,�ڶ���Ԫ����ʵ�ʱ�ǩ
    val predictionsAndLabels = testData.map(r => (model.predict(r.features), r.label))
    //�����ж���Ԥ���ǩ��ʵ�ʱ�ǩ��ƥ��ļ�¼
    predictionsAndLabels.filter(p => p._1 != p._2).count
  }
}