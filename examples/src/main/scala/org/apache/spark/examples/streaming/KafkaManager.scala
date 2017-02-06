package org.apache.spark.streaming.kafka
import kafka.common.TopicAndPartition
import kafka.message.MessageAndMetadata
import kafka.serializer.Decoder
import org.apache.spark.SparkException
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka.KafkaCluster.{LeaderOffset, Err}

import scala.reflect.ClassTag

/**
 * Created by knowpigxia on 15-8-5.
 */
class KafkaManager(val kafkaParams: Map[String, String]) extends Serializable {

  private def kc = new KafkaCluster(kafkaParams)

  /**
   * ����������
   * @param ssc
   * @param kafkaParams
   * @param topics
   * @tparam K
   * @tparam V
   * @tparam KD
   * @tparam VD
   * @return
   */
  def createDirectStream[K: ClassTag, V: ClassTag, KD <: Decoder[K] : ClassTag, VD <: Decoder[V] : ClassTag](ssc: StreamingContext,
            // kafkaParams: Map[String, String],
            topics: Set[String]): InputDStream[(K, V)] = {
    val groupId = kafkaParams.get("group.id").get
    // ��zookeeper�϶�ȡoffsetsǰ�ȸ���ʵ���������offsets
    setOrUpdateOffsets(topics, groupId)

    //��zookeeper�϶�ȡoffset��ʼ����message
    val partitionsE = kc.getPartitions(topics)
    if (partitionsE.isLeft) throw new SparkException("get kafka partition failed:")
    val partitions = partitionsE.right.get
    val consumerOffsetsE = kc.getConsumerOffsets(groupId, partitions)
    if (consumerOffsetsE.isLeft) throw new SparkException("get kafka consumer offsets failed:")
    val consumerOffsets = consumerOffsetsE.right.get
    MyKafkaUtils.createDirectStream[K, V, KD, VD, (K, V)](
      ssc, kafkaParams, consumerOffsets, (mmd: MessageAndMetadata[K, V]) => (mmd.key, mmd.message))
  }

  /**
   * ����������ǰ,����ʵ�����������������offsets
   * @param topics
   * @param groupId
   */
  private def setOrUpdateOffsets(topics: Set[String], groupId: String): Unit = {
    topics.foreach(topic => {
      var hasConsumed = true
      val partitionsE = kc.getPartitions(Set(topic))
      if (partitionsE.isLeft) throw new SparkException("get kafka partition failed:")
      val partitions = partitionsE.right.get
      val consumerOffsetsE = kc.getConsumerOffsets(groupId, partitions)
      if (consumerOffsetsE.isLeft) hasConsumed = false
      if (hasConsumed) {
        // ���ѹ�
        /**
         * ���zk�ϱ����offsets�Ѿ���ʱ��,��kafka�Ķ�ʱ��������Ѿ���������offsets���ļ�ɾ����
         * ����������,ֻҪ�ж�һ��zk�ϵ�consumerOffsets��earliestLeaderOffsets�Ĵ�С,
         * ���consumerOffsets��earliestLeaderOffsets��С�Ļ�,˵��consumerOffsets�ѹ�ʱ,
         * ��ʱ��consumerOffsets����ΪearliestLeaderOffsets
         */
        val earliestLeaderOffsets = kc.getEarliestLeaderOffsets(partitions).right.get
        val consumerOffsets = consumerOffsetsE.right.get

        // ����ֻ�Ǵ��ڲ��ַ���consumerOffsets��ʱ,����ֻ���¹�ʱ������consumerOffsetsΪearliestLeaderOffsets
        var offsets: Map[TopicAndPartition, Long] = Map()
        consumerOffsets.foreach({
          case (tp, n) =>
            val earliestLeaderOffset = earliestLeaderOffsets(tp).offset
            if (n < earliestLeaderOffset) {
              println("consumer group:" + groupId + ",topic:" + tp.topic + ",partition:" + tp.partition +
                " offsets�Ѿ���ʱ,����Ϊ" + earliestLeaderOffset)
              offsets += (tp -> earliestLeaderOffset)
            }
        })
        if (!offsets.isEmpty) {
          kc.setConsumerOffsets(groupId, offsets)
        }
      } else {
        // û�����ѹ�
        val reset = kafkaParams.get("auto.offset.reset").map(_.toLowerCase)
        var leaderOffsets: Map[TopicAndPartition, LeaderOffset] = null
        if (reset == Some("smallest")) {
          leaderOffsets = kc.getEarliestLeaderOffsets(partitions).right.get
        } else {
          leaderOffsets = kc.getLatestLeaderOffsets(partitions).right.get
        }
        val offsets = leaderOffsets.map {
          case (tp, offset) => (tp, offset.offset)
        }
        kc.setConsumerOffsets(groupId, offsets)
      }
    })
  }

