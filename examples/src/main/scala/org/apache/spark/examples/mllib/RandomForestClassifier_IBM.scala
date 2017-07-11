package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.mllib.util.MLUtils
/**
 * 参考文献:
 * https://www.ibm.com/developerworks/cn/opensource/os-cn-spark-random-forest/
 * 随机森林算法算法使用 demo
 * 随机森林(Random Forests)其实就是多个决策树,每个决策树有一个权重,对未知数据进行预测时,
 * 会用多个决策树分别预测一个值,然后考虑树的权重,将这多个预测值综合起来,
 * 对于分类问题,采用多数表决,对于回归问题,直接求平均。
 */
object RandomForestDemo {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
 /**
 *  libSVM的数据格式
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  其中<label>是训练数据集的目标值,对于分类,它是标识某类的整数(支持多个类);对于回归,是任意实数
 *  <index>是以1开始的整数,可以是不连续
 *  <value>为实数,也就是我们常说的自变量
 */
    // 加载数据
    val data = MLUtils.loadLibSVMFile(sc, "data/mllib/sample_libsvm_data.txt")
    // 将数据随机分配为两份,一份用于训练,一份用于测试
    val splits = data.randomSplit(Array(0.7, 0.3))
    val (trainingData, testData) = (splits(0), splits(1))
    // 随机森林训练参数设置
    //分类数
    val numClasses = 2
    // categoricalFeaturesInfo 为空,意味着所有的特征为连续型变量
    val categoricalFeaturesInfo = Map[Int, Int]()
    //树的个数
    val numTrees = 3
    //特征子集采样策略,auto 表示算法自主选取
    val featureSubsetStrategy = "auto"
    //纯度计算
    val impurity = "gini"
    //树的最大深度,为了防止过拟合,设定划分的终止条件
    val maxDepth = 4
    //连续特征离散化的最大数量,以及选择每个节点分裂特征的方式
    val maxBins = 32
    //训练随机森林分类器,trainClassifier 返回的是 RandomForestModel 对象
    val model = RandomForest.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
      numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins)
    // 测试数据评价训练好的分类器并计算错误率
    val labelAndPreds = testData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    val testErr = labelAndPreds.filter(r => r._1 != r._2).count.toDouble / testData.count()
    println("Test Error = " + testErr)
    println("Learned classification forest model:\n" + model.toDebugString)
    // 将训练后的随机森林模型持久化
    model.save(sc, "myModelPath")
    //加载随机森林模型到内存
    val sameModel = RandomForestModel.load(sc, "myModelPath")

  }
}
