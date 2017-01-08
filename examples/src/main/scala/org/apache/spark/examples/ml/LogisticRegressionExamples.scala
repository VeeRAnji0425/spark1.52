package org.apache.spark.examples.ml

import org.apache.spark.mllib.linalg.{ Vector, Vectors }
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.examples.mllib.AbstractParams
import org.apache.spark.ml.{ Pipeline, PipelineStage }
import org.apache.spark.ml.classification.{ LogisticRegression, LogisticRegressionModel }
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.sql.DataFrame
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.ml.classification.LogisticRegression

import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.apache.spark.util.Utils

   case class Feature(v: Vector)
  object LogisticRegressionExamples {
/**
 * Spark Coolbook ʹ��ML��������ѧϰ��ˮ��
 * ML��������:��ʹ��ת������һ��DataFrameת��Ϊ��һ��DataFrame,
 * һ��ת�����ļ����Ӿ�������һ��,����Եȼ��ڹ�ϵ���ݿ��"alter table"
 * ����ʹ��ѵ��������ѵ���㷨,Ȼ��ת��������Ԥ��
 */
  def main(args: Array[String]) {
    
    val conf = new SparkConf().setAppName("CountVectorizerExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    
    import sqlContext.implicits._
    //����һ������Lebron�������˶�Ա��������ǩ 80��,250��
    val lebron = LabeledPoint(1.0, Vectors.dense(80.0, 250.0))
    //����һ������tim�������˶�Ա��������ǩ 70��,150��
    val tim = LabeledPoint(0.0, Vectors.dense(70.0, 150.0))
    //����һ������brittan�������˶�Ա��������ǩ 80��,207��
    val brittany = LabeledPoint(1.0, Vectors.dense(80.0, 207.0))
    //����һ������Stacey�������˶�Ա��������ǩ 65��,120��
    val stacey = LabeledPoint(0.0, Vectors.dense(65.0, 120.0))    
    //����һ��ѵ����RDD    
    val trainingRDD = sc.parallelize(List(lebron, tim, brittany, stacey))
    //����һ��ѵ����DataFrame
   val  trainingDF=sqlContext.createDataFrame(trainingRDD)
    //val trainingDF = sqlContext.toDF()
    /** 
     * ����(estimator):�����Ż���ѧϰ�㷨,���ӻ�����ѧϰ,���������һ��DataFrame,�����һ��ת����
     * ÿ�����㶼��һ��fit()����,����ѵ���㷨
     * ����һ��LogisticRegression����
     */
    val estimator = new LogisticRegression
    //����һ��ѵ����DataFrameת����
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val transformer = estimator.fit(trainingDF)
    //������������john���90,����270.0�������˶�Ա
    val john = Vectors.dense(90.0, 270.0)
    //������������tom���62,����120.0���������˶�Ա
    val tom = Vectors.dense(62.0, 120.0)
    //����ѵ����RDD
    val testRDD =  sc.parallelize(List(john, tom))
    //ӳ��testRDD��Feature RDD
    val featuresRDD = testRDD.map(v => Feature(v))
    //��featuresRDDת��Ϊ����features��DataFrame
    val featuresDF = featuresRDD.toDF("features")
    //��ת��������Ԥ����features,ת��������Ԥ��
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val predictionsDF = transformer.transform(featuresDF)
    println("======")
    predictionsDF.foreach { x =>println _}
    //��predictionsDF������3��,��Ԥ��,�����Ժ�Ԥ��,����ֻѡ�������Ժ�Ԥ����
    val shorterPredictionsDF = predictionsDF.select("features", "prediction")
    //��Ԥ����(prediction)����ΪisBasketBallPlayer
    val playerDF = shorterPredictionsDF.toDF("features", "isBasketBallPlayer")
    playerDF.foreach { x =>println _ }
    /**
     *[[62.0,120.0],[31.460769106353915,-31.460769106353915],[0.9999999999999782,2.1715087350282438E-14],0.0]
		 *[[90.0,270.0],[-61.88475862618768,61.88475862618768],[1.3298137364599064E-27,1.0],1.0]
     */
    playerDF.collect().foreach { x =>println _}
    //��ӡplayerDF���ݽṹ
    playerDF.printSchema
    
  }
}