  /**
   * ����zookeeper�ϵ�����offsets
   * @param rdd
   */
  def updateZKOffsets(rdd: RDD[(String, String)]): Unit = {
    val groupId = kafkaParams.get("group.id").get
    val offsetsList = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

    for (offsets <- offsetsList) {
      val topicAndPartition = TopicAndPartition(offsets.topic, offsets.partition)
      val o = kc.setConsumerOffsets(groupId, Map((topicAndPartition, offsets.untilOffset)))
      if (o.isLeft) {
        println(s"Error updating the offset to Kafka cluster: ${o.left.get}")
      }
    }
  }
}

import scala.reflect.ClassTag

object MyKafkaUtils {
  def createDirectStream[
  K: ClassTag,
  V: ClassTag,
  KD <: Decoder[K] : ClassTag,
  VD <: Decoder[V] : ClassTag,
  R: ClassTag](
                ssc: StreamingContext,
                kafkaParams: Map[String, String],
                fromOffsets: Map[TopicAndPartition, Long],
                messageHandler: MessageAndMetadata[K, V] => R
                ): InputDStream[R] = {
    val cleanedHandler = ssc.sc.clean(messageHandler)
    new MyDirectKafkaInputDStream[K, V, KD, VD, R](
      ssc, kafkaParams, fromOffsets, cleanedHandler)
  }
}

private[streaming]
class MyDirectKafkaInputDStream[K: ClassTag,
V: ClassTag,
U <: Decoder[K] : ClassTag,
T <: Decoder[V] : ClassTag,
R: ClassTag](ssc_ : StreamingContext,
             override val kafkaParams: Map[String, String],
             override val fromOffsets: Map[TopicAndPartition, Long],
             messageHandler: MessageAndMetadata[K, V] => R)
  extends DirectKafkaInputDStream[K, V, U, T, R](ssc_, kafkaParams, fromOffsets, messageHandler) {
  override protected def clamp(leaderOffsets: Map[TopicAndPartition, LeaderOffset]): Map[TopicAndPartition, LeaderOffset] = {
    maxMessagesPerPartition.map {
      mmp =>
        leaderOffsets.map {
          case (tp, lo) =>
            tp -> lo.copy(offset = Math.min(currentOffsets(tp) + mmp, lo.offset))
        }
    }.getOrElse(leaderOffsets)
  }

  val maxRateLimitPerPartition = 100

  override protected val maxMessagesPerPartition: Option[Long] = {
    //    val estimatedRateLimit = rateController.map(_.getLatestRate().toInt)
    val estimatedRateLimit = Some(10000)
    val numPartitions = currentOffsets.keys.size

    val effectiveRateLimitPerPartition = estimatedRateLimit
      .filter(_ > 0)
      .map {
      limit =>
        if (maxRateLimitPerPartition > 0) {
          Math.min(maxRateLimitPerPartition, limit / numPartitions)
        } else {
          limit / numPartitions
        }
    }.getOrElse(maxRateLimitPerPartition)

    if (effectiveRateLimitPerPartition > 0) {
      val secsPerBatch = context.graph.batchDuration.milliseconds.toDouble / 1000
      Some((secsPerBatch * effectiveRateLimitPerPartition).toLong)
    } else {
      None
    }
  }

}
