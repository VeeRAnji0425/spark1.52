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
 * ����֧�����������ڴ��ģ��������ı�׼����
 */
object SVMWithSGDDemo {
  def main(args: Array[String]) {
    // ���β���Ҫ����־��ʾ�ն���
    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)
    // �������л���
    val conf = new SparkConf().setAppName("SVMWithSGDDemo").setMaster("local[4]")
    val sc = new SparkContext(conf)
/**
 *  libSVM�����ݸ�ʽ
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
 *  <index>����1��ʼ������,�����ǲ�����
 *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
 */
    // ����LIBSVM��ʽ����    
    // Load and parse the data file
    // Load training data in LIBSVM format.
    //����ѵ��libsvm��ʽ������
    val data = MLUtils.loadLibSVMFile(sc, "../data/mllib/sample_libsvm_data.txt")
    // Split data into training (60%) and test (40%).
    //�������з�ѵ������(60%)�Ͳ�������(40%)
    val splits = data.randomSplit(Array(0.6, 0.4), seed = 11L)
    val training = splits(0).cache()
    val test = splits(1)
    //����ѵ������ģ�͹���ģ��
    // Run training algorithm to build the model    
    val numIterations = 100
    //(SGD����ݶ��½�)
    val model = SVMWithSGD.train(training, numIterations)
    //���Ĭ����ֵ
    // Clear the default threshold.
    model.clearThreshold()
    //�ڲ��������ϼ���ԭʼ����
    // Compute raw scores on the test set.
    val scoreAndLabels = test.map { point =>
      val score = model.predict(point.features)
      println(">>" + score + "\t" + point.label)
      (score, point.label)
    }
    //�������ָ��
    // Get evaluation metrics.
    val metrics = new BinaryClassificationMetrics(scoreAndLabels)
    //ROC���������,��һ��������������ģ�ͺû���һ����׼
    val auROC = metrics.areaUnderROC()
   //ROC���������,��һ��������������ģ�ͺû���һ����׼
    println("Area under ROC = " + auROC)

    
    /**�߼��ع�***/
    //�߼��ع�,����lbfgs�Ż���ʧ����,֧�ֶ����,(BFGS������2��ţ�ٷ�)
    val modelBFGS = new LogisticRegressionWithLBFGS()
      .setNumClasses(10)
      .run(training)
    //�ڲ��������ϼ���ԭʼ����
    // Compute raw scores on the test set.
    val predictionAndLabels = test.map {
    //LabeledPoint��ǵ��Ǿֲ�����,�����������ܼ��ͻ���ϡ����,ÿ�������������һ����ǩ(label)
      case LabeledPoint(label, features) =>
        val prediction = model.predict(features)
        (prediction, label)
    }
    //��ȡ����ָ��
    // Get evaluation metrics.
    val metricsBFGS = new MulticlassMetrics(predictionAndLabels)
    val precision = metricsBFGS.precision
    println("Precision = " + precision)

  }

}