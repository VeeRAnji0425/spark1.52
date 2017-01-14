package org.apache.spark.examples.mllib
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating
/**
 * ���Է�����Эͬ����
 */
object ALStrainImplicit {
  def main(args: Array[String]) {
    /**
     * Эͬ����ALS�㷨�Ƽ��������£�
     * �������ݵ� ratings RDD,ÿ�м�¼����:user,product,rate
     * �� ratings �õ��û���Ʒ�����ݼ�:(user, product)
     * ʹ��ALS�� ratings ����ѵ��
     * ͨ�� model ���û���Ʒ����Ԥ������:((user, product),rate)
     * �� ratings �õ��û���Ʒ��ʵ������:((user, product),rate)
     * �ϲ�Ԥ�����ֺ�ʵ�����ֵ��������ݼ������������
     */
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
    /**�ļ���ÿһ�а���һ���û�id����Ʒid������****/
    //��kaggle_songs���ݵ��뵽RDD
    val songs = sc.textFile("../data/mllib/als/kaggle_songs.txt")
    //��kaggle_users���ݵ��뵽RDD
    val users = sc.textFile("../data/mllib/als/kaggle_users.txt")
    //��kaggle_visible_evaluation_triplets���ݵ��뵽RDD
    val triplets = sc.textFile("../data/mllib/als/kaggle_visible_evaluation_triplets.txt")
    //����������ת��ΪPairRDD
    val songIndex = songs.map(_.split("\\W+")).map(v =>(v(0),v(1).toInt))
    //��songIndexת��Ϊmap����
     /**
     * Map(SOWZAXW12A8C13FDF2 -> 345316, 
        SOJPVJN12A6BD4FE30 -> 152438, 
        SOCQGQQ12AB01899D1 -> 42237, 
        SOAZZNE12AB0186B41 -> 16112, 
        SOKVRTV12AC4689943 -> 171148, 
        SOWTILD12A6D4FA5CA -> 342308, 
        SOHEABL12A58A80C21 -> 114376, 
        SOEKOOE12A8C13FE18 -> 70864, 
        SODRTXS12A8C13CC3F -> 59311)
     */
    val songMap = songIndex.collectAsMap
    //���û�����ת��ΪPairRDD
    val userIndex = users.zipWithIndex.map( t => (t._1,t._2.toInt))
    //��UserIndexת��ΪMap����
    /**
     * Map(af42f25800f70fe00eb91e6d5a7215493e1386cb -> 58435, 
       d54d311bcbdb6486ef17cc22414e5ce9a7f4834e -> 64818, 
       3ed4de747450da358ef70aee9da59c35f87535ef -> 21807, 
       159d56cc42ee8c91de32f7e7fcfaea77d486f9d9 -> 55694, 
       9b9fb6acabe8bdde3012ddc21626c435e62bc077 -> 57964, 
       a8f1947bf5122527228d6780535767657f679318 -> 99490, 
       07c040fbe93d6a56b316ff3383d046e80f9dd511 -> 95351, 
       77df6308c2ff6b24f1401106244b044db2e026a2 -> 319, 
       40420d1b02b2f590fc28fee7cf58da7c1dab7e40 -> 89248, 
       9475c8039e9bab7221b57ab23db7ccb4193ff6c7 -> 14820)
     */
    val userMap = userIndex.collectAsMap
    //�㲥userMap
    val broadcastUserMap = sc.broadcast(userMap)
    //�㲥songMap
    val broadcastSongMap = sc.broadcast(songMap)
    //��triplets����ת��Ϊһ������
    val tripArray = triplets.map(_.split("\\W+"))
    //����Rating��
    import org.apache.spark.mllib.recommendation.Rating
    //��tripArray����ת��Ϊ��������RDD
    val ratings = tripArray.map { case Array(user, song, plays)=>
    val userId = broadcastUserMap.value.getOrElse(user, 0)
    val songId = broadcastUserMap.value.getOrElse(song, 0)
    Rating(userId, songId, plays.toDouble)
    }
    //����ALS
    import org.apache.spark.mllib.recommendation.ALS
    //��Rank����Ϊ10,����������Ϊ10,Rankģ���е�Ǳ��������    
    val model = ALS.trainImplicit(ratings, 10, 10)
    //��triplet�е����û��͸���Ԫ��
    val usersSongs = ratings.map( r => {
      println(r.user+"|||"+r.product)
      (r.user, r.product) 
    })
  

    //Ԥ���û��͸���
    val predictions = model.predict(usersSongs)
    predictions.foreach { x => println(x.user.toString()+"|||||"+x.rating.toString()+"======="+x.product.toString()) }
  }
}