package org.apache.spark.examples.mllib

import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{ IndexToString, StringIndexer, Word2Vec }
import org.apache.spark.sql.SQLContext
import org.apache.spark.{ SparkContext, SparkConf }
/**
 * Spark ML ���ı�����
 * �ο�����
 * https://www.ibm.com/developerworks/cn/opensource/os-cn-spark-practice6/
 */
object SMSClassifierWord2Vec_IBM {
  final val VECTOR_SIZE = 100
  def main(args: Array[String]) {
    /*   if (args.length < 1) {
      println("Usage:SMSClassifier SMSTextFile")
      sys.exit(1)
    }*/
    //LogUtils.setDefaultLogLevel()
    val conf = new SparkConf().setMaster("local[2]").setAppName("SMS Message Classification (HAM or SPAM)")
    val sc = new SparkContext(conf)
    val sqlCtx = new SQLContext(sc)
    //��ȡԭʼ���ݼ���������һ�� DataFrame
    val parsedRDD = sc.textFile("../data/mllib/SMSSpamCollection").map(_.split("\t")).map(eachRow => {
      (eachRow(0), eachRow(1).split(" "))
    })
    val msgDF = sqlCtx.createDataFrame(parsedRDD).toDF("label", "message")
    //ʹ�� StringIndexer ��ԭʼ���ı���ǩ (��Ham�����ߡ�Spam��) ת������ֵ�͵ı��ͣ��Ա� Spark ML ����
    val labelIndexer = new StringIndexer().setInputCol("label").setOutputCol("indexedLabel").fit(msgDF)
    //ʹ�� Word2Vec �������ı�ת������ֵ�ʹ�����
    val word2Vec = new Word2Vec().setInputCol("message").setOutputCol("features").setVectorSize(VECTOR_SIZE).setMinCount(1)
    val layers = Array[Int](VECTOR_SIZE, 6, 5, 2)
    //ʹ�� MultilayerPerceptronClassifier ѵ��һ������֪��ģ��
    val mlpc = new MultilayerPerceptronClassifier().setLayers(layers).setBlockSize(512)
    .setSeed(1234L).setMaxIter(128).setFeaturesCol("features")
    .setLabelCol("indexedLabel").setPredictionCol("prediction")
    //ʹ�� LabelConverter ��Ԥ��������ֵ��ǩת����ԭʼ���ı���ǩ
    val labelConverter = new IndexToString().setInputCol("prediction").setOutputCol("predictedLabel").setLabels(labelIndexer.labels)
    //��ԭʼ�ı����ݰ��� 8:2 �ı����ֳ�ѵ���Ͳ������ݼ�
    val Array(trainingData, testData) = msgDF.randomSplit(Array(0.8, 0.2))

    val pipeline = new Pipeline().setStages(Array(labelIndexer, word2Vec, mlpc, labelConverter))
    val model = pipeline.fit(trainingData)

    val predictionResultDF = model.transform(testData)
    //below 2 lines are for debug use
    predictionResultDF.printSchema
    predictionResultDF.select("message", "label", "predictedLabel").show(30)

    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol("indexedLabel")
      .setPredictionCol("prediction")
      .setMetricName("precision")
    //����ڲ������ݼ��ϲ���ģ�͵�Ԥ�⾫ȷ��
    val predictionAccuracy = evaluator.evaluate(predictionResultDF)
    println("Testing Accuracy is %2.4f".format(predictionAccuracy * 100) + "%")
    sc.stop
  }
}