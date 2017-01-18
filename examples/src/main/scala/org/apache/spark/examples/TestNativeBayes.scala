package org.apache.spark.examples

import scala.reflect.runtime.universe

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.ml.feature.HashingTF
import org.apache.spark.ml.feature.IDF
import org.apache.spark.ml.feature.Tokenizer
import org.apache.spark.mllib.classification.NaiveBayes
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.sql.Row

/**
 *ʹ��Spark MLlib�ṩ�����ر�Ҷ˹��Native Bayes���㷨����ɶ������ı��ķ�����̡�
 * ��Ҫ�������ķִʡ��ı���ʾ��TF-IDF����ģ��ѵ��������Ԥ���
 */
//http://www.open-open.com/lib/view/open1453539595620.html

case class RawDataRecord(category: String, text: String)
object TestNativeBayes {
  def main(args: Array[String]) {

    val conf = new SparkConf().setMaster("local[2]").setAppName("NativeBayes")
    val sc = new SparkContext(conf)

    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    //��ʽ����,�Զ�ת��toDF
    import sqlContext.implicits._
    var srcRDD = sc.textFile("../data/mllib/sougou/C000007/10.txt").filter(!_.isEmpty).map {
      x =>
        // println("==="+x+"===========")
        var data = x.split("��")
        println("==="+data(0)+"\t======"+data(1))
        RawDataRecord(data(0), data(1))
    }

    //70%��Ϊѵ�����ݣ�30%��Ϊ��������
    val splits = srcRDD.randomSplit(Array(0.7, 0.3))
    var trainingDF = splits(0).toDF()
    var testDF = splits(1).toDF()

    //������ת��������
    var tokenizer = new Tokenizer().setInputCol("text").setOutputCol("words")
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    var wordsData = tokenizer.transform(trainingDF)
    //output1����������ת�������飩
    println("output1��")   
    wordsData.select($"category", $"text", $"words").take(1)

    //����ÿ�������ĵ��еĴ�Ƶ
    var hashingTF = new HashingTF().setNumFeatures(500000).setInputCol("words").setOutputCol("rawFeatures")
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    var featurizedData = hashingTF.transform(wordsData)
    //output2��������ÿ�������ĵ��еĴ�Ƶ��
    println("output2��")
    featurizedData.select($"category", $"words", $"rawFeatures").take(1)
    //println(">>>>>>>>>>>>>>>."+featurizedData.toString())
    //����ÿ���ʵ�TF-IDF�㷨���ı��ִ��д�����������
    var idf = new IDF().setInputCol("rawFeatures").setOutputCol("features")
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    var idfModel = idf.fit(featurizedData)
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    var rescaledData = idfModel.transform(featurizedData)
    //output3��������ÿ���ʵ�TF-IDF��
    println("output3��")
    rescaledData.select($"category", $"features").take(1)

    //ת����Bayes�������ʽ
    var trainDataRdd = rescaledData.select($"category", $"features").map {
      case Row(label: String, features: Vector) =>
      //LabeledPoint��ǵ��Ǿֲ�����,�����������ܼ��ͻ���ϡ����,ÿ�������������һ����ǩ(label)
        LabeledPoint(label.toDouble, Vectors.dense(features.toArray))
    }
    //output4����Bayes�㷨���������ݸ�ʽ��
    println("output4��")
    trainDataRdd.take(1)

    
    var srcDF = sc.textFile("../data/mllib/1.txt").map { 
      x => 
        var data = x.split(",")
        RawDataRecord(data(0),data(1))
    }.toDF()

    //ѵ��ģ��,modelTypeģ������(���ִ�Сд)
    val model = NaiveBayes.train(trainDataRdd, lambda = 1.0, modelType = "multinomial")

    //�������ݼ�����ͬ����������ʾ����ʽת��
    var testwordsData = tokenizer.transform(testDF)
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    var testfeaturizedData = hashingTF.transform(testwordsData)
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    var testrescaledData = idfModel.transform(testfeaturizedData)
    var testDataRdd = testrescaledData.select($"category", $"features").map {
      case Row(label: String, features: Vector) =>
      //LabeledPoint��ǵ��Ǿֲ�����,�����������ܼ��ͻ���ϡ����,ÿ�������������һ����ǩ(label)
        LabeledPoint(label.toDouble, Vectors.dense(features.toArray))
    }

    //�Բ������ݼ�ʹ��ѵ��ģ�ͽ��з���Ԥ��
    val testpredictionAndLabel = testDataRdd.map(p => (model.predict(p.features), p.label))

    //ͳ�Ʒ���׼ȷ��
    var testaccuracy = 1.0 * testpredictionAndLabel.filter(x => x._1 == x._2).count() / testDataRdd.count()
    //output5�����������ݼ�����׼ȷ�ʣ�
    println("output5��")
   //׼ȷ��90%�������ԡ���������Ҫ�ռ������ϸ��ʱ����µ�������ѵ���Ͳ����ˡ� 
    println(testaccuracy)

  }
}