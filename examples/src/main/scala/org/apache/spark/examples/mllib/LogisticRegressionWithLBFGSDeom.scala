package org.apache.spark.examples.mllib
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.SVMWithSGD
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.util.MLUtils
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.spark.SparkConf
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS
/**
 * �߼��ع�
 */
object LogisticRegressionWithLBFGSDeom {
  def main(args: Array[String]) {
    // ���β���Ҫ����־��ʾ�ն���
    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)
    // �������л���
    val conf = new SparkConf().setAppName("LogisticRegressionWithLBFGS").setMaster("local[4]")
    val sc = new SparkContext(conf)
    // ����LIBSVM��ʽ����    
    // Load and parse the data file
    // Load training data in LIBSVM format.
    val data = MLUtils.loadLibSVMFile(sc, "../data/mllib/sample_libsvm_data.txt")
    // Split data into training (60%) and test (40%).
    //�������з�ѵ������(60%)�Ͳ�������(40%)
    val splits = data.randomSplit(Array(0.6, 0.4), seed = 11L)
    val training = splits(0).cache()
    val test = splits(1)
    /**�߼��ع�***/
    //����ѵ���㷨����ģ��
    val modelBFGS = new LogisticRegressionWithLBFGS()
      .setNumClasses(10)
      .run(training)
    //�ڲ��������ϼ���ԭʼ����
    // Compute raw scores on the test set.
    val predictionAndLabels = test.map {
      case LabeledPoint(label, features) =>
        val prediction = modelBFGS.predict(features)
        (prediction, label)
    }
    //��ȡ����ָ��
    // Get evaluation metrics.
    val metricsBFGS = new MulticlassMetrics(predictionAndLabels)
    val precision = metricsBFGS.precision
    println("Precision = " + precision)

  }
}