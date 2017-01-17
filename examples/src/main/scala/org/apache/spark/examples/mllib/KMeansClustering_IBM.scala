package org.apache.spark.examples.mllib
import org.apache.spark.{ SparkContext, SparkConf }
import org.apache.spark.mllib.clustering.{ KMeans, KMeansModel }
import org.apache.spark.mllib.linalg.Vectors
/**
 * http://www.ibm.com/developerworks/cn/opensource/os-cn-spark-practice4/index.html
 * ���ǽ�����Ŀ��ͻ����������ݣ���ÿһ����Ϊһ������ָ�꣬�����ݼ����о������(��8��)
 */
object KMeansClustering {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("KMeansClustering")
    val sc = new SparkContext(sparkConf)   
    /**
     * 
     * Channel Region Fresh Milk Grocery Frozen Detergents_Paper Delicassen
     * 2 3
     * 12669 9656 7561 214 2674 1338
     * 2 3 7057 9810 9568 1762 3293 1776
     * 2 3 6353 8808
     * 7684 2405 3516 7844
     */
    val rawTrainingData = sc.textFile("../data/mllib/wholesale_customers_data_training.txt")
    //isColumnNameLine ���ǵ����� 
    val parsedTrainingData =
      rawTrainingData.filter(!isColumnNameLine(_)).map(line => {
        // println(">>>>>>>>>>>>" + line.split(",").map(_.trim).filter(!"".equals(_)))
        Vectors.dense(line.split(",").map(_.trim).filter(!"".equals(_)).map(_.toDouble))
      }).cache()
    // Cluster the data into two classes using KMeans
    val numClusters = 6//k ��ʾ�����ľ���ĸ���
    val numIterations = 10 //��ʾ���������������ĵ�������
    val runTimes = 3 //��ʾ�㷨�����еĴ���,ѡ�����Ž�
    var clusterIndex: Int = 0 
    //train���������ݼ����о���ѵ������������᷵�� KMeansModel ��ʵ��
    val clusters: KMeansModel =
      KMeans.train(parsedTrainingData, numClusters, numIterations, runTimes)
     //�������ĵ�
    println("Cluster Number:" + clusters.clusterCenters.length)
    println("Cluster Centers Information Overview:")
    //��ӡ�����ĵ�
    clusters.clusterCenters.foreach(
      x => {
        println("Center Point of Cluster " + clusterIndex + ":")
        println(x)
        clusterIndex += 1
      })
    //begin to check which cluster each test data belongs to based on the clustering result
    val rawTestData = sc.textFile("../data/mllib/wholesale_customers_data_test.txt")
    val parsedTestData = rawTestData.filter(!isColumnNameLine(_)).map(line =>
      {
        //��ӡÿ��ֵ
     /*   line.split(",").map(_.trim).filter(!"".equals(_)).map{line=>
          println(line)
        }*/
        Vectors.dense(line.split(",").map(_.trim).filter(!"".equals(_)).map(_.toDouble))
      })
    parsedTestData.collect().foreach(testDataLine => {
      //����������ݷֱ������Ǹ�����
      val predictedClusterIndex: Int = clusters.predict(testDataLine)
      println("��������: " + testDataLine.toString + " ���ھ��� " +
        predictedClusterIndex)
    })
    println("Spark MLlib K-means clustering test finished.")
    //����KMeansģ�� ���ѡ��Kֵ
    val ks: Array[Int] = Array(3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 50, 80, 100)
    ks.foreach(cluster => {
      //parsedTrainingDataѵ��ģ������
      val model: KMeansModel = KMeans.train(parsedTrainingData, cluster, 30, 1)
      //KMeansModel �����ṩ�� computeCost �������÷���ͨ�������������ݵ㵽����������ĵ��ƽ���������������Ч����  
      //ͳ�ƾ���������������
      val ssd = model.computeCost(parsedTrainingData)
      //model.predict(point)
      println("sum of squared distances of points to their nearest center when k=" + cluster + " -> " + ssd)
    })
  }
  //���˱�����
  private def isColumnNameLine(line: String): Boolean = {
    if (line != null &&
      line.contains("Channel")) true
    else false
  }
}