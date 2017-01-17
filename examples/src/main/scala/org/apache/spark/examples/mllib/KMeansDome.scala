package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.linalg.Vectors
/**
 * �����㷨
 * http://blog.selfup.cn/728.html,
 * ��һ��Ŀ��object����Ϊ���ɸ��أ�ÿ����֮���object�����ܵ����ƣ������֮���object�����ܵ�����
 */
object KMeansDome {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("KMeansClustering")
    val sc = new SparkContext(sparkConf)
    val rawTrainingData = sc.textFile("../data/mllib/kmeans_data2.txt")
    val parsedData =
      rawTrainingData.map(line => {
        Vectors.dense(line.split(" ").map(_.trim).filter(!"".equals(_)).map(_.toDouble))
      }).cache()

    val pointsData = Seq(
      /***======�����ĵ�����ӳ�3============**/
      Vectors.dense(0.0, 0.0, 0.0),
      Vectors.dense(0.1, 0.1, 0.1),
      Vectors.dense(0.2, 0.2, 0.2),
      /***======�����ĵ�����ӳ�3============**/
      Vectors.dense(9.0, 9.0, 9.0),
      Vectors.dense(9.1, 9.1, 9.1),
      Vectors.dense(9.2, 9.2, 9.2),
      /***======�����ĵ�����ӳ�3============**/
      Vectors.dense(15.1, 16.1, 17.0),
      Vectors.dense(18.0, 17.0, 19.0),
      Vectors.dense(20.0, 21.0, 22.0))
    val parsedDataRdd = sc.parallelize(pointsData, 3)

    // Cluster the data into two classes using KMeans
    val numClusters = 3 //Ԥ���Ϊ3������
    val numIterations = 20 //����20��
    val runTimes = 10 //����10�Σ�ѡ�����Ž�
    var clusterIndex: Int = 0
    //train���������ݼ����о���ѵ������������᷵�� KMeansModel ��ʵ��
    val clusters: KMeansModel =
      KMeans.train(parsedData, numClusters, numIterations, runTimes)
      
     
     
    //����������ݷֱ������Ǹ�����
    parsedData.map(v =>
      {
	//predict ���µ����ݵ�������������Ԥ��
        println(v.toString() + " belong to cluster :" + clusters.predict(v))
      }).collect()
    //����cost
     /**
      * computeCostͨ�������������ݵ㵽����������ĵ��ƽ���������������Ч��,
      * ͳ�ƾ���������������
      */
    val wssse = clusters.computeCost(parsedData)
    println("Within Set Sum of Squared Errors = " + wssse)
    //��ӡ�����ĵ�
    /**
              ���ĵ�(Cluster centers):
 			[17.7,18.03333333333333,19.333333333333332]
 			[0.1,0.1,0.1]
 			[9.1,9.1,9.1]
     */
    println("���ĵ�(Cluster centers):")
    for (center <- clusters.clusterCenters) {
      println(" " + center);
    }
    val points = Seq(
      Vectors.dense(1.1, 2.1, 3.1),
      Vectors.dense(10.1, 9.1, 11.1),
      Vectors.dense(21.1, 17.1, 16.1))
    val rdd = sc.parallelize(points, 3)

    val data1 = sc.parallelize(Array(Vectors.dense(1.1, 2.1, 3.1)))
    val data2 = sc.parallelize(Array(Vectors.dense(10.1, 9.1, 11.1)))
    val data3 = sc.parallelize(Array(Vectors.dense(21.1, 17.1, 16.1)))

    val pointsdens = clusters.predict(rdd).collect()
    
    //Ԥ��������
    for (p <- pointsdens) {
      println(p)

    }
    //����һЩԤ��
    val l = clusters.predict(data1).collect()
    val ll = clusters.predict(data2).collect()
    val lll = clusters.predict(data3).collect()
    /*   for(a<-l){
       println(a)
     }*/
    println("Prediction of (1.1, 2.1, 3.1): " + l(0))
    println("Prediction of (10.1, 9.1, 11.1): " + ll(0))
    println("Prediction of (21.1, 17.1, 16.1): " + lll(0))
     import breeze.linalg._
    import breeze.numerics.pow
    //����ŷ������֮��,����ƽ����֮��
    def computeDistance(v1: DenseVector[Double], v2: DenseVector[Double]): Double = pow(v1 - v2, 2).sum //ƽ����֮��

  }
 
}