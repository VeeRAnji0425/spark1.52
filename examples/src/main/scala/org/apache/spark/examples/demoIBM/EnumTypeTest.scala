package org.apache.spark.examples.demoIBM

/**
 * 
 */

object MyEnum extends Enumeration {
  //���������ֶ�,Ȼ����Value���ý����ǳ�ʼ��,ÿ�ε���Value�������ڲ������ʵ��,���ڲ���Ҳ����Value,
  //������Ҳ������Value��������ID/����/������������
  val Red, Yellow, Green = Value
}
object Hi {
  def main(args: Array[String]) {
    //����ͨ���������ֱ������ö��ֵ
    //import MyEnum._
    //��סö�ٵ�������MyEnum.Value������MyEnum
    //type MyEnum = Value //�˾仰�����������ͱ���,�����Ļ�,ö�ٵ����;ͱ����MyEnum.MyEnum
    //ö��ֵ��ID��ͨ��id��������,����ͨ��toString��������,��MyEnum.values�ĵ����������ö��ֵ�ļ���
    //����ͨ��ö�ٵ�ID�����������в��Ҷ�λ,���������������һ��
    println(MyEnum.values.toString())     
    println(MyEnum(0))
    println(MyEnum.withName("Red"))
  }
}
