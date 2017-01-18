package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.tree.GradientBoostedTrees
import org.apache.spark.mllib.tree.configuration.{BoostingStrategy, Algo}
import org.apache.spark.mllib.util.MLUtils
/**
 *Spark coolbook p139 
 * �ݶ�����������:�ۺ϶��������,��������,��������,GBTֻ�����ڶ�����ͻع�,��֧�ֶ����
 * �ݶ������������㷨һ��ѵ��һ����,����ÿ���µ��������֮ǰѵ��������ȱ�����Ľ��㷨
 * �ݶ������������㷨Ŀ���Ǹ�һ����Ԥ�����Ƿ�ӵ�����õ�����
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
object GradientBoostedTreesExample {
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
    //����һ��������������Բ����õ�������Ϊ3(���ɭ��Ҳ֧�ֻع�)
    val boostingStrategy =BoostingStrategy.defaultParams("Classification")
        boostingStrategy.numIterations = 3
    //�ݶ�����������:�ۺ϶��������,��������,��������
    val model = GradientBoostedTrees.train(trainingData,boostingStrategy)
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