package org.apache.spark.examples

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimerTask
import java.util.Timer

object schedule {
  /**
   * ��java�У�Timer����Ҫ���ڶ�ʱ�ԡ����������� �Ĵ�����������������������Ƚ�����⣬
   * �Ǿ���schedule��scheduleAtFixedRate���������������ʵ������һ��
   * ��1��schedule��������fixed-delay���������һ��ִ��ʱ�䱻delay�ˣ�����ִ��ʱ�䰴 �� ��һ�� ʵ��ִ����ɵ�ʱ��� ���м���
   * ��2��scheduleAtFixedRate��������fixed-rate���������һ��ִ��ʱ�䱻delay�ˣ�����ִ��ʱ�䰴�� ��һ�ο�ʼ�� ʱ��� ���м��㣬
   * ����Ϊ�ˡ�catch up������ִ������,TimerTask�е�ִ������Ҫ����ͬ��
   *
   *
   */
  def main(args: Array[String]) {

    /**
     *
     * ���ϵĴ��룬��ʾ��2010-11-26 00:20:00�뿪ʼִ�У�ÿ3����ִ��һ��
     * ������2010/11/26 00:27:00ִ��
     * ���ϻ��ӡ��3��
     * execute task!   00:20
     * execute task!   00:23    catch up
     * execute task!   00:26    catch up
     * ��һ��ִ��ʱ����00:29�������00:26
     * ������schedule����ʱ����2010/11/26 00:27:00ִ��
     * ���ӡ��1��
     * execute task!   00:20   ��catch up
     * ��һ��ִ��ʱ��Ϊ00:30�������00:27
     * ���Ͽ��ǵĶ��������趨��timer��ʼʱ��󣬳���ű�ִ��
     */
    val fTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    val d1 = fTime.parse("2005/12/30 14:10:00");
    val timer: Timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      override def run(): Unit = {
        System.out.println("this is task you do6");
      }
    }, d1, 3 * 60 * 1000);

  }
}