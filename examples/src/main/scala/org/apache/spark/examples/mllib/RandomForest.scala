package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.mllib.util.MLUtils
/**
 * ���ɭ���㷨�㷨ʹ�� demo
 */
object RandomForestDemo {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
    // ��������
    val data = MLUtils.loadLibSVMFile(sc, "data/mllib/sample_libsvm_data.txt")
    // �������������Ϊ���ݣ�һ������ѵ����һ�����ڲ���
    val splits = data.randomSplit(Array(0.7, 0.3))
    val (trainingData, testData) = (splits(0), splits(1))
    // ���ɭ��ѵ����������
    //������
    val numClasses = 2
    // categoricalFeaturesInfo Ϊ�գ���ζ�����е�����Ϊ�����ͱ���
    val categoricalFeaturesInfo = Map[Int, Int]()
    //���ĸ���
    val numTrees = 3
    //�����Ӽ��������ԣ�auto ��ʾ�㷨����ѡȡ
    val featureSubsetStrategy = "auto"
    //���ȼ���
    val impurity = "gini"
    //���������
    val maxDepth = 4
    //�������װ����
    val maxBins = 32
    //ѵ�����ɭ�ַ�������trainClassifier ���ص��� RandomForestModel ����
    val model = RandomForest.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
      numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins)
    // ������������ѵ���õķ����������������
    val labelAndPreds = testData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    val testErr = labelAndPreds.filter(r => r._1 != r._2).count.toDouble / testData.count()
    println("Test Error = " + testErr)
    println("Learned classification forest model:\n" + model.toDebugString)
    // ��ѵ��������ɭ��ģ�ͳ־û�
    model.save(sc, "myModelPath")
    //�������ɭ��ģ�͵��ڴ�
    val sameModel = RandomForestModel.load(sc, "myModelPath")

  }
}