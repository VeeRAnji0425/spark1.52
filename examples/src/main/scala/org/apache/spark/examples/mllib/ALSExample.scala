package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating

object ALSExample {
  def main(args: Array[String]) {
    /**
     * Эͬ����ALS�㷨�Ƽ��������£�
     * �������ݵ� ratings RDD,ÿ�м�¼������user, product, rate
     * �� ratings �õ��û���Ʒ�����ݼ���(user, product)
     * ʹ��ALS�� ratings ����ѵ��
     * ͨ�� model ���û���Ʒ����Ԥ�����֣�((user, product), rate)
     * �� ratings �õ��û���Ʒ��ʵ�����֣�((user, product), rate)
     * �ϲ�Ԥ�����ֺ�ʵ�����ֵ��������ݼ������������
     */
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
    /**�ļ���ÿһ�а���һ���û�id����Ʒid������****/
    val data = sc.textFile("../data/mllib/als/test.data")
    //ƥ������
    val ratings = data.map(_.split(',') match {
      case Array(user, product, rate) =>
        //�û�ID,��ƷID,(����,�ȼ�)
        Rating(user.toInt, product.toInt, rate.toDouble)
    })
    //ʹ��ALSѵ�����ݽ����Ƽ�ģ��
    val rank = 10 //ģ�������������ӵĸ���(������ģ��),ALS�����ӵĸ�����ͨ����˵Խ��Խ�ã����Ƕ��ڴ�ռ������ֱ��Ӱ�죬ͨ��rank��10��200֮��
    val numIterations = 20 //������
    val model = ALS.train(ratings, rank, numIterations, 0.01)
    //�� ratings �л��ֻ�����û�����Ʒ�����ݼ� 
    val usersProducts = ratings.map {
      case Rating(user, product, rate) =>
        (user, product)
    }
    //ʹ���Ƽ�ģ�Ͷ��û���Ʒ����Ԥ�����֣��õ�Ԥ�����ֵ����ݼ�
    val predictions =
      model.predict(usersProducts).map {
        case Rating(user, product, rate) =>
          ((user, product), rate)
      }
    //����ʵ�������ݼ���Ԥ���������ݼ����кϲ�
    val ratesAndPreds = ratings.map {
      case Rating(user, product, rate) =>
        ((user, product), rate)
    }.join(predictions).sortByKey() //ascending or descending 
    //Ȼ���������ע������û�е��� math.sqrt����
    val MSE = ratesAndPreds.map {
      case ((user, product), (r1, r2)) =>
        val err = (r1 - r2)
        err * err
    }.mean()
    //��ӡ��������ֵ  
    println("Mean Squared Error = " + MSE)
    //Mean Squared Error = 1.37797097094789E-5

    /***�û��Ƽ���Ʒ**/

    //Ϊÿ���û������Ƽ����Ƽ��Ľ���������û�idΪkey�����Ϊvalue����redis����hbase��
    val users = data.map(_.split(",") match {
      case Array(user, product, rate) => (user)
    }).distinct().collect()
    //users: Array[String] = Array(4, 2, 3, 1)
    users.foreach(
      user => {
        //����Ϊ�û��Ƽ���Ʒ   
        var rs = model.recommendProducts(user.toInt, numIterations)
        var value = ""
        var key = 0

        //ƴ���Ƽ����
        rs.foreach(r => {
          key = r.user
          value = value + r.product + ":" + r.rating + ","
        })
        println(key.toString + "   " + value)
      })

    //��Ԥ������Ԥ�����������
    predictions.collect.sortBy(_._2)
    //��Ԥ�������û����з��飬Ȼ��ϲ��Ƽ�������ⲿ�ִ��������
    predictions.map { case ((user, product), rate) => (user, (product, rate)) }.groupByKey.collect
    //��ʽ���������ֺ�ʵ�����ֵĽ��
    val formatedRatesAndPreds = ratesAndPreds.map {
      case ((user, product), (rate, pred)) => user + "," + product + "," + rate + "," + pred
    }
    formatedRatesAndPreds.collect()
  }
}