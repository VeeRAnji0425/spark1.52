package org.apache.spark.examples.mllib

import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.regression.LinearRegressionWithSGD
/**
 * ���Իع�
 */
object LinearRegressionDemo {
  def main(args: Array[String]): Unit = {

    // ���β���Ҫ����־��ʾ�ն���
    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)
    // �������л���
    val conf = new SparkConf().setAppName("LinearRegressionDemo").setMaster("local[4]")
    val sc = new SparkContext(conf)
    // ��������
    val data = sc.textFile("../data/mllib/ridge-data/lpsa.data")

    val parsedData = data.map { line =>

      val parts = line.split(',')
	//LabeledPoint��ǵ��Ǿֲ�����,�����������ܼ��ͻ���ϡ����,ÿ�������������һ����ǩ(label)
      LabeledPoint(parts(0).toDouble, Vectors.dense(parts(1).split(' ').map(_.toDouble)))

    }

    // Building the model
    //��������ģ��

    val numIterations = 100

    val model = LinearRegressionWithSGD.train(parsedData, numIterations)

    // Evaluate model on training examples and compute training error
    //ʹ��ѵ����������ģ�Ͳ��Ҽ���ѵ�����
    val valuesAndPreds = parsedData.map { point =>

      val prediction = model.predict(point.features)
      //println("point.label:"+point.label+"\t prediction:"+prediction)
      (point.label, prediction)
    }
    //�������
    val MSE = valuesAndPreds.map { case (x, y) =>
           var w=math.pow((x - y), 2)
           println("x:"+x+"\t y:"+y+"\t x-y:"+(x - y)+"\t pow:"+math.pow((x - y), 2))
           w      
    }.reduce(_ + _) / valuesAndPreds.count //
    //�������,�ڶ��ַ�ʽ������������Ԥ��ֵ
    val MSE2 = valuesAndPreds.map { case (v, p) => math.pow((v - p), 2)}.mean() //��ƽ��ֵ
    println("training Mean Squared Error = " + MSE + "\t MSE2:" + MSE2)

    sc.stop()

  }
}