package org.apache.spark.examples.mllib
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.tree.configuration.Algo._
import org.apache.spark.mllib.tree.impurity.Entropy
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
/**
 * Spark coolbook p131
 * ������
 */
object DecisionTreeExample {
   def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("DecisionTreeExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
    /**
     * �����Բ�̬��������Ϊ��ǵ�LabledPoint����
     */   
    /**
     * �����Ƿ�(2,1)
     * ����Ƿ�(2,1)
     * �¶ȸ�,����,��(3,2,1)
     *�Ƿ������|����|���|�¶�| 
      0.0,			1.0,	1.0,2.0
      0.0,			1.0,	1.0,1.0
      0.0,			1.0,	1.0,0.0
      0.0,			0.0,	1.0,2.0
      0.0,			0.0,	1.0,0.0
      1.0,			0.0,	0.0,2.0
      1.0,			0.0,	0.0,1.0
      0.0,			0.0,	0.0,0.0 
     */
    //�����ļ�
    val data = sc.textFile("../data/mllib/tennis.csv")
    //�������ݲ��������ص�LablePoint
    val parsedData = data.map {line => 
          val parts = line.split(',').map(_.toDouble)
          LabeledPoint(parts(0), Vectors.dense(parts.tail))
          }
    //����Щ����ѵ���㷨
   val model = DecisionTree.train(parsedData, Classification,Entropy, 3)
    //����һ��������ʾ����,���,����
   val v=Vectors.dense(0.0,1.0,0.0)
   //Ԥ���Ƿ������
   model.predict(v)
  
  }
}