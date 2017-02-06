package org.apache.spark.examples.IBM
import org.apache.spark.SparkConf
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.HashPartitioner
import org.apache.spark.streaming.Duration
/**
 * ����ο�:
 * https://www.ibm.com/developerworks/cn/opensource/os-cn-spark-practice2/
 */
object WebPagePopularityValueCalculator {

  private val checkpointDir = "popularity-data-checkpoint"
  private val msgConsumerGroup = "user-behavior-topic-message-consumer-group"

  def main(args: Array[String]) {
    /*    if (args.length < 2) {
      println("Usage:WebPagePopularityValueCalculator zkserver1:2181,zkserver2:2181,zkserver3:2181 consumeMsgDataTimeInterval(secs)")
      System.exit(1)
    }*/
    val Array(zkServers, processingInterval) = Array("192.168.0.39:2181", "2")
    val jarpath = "D:\\eclipse44_64\\workspace\\spark1.5\\examples\\lib\\"
    val conf = new SparkConf().setAppName("Web Page Popularity Value Calculator").setMaster("spark://dept3:8088")
      /** val conf = new SparkConf().setMaster("spark://dept3:8088").setAppName("Chapter01")**/
      .set("spark.driver.port", "8088")
      .set("spark.fileserver.port", "3306")
      .set("spark.replClassServer.port", "8080")
      .set("spark.broadcast.port", "8089")
      .set("spark.blockManager.port", "15000")
      .setJars(Array(jarpath + "spark-streaming-kafka_2.10-1.5.3-20151212.002413-63.jar", jarpath + "kafka_2.10-0.10.1.0.jar", jarpath + "kafka-clients-0.10.1.0.jar", jarpath + "zkclient-0.3.jar", jarpath + "metrics-core-2.2.0.jar", "D:\\testjar\\spark-examples-ibm.jar"))
    val ssc = new StreamingContext(conf, Seconds(processingInterval.toInt))
    //using updateStateByKey asks for enabling checkpoint
    //���ҿ��� checkpoint ����,��Ϊ������Ҫʹ�� updateStateByKey ԭ��ȥ�ۼƵĸ�����ҳ������ȶ�ֵ
    ssc.checkpoint(checkpointDir)
    //���� Spark�ṩ�� KafkaUtils.createStream����������Ϣ����,��������᷵�� ReceiverInputDStream����ʵ��
    val kafkaStream = KafkaUtils.createStream(
      //Spark streaming context
      //����һ��StreamingContext����
      ssc,
      //zookeeper quorum. e.g zkserver1:2181,zkserver2:2181,...zookeeper������λ��
      //����Zookeeper����Ϣ,��ΪҪ�߿���,������Zookeeper���м�Ⱥ��ء��Զ����ϻָ�
      zkServers,
      //kafka message consumer group ID
      //kafka��Ϣ������
      msgConsumerGroup,
      //Map of (topic_name -> numPartitions) to consume. Each partition is consumed in its own thread
      //Map of (topic_name -> numPartitions) to ������,ÿ�����������Լ����߳��б����ĵ�
      Map("user-behavior-topic" -> 3))
    //
    val msgDataRDD = kafkaStream.map(_._2)
    //for debug use only
    //�����ڵ���
    //�ڴ˼���е�����
    println("Coming data in this interval...")
    //msgDataRDD.print()
    // e.g page37|5|1.5119122|-1
    val popularityData = msgDataRDD.map { msgLine =>
      {
        val dataArr: Array[String] = msgLine.split("\\|")
        val pageID = dataArr(0)
        //calculate the popularity value
        //����ÿһ����Ϣ,�������ĵĹ�ʽ������ҳ������ȶ�ֵ
        val popValue: Double = dataArr(1).toFloat * 0.8 + dataArr(2).toFloat * 0.8 + dataArr(3).toFloat * 1
        (pageID, popValue)
      }
    }
    //sum the previous popularity value and current value
    //����һ����������ȥ����ҳ�ȶ���һ�εļ�����ֵ���¼����ֵ���,�õ����µ��ȶ�ֵ
    val updatePopularityValue = (iterator: Iterator[(String, Seq[Double], Option[Double])]) => {
      iterator.flatMap(t => {
        val newValue: Double = t._2.sum
        val stateValue: Double = t._3.getOrElse(0);
        Some(newValue + stateValue)
      }.map(sumedValue => (t._1, sumedValue)))
    }
    //
    val initialRDD = ssc.sparkContext.parallelize(List(("page1", 0.00)))
    //���� updateStateByKey ԭ�ﲢ�������涨�����������������ҳ�ȶ�ֵ
    //
    val stateDstream = popularityData.updateStateByKey[Double](updatePopularityValue,
      new HashPartitioner(ssc.sparkContext.defaultParallelism), true, initialRDD)
    //set the checkpoint interval to avoid too frequently data checkpoint which may
    //may significantly reduce operation throughput
    //���ü�����,�Ա������Ƶ�������ݼ���,����ܻ����Ž��Ͳ���������
    stateDstream.checkpoint(Duration(8 * processingInterval.toInt * 1000))
    //after calculation, we need to sort the result and only show the top 10 hot pages
    //�����,������Ҫ�Խ����������,ֻ��ʾǰ10������ҳ��
    stateDstream.foreachRDD { rdd =>
      {
        val sortedData = rdd.map { case (k, v) => (v, k) }.sortByKey(false)
        val topKData = sortedData.take(10).map { case (v, k) => (k, v) }
        topKData.foreach(x => {
          println(x)
        })
      }
    }
    ssc.start()
    ssc.awaitTermination()
  }
}