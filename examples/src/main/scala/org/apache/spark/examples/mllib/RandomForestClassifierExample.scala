package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.mllib.tree.configuration.Strategy
/**
 *Spark coolbook p135 
 * ���ɭ���㷨�㷨Ŀ���Ǹ�һ����Ԥ�����Ƿ�ӵ�����õ�����
 * ��������õ�����
 * ����	���	����	   ���θ�	����  ������	  �ӻ�	��ŭ
        �ܿ�		0		0		0			0			0				1			1
        ������   1		1		1			1			1				0			0
        ����		0		0		0			1			0				1			1
        ���		1		1		1			0			1				0			0
        ����		0		0		0			0			0				1			1
        �ܲ�		1		1		1			1			0				0			0
        ����		1		0		1			1			1				0			0
        ����		0		1		0			0			0				1			1
        �޶�		1		0		1			1			1				0			0
        ����		0		1		0			0			0				1			1
      ʹ��libsvm��ʽ��ʾ��ʽ
        0 5:1 6:1
        1 1:1 2:1 3:1 4:1
        0 3:1 5:1 6:1
        1 1:1 2:1 4:1
        0 5:1 6:1
        1 1:1 2:1 3:1 4:1
        0 1:1 5:1 6:1
        1 2:1 3:1 4:1
        0 1:1 5:1 6:1
 **/
object RandomForestClassifierExample {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
 /**
 *  libSVM�����ݸ�ʽ
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
 *  <index>����1��ʼ������,�����ǲ�����
 *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
 */
    // ��������
    val data = MLUtils.loadLibSVMFile(sc, "../data/mllib/rf_libsvm_data.txt")
    // �������������Ϊ���ݣ�һ������ѵ����һ�����ڲ���
    val splits = data.randomSplit(Array(0.7, 0.3))
    //���ݷֳ�ѵ���Ͳ������ݼ�
    val (trainingData, testData) = (splits(0), splits(1))
    //����һ�������������(���ɭ��Ҳ֧�ֻع�)
    val treeStrategy = Strategy.defaultStrategy("Classification")
    //ѵ��ģ��
    val model = RandomForest.trainClassifier(trainingData,treeStrategy, numTrees=3,
                featureSubsetStrategy="auto", seed =12345)
    //���ڲ���ʵ������ģ�Ͳ�������Դ���
    val testErr = testData.map { point =>
            //Ԥ��
            val prediction = model.predict(point.features)
            if (point.label == prediction) 
                1.0 
            else 0.0}.mean()//ƽ����
    //���ģ��
    println("Test Error = " + testErr)
    println("Learned Random Forest:n" + model.toDebugString)
  }
}