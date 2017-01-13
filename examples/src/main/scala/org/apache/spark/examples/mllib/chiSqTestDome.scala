package org.apache.spark.examples.mllib

import org.apache.spark.mllib.stat.test.ChiSqTestResult
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.stat.Statistics
/**
 * ������ǵ�5���Ƿֱ���Ϊ1,2,3,4,5��������ת���ɴ�����ǣ���¼ÿ����ָ���Լ��Ĵ�����
 * ��һ���Ľ��Ϊ(1,7,2,3,18)���ڶ�������ǵĽ��Ϊ(7,8,6,7,9)��
 * ���������޼��裺����ǵ�ÿ����ָ���Լ��ĸ�������ͬ�ġ�
 */
object chiSqTestDome {
  def main(args: Array[String]) {
    val vec1 = Vectors.dense(1, 7, 2, 3, 18)//31��
    val vec2 = Vectors.dense(7, 8, 6, 7, 9)//73��
    val goodnessOfFitTestResult1 = Statistics.chiSqTest(vec1)
    val goodnessOfFitTestResult2 = Statistics.chiSqTest(vec2)
    println(goodnessOfFitTestResult1)
    println(goodnessOfFitTestResult2)
   
    /**
     * Chi squared test summary:
      method: pearson
      degrees of freedom = 4        ���ɶ�
      statistic = 31.41935483870968 ��׼��
      pValue = 2.513864414077638E-6 �ٶ�ֵ���������
      Very strong presumption against null hypothesis: observed follows the same distribution as expected..
      Chi squared test summary:
      method: pearson 
      degrees of freedom = 4          ���ɶ�
      statistic = 0.7027027027027026  ��׼��
      pValue = 0.9509952049458091     �ٶ�ֵ���������
      No presumption against null hypothesis: observed follows the same distribution as expected..     
     * ���
			������5��ά�ȵ�����,���ɶ�Ϊ4�����ݾ��ȷֲ�,ÿ��ά�ȳ��ֵĸ���Ӧ����1/5=0.2��
			�����ݲ�����������,��5��ֵ���ֵĴ�����1,7,2,3,18,��Ȼ�ǲ����ϸ��ʶ���0.2�ġ�
			��һ��ʵ���г��������ķֲ��Ŀ����Լ�Ϊp-value��ֵ(2.5138644141226737E-6),
			����һ��ʵ�����ǲ����ܳ����ˣ������������������㹻��������Ϊ�����ǲ������ġ�
			���Ծܾ������޼���(observed follows the same distribution as expected),
			���������ݲ�������ȷֲ����������ǿ������������ֲ���ԭ���������Ľ������
			����һ������(7,8,6,7,9)������ϵȸ��ʵĳ���,���Խ������޼��衣
     */
  }
}