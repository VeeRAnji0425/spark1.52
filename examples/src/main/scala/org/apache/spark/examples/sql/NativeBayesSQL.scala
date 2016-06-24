package org.apache.spark.examples.sql

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
 * ���磬ѵ������/tmp/lxw1234/1.txt��
 * 0,ƻ�� ���� ƻ�� ����
 * 1,ƻ�� �� �㽶
 */
case class RawDataRecord(category: String, text: String)

object NativeBayesSQL {
  def main(args: Array[String]) {
    val conf = new SparkConf().setMaster("NativeBayesSQL")
    val sc = new SparkContext(conf)
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    import sqlContext.implicits._
    //��ԭʼ����ӳ�䵽DataFrame�У��ֶ�categoryΪ�����ţ��ֶ�textΪ�ֺõĴʣ��Կո�ָ�
    var srcDF = sc.textFile("/tmp/lxw1234/1.txt").map {
      x =>
        var data = x.split(",")
        RawDataRecord(data(0), data(1))
    }.toDF()
    srcDF.select("category", "text").take(2).foreach(println)
    //[0,ƻ�� ���� ƻ�� ����]
    //[1,ƻ�� �� �㽶]
    //���ֺõĴ�ת��Ϊ����
    var tokenizer = new Tokenizer().setInputCol("text").setOutputCol("words")
    var wordsData = tokenizer.transform(srcDF)
    wordsData.select($"category", $"text", $"words").take(2).foreach(println)
    //[0,ƻ�� ���� ƻ�� ����,WrappedArray(ƻ��, ����, ƻ��, ����)]
    //[1,ƻ�� �� �㽶,WrappedArray(ƻ��, ��, �㽶)]

    //��ÿ����ת����Int�ͣ������������ĵ��еĴ�Ƶ��TF��
    var hashingTF =
     //setNumFeatures(100)��ʾ��Hash��Ͱ����������Ϊ100��,���ֵĬ��Ϊ2��20�η�����1048576�����Ը�����Ĵ���������������
     //һ����˵�����ֵԽ�󣬲�ͬ�Ĵʱ�����Ϊһ��Hashֵ�ĸ��ʾ�ԽС������Ҳ��׼ȷ������Ҫ���ĸ�����ڴ�
      new HashingTF().setInputCol("words").setOutputCol("rawFeatures").setNumFeatures(100)
    var featurizedData = hashingTF.transform(wordsData)
    featurizedData.select($"category", $"words", $"rawFeatures").take(2).foreach(println)
    //[0,WrappedArray(ƻ��, ����, ƻ��, ����),(100,[23,81,96],[2.0,1.0,1.0])]
    //[1,WrappedArray(ƻ��, ��, �㽶),(100,[23,72,92],[1.0,1.0,1.0])]
    //����У���ƻ������23����ʾ����һ���ĵ��У���ƵΪ2���ڶ����ĵ��д�ƵΪ1.

    //����TF-IDFֵ
    var idf = new IDF().setInputCol("rawFeatures").setOutputCol("features")
    var idfModel = idf.fit(featurizedData)
    var rescaledData = idfModel.transform(featurizedData)
    rescaledData.select($"category", $"words", $"features").take(2).foreach(println)

    //[0,WrappedArray(ƻ��, ����, ƻ��, ����),(100,[23,81,96],[0.0,0.4054651081081644,0.4054651081081644])]
    //[1,WrappedArray(ƻ��, ��, �㽶),(100,[23,72,92],[0.0,0.4054651081081644,0.4054651081081644])]
    //��Ϊһ��ֻ�������ĵ����Ҷ������ˡ�ƻ��������˸ôʵ�TF-IDFֵΪ0.

    //ÿһ��LabeledPoint�У���������ĳ���Ϊ100��setNumFeatures(100)����
    //���������͡���������Ӧ�����������ŷֱ�Ϊ81��96����ˣ������������У���81λ�͵�96λ�ֱ�Ϊ���ǵ�TF-IDFֵ��


    var trainDataRdd = rescaledData.select($"category", $"features").map {
      case Row(label: String, features: Vector) =>
        LabeledPoint(label.toDouble, Vectors.dense(features.toArray))
    }

  }
}