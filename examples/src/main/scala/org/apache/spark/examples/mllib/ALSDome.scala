package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating
/**
 * Spark coolbook p163
 */
object ALSDome {
  def main(args: Array[String]) {
    /**
     * Эͬ����ALS�㷨�Ƽ��������£�
     * �������ݵ� ratings RDD,ÿ�м�¼������user, product, rate
     * �� ratings �õ��û���Ʒ�����ݼ���(user, product)
     * ʹ��ALS�� ratings ����ѵ��
     * ͨ�� model ���û���Ʒ����Ԥ�����֣�((user, product), rate)
     * �� ratings �õ��û���Ʒ��ʵ�����֣�((user, product), rate)
     * �ϲ�Ԥ�����ֺ�ʵ�����ֵ��������ݼ�,���������
     */
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("ALSExample")
    val sc = new SparkContext(sparkConf)
    /**�ļ���ÿһ�а���һ���û�id����Ʒid������****/
    val data = sc.textFile("../data/mllib/u.data")
    //��val����ת��(transform)������(Rating)RDD
    val ratings = data.map { line =>
        val Array(userId, itemId, rating, _) = line.split("\t")
        //�û�ID,��ƷID,(����,�ȼ�)
        Rating(userId.toInt, itemId.toInt, rating.toDouble)
    }
    
    //�������������ݵ���RDD
    /**
     * �û�ID,��ӰID,����
     * 944,313,5
       944,2,3
       944,1,1
       944,43,4
       944,67,4
       944,82,5
       944,96,5
       944,121,4
       944,148,4
     */
    val pdata = sc.textFile("../data/mllib/p.data")
    val pratings = pdata.map { line =>
        val Array(userId, itemId, rating) = line.split(",")
        println(userId.toInt+"||"+itemId.toInt+"||"+rating.toDouble)
         /**�ļ���ÿһ�а���һ���û�id����Ʒid������****/
        Rating(userId.toInt, itemId.toInt, rating.toDouble)
       }
   //���������ݺ͸�����������
    val movieratings = ratings.union(pratings)
    //ʹ��ALS����ģ��,�趨rankΪ5,��������Ϊ10�Լ�lambdaΪ0.01
    val model = ALS.train(movieratings, 10, 10, 0.01)
     //��ģ����ѡ��һ����ӰԤ���ҵ�����,�����Ǵӵ�ӰIDΪ195��<�ս���>��ʼ
    model.predict(sc.parallelize(Array((944,195)))).collect.foreach(println)
    //��ģ����ѡ��һ����ӰԤ���ҵ�����,�����Ǵӵ�ӰIDΪ402<�˹���δ��>
    model.predict(sc.parallelize(Array((944,402)))).collect.foreach(println)
    //��ģ����ѡ��һ����ӰԤ���ҵ�����,�����Ǵӵ�ӰIDΪ148<��ҹ����>
    model.predict(sc.parallelize(Array((944,402)))).collect.foreach(println)  
  }
}