package org.apache.spark.examples.mllib
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.tree.configuration.Algo._
import org.apache.spark.mllib.tree.impurity.Gini
/**
 * ����������
 */
object DecisionTreeTest {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("KMeansClustering")
    val sc = new SparkContext(sparkConf)
    val data = sc.textFile("../data/mllib/sample_tree_data.csv")    
    val parsedData = data.map { line =>
      val parts = line.split(',').map(_.toDouble)
      //LabeledPoint��ǵ��Ǿֲ�����,�����������ܼ��ͻ���ϡ����,ÿ�������������һ����ǩ(label)
      LabeledPoint(parts(0), Vectors.dense(parts.tail))
    }

    val maxDepth = 5//���������ȣ�Ĭ��ֵ�� 5
    val model = DecisionTree.train(parsedData, Classification, Gini, maxDepth)

    val labelAndPreds = parsedData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    val trainErr = labelAndPreds.filter(r => r._1 != r._2).count().toDouble / parsedData.count
    println("Training Error = " + trainErr)
  }
}