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
 * ����,ѵ������/tmp/lxw1234/1.txt��
 * 0,ƻ�� ���� ƻ�� ����
 * 1,ƻ�� �� �㽶
 * ����http://lxw1234.com/archives/2016/01/605.htm
 */
case class RawDataRecord(category: String, text: String)

object HashingTFIDFSQL {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("HashingTFIDFSQL").setMaster("local")
    val sc = new SparkContext(conf)
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    import sqlContext.implicits._    
    /**
      0,ƻ�� ���� ƻ�� ����
      1,ƻ�� �� �㽶
     */
    var srcDF = sc.textFile("..//data/mllib/1.txt").map {
      x =>
        //��ԭʼ����ӳ�䵽DataFrame��,�ֶ�categoryΪ������,�ֶ�textΪ�ֺõĴ�,�Կո�ָ�
        var data = x.split(",")
        //ȡ�������ֶ�
        RawDataRecord(data(0), data(1))
    }.toDF()
    /**
    +--------+-----------------+
    |category|       text      |
    +--------+-----------------+
    |       0|ƻ�� ���� ƻ�� ����  |
    |       1|    ƻ�� �� �㽶       |
    +--------+-----------------+*/
    srcDF.select("category", "text").show()
    //���ֺõĴ�ת�Կո�ָ���Ϊ����
    var tokenizer = new Tokenizer().setInputCol("text").setOutputCol("words")
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    var wordsData = tokenizer.transform(srcDF)
    /**
      [0,ƻ�� ���� ƻ�� ����,WrappedArray(ƻ��, ����, ƻ��, ����)]
      [1,ƻ�� �� �㽶,WrappedArray(ƻ��, ��, �㽶)]
      +--------+-----------------+-----------------------+
      |category|       text      |           words       |
      +--------+-----------------+-----------------------+
      |       0|ƻ�� ���� ƻ�� ����  |[ƻ��, ����, ƻ��, ����]|
      |       1|    ƻ�� �� �㽶       |     [ƻ��, ��, �㽶]   |
      +--------+-----------------+-----------------------+
     */
    wordsData.select($"category", $"text", $"words").show()
    

    //��ÿ����ת����Int��,�����������ĵ��еĴ�Ƶ(TF)
    var hashingTF =
     //setNumFeatures(100)��ʾ��Hash��Ͱ����������Ϊ100��,���ֵĬ��Ϊ2��20�η�,��1048576,���Ը��ݴ�������������,
     //һ����˵,���ֵԽ��,��ͬ�Ĵʱ�����Ϊһ��Hashֵ�ĸ��ʾ�ԽС,����Ҳ��׼ȷ,����Ҫ���ĸ�����ڴ�
      new HashingTF().setInputCol("words").setOutputCol("rawFeatures").setNumFeatures(100)
    var featurizedData = hashingTF.transform(wordsData)
    /**
     * [0,WrappedArray(ƻ��, ����, ƻ��, ����),(100,[23,81,96],[2.0,1.0,1.0])]
     * [1,WrappedArray(ƻ��, ��, �㽶),(100,[23,72,92],[1.0,1.0,1.0])]
     * �����,"ƻ��"��23����ʾ,��һ��words��,��ƵΪ2,�ڶ���words�д�ƵΪ1.
     *+--------+-----------------------+--------------------+
      |category|           words       |         rawFeatures|
      +--------+-----------------------+--------------------+
      |       0|[ƻ��, ����, ƻ��, ����]|(100,[23,81,96],[...|
      |       1|     [ƻ��, ��, �㽶]   |(100,[23,72,92],[...|
      +--------+-----------------------+--------------------+
     */
    featurizedData.select($"category", $"words", $"rawFeatures").show()  
    //���ĵ�Ƶ��(IDF),��������һ�������ض��ĵ�����ض�,����TF-IDFֵ
    var idf = new IDF().setInputCol("rawFeatures").setOutputCol("features")
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    var idfModel = idf.fit(featurizedData)
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    var rescaledData = idfModel.transform(featurizedData)
    /**
     * [0,WrappedArray(ƻ��,����,ƻ��,����),(100,[23,81,96],[0.0,0.4054651081081644,0.4054651081081644])]
     * [1,WrappedArray(ƻ��,��,�㽶),(100,[23,72,92],[0.0,0.4054651081081644,0.4054651081081644])]
     * ��Ϊһ��ֻ�������ĵ�,�Ҷ�������"ƻ��",��˸ôʵ�TF-IDFֵ��ض�Ϊ0.
     * ���������͡���������Ӧ�����������ŷֱ�Ϊ81��96,������������,��81λ�͵�96λ�ֱ�Ϊ���ǵ�TF-IDFֵ��ض�
      +--------+------------------------+--------------------+
      |category|           words        |            features|
      +--------+------------------------+--------------------+
      |       0|[ƻ��, ����, ƻ��, ����] |(100,[23,81,96],[...|
      |       1|     [ƻ��, ��, �㽶]    |(100,[23,72,92],[.. |
      +--------+------------------------+---------------------+
     */
    rescaledData.select($"category", $"words", $"features").show()    
    /**
     * �����������ת����Bayes�㷨��Ҫ�ĸ�ʽ:
     		0,1 0 0
        0,2 0 0
        1,0 1 0
        1,0 2 0
        2,0 0 1
        2,0 0 2
     */
    var trainDataRdd = rescaledData.select($"category", $"features").map {
      case Row(label: String, features: Vector) =>
      //LabeledPoint��ǵ��Ǿֲ�����,�����������ܼ��ͻ���ϡ����,ÿ�������������һ����ǩ(label)
        LabeledPoint(label.toDouble, Vectors.dense(features.toArray))
    }

  }
}