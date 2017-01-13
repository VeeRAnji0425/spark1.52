/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// scalastyle:off println
package org.apache.spark.examples.mllib

import java.text.BreakIterator

import scala.collection.mutable

import scopt.OptionParser

import org.apache.log4j.{Level, Logger}

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.clustering.{EMLDAOptimizer, OnlineLDAOptimizer, DistributedLDAModel, LDA}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD


/**
 * ���� LDA��һ���ĵ���������ģ��
 * An example Latent Dirichlet Allocation (LDA) app. Run with
 * һ���������׷���(LDA)��Ӧ������
 * {{{
 * ./bin/run-example mllib.LDAExample [options] <input>
 * }}}
 * If you use it as a template to create your own app, please use `spark-submit` to submit your app.
 */
object LDAExample {

  private case class Params(
      input: Seq[String] = Seq.empty,
      k: Int = 20,//���ƶϵ����⣨�أ�����Ŀ
      maxIterations: Int = 10,//��������
      docConcentration: Double = -1,//�ĵ���������("theta")������ֲ����в���(ͨ����Ϊ��alpha")
      topicConcentration: Double = -1,//����������ֵ�����ֲ����в���(ͨ����Ϊ��beta"��"eta")
      vocabSize: Int = 10000,//
      stopwordFile: String = "",
      algorithm: String = "em",
      checkpointDir: Option[String] = None,//���ü�����(>=1)
      checkpointInterval: Int = 10) extends AbstractParams[Params]

  def main(args: Array[String]) {
    val defaultParams = Params()

    val parser = new OptionParser[Params]("LDAExample") {
      head("LDAExample: an example LDA app for plain text data.")
      opt[Int]("k")
        .text(s"number of topics. default: ${defaultParams.k}")
        .action((x, c) => c.copy(k = x))
      opt[Int]("maxIterations")
        .text(s"number of iterations of learning. default: ${defaultParams.maxIterations}")
        .action((x, c) => c.copy(maxIterations = x))
      opt[Double]("docConcentration")
        .text(s"amount of topic smoothing to use (> 1.0) (-1=auto)." +
        s"  default: ${defaultParams.docConcentration}")
        .action((x, c) => c.copy(docConcentration = x))
      opt[Double]("topicConcentration")
        .text(s"amount of term (word) smoothing to use (> 1.0) (-1=auto)." +
        s"  default: ${defaultParams.topicConcentration}")
        .action((x, c) => c.copy(topicConcentration = x))
      opt[Int]("vocabSize")
        .text(s"number of distinct word types to use, chosen by frequency. (-1=all)" +
          s"  default: ${defaultParams.vocabSize}")
        .action((x, c) => c.copy(vocabSize = x))
      opt[String]("stopwordFile")
        .text(s"filepath for a list of stopwords. Note: This must fit on a single machine." +
        s"  default: ${defaultParams.stopwordFile}")
        .action((x, c) => c.copy(stopwordFile = x))
      opt[String]("algorithm")
        .text(s"inference algorithm to use. em and online are supported." +
        s" default: ${defaultParams.algorithm}")
        .action((x, c) => c.copy(algorithm = x))
      opt[String]("checkpointDir")
        .text(s"Directory for checkpointing intermediate results." +
        s"  Checkpointing helps with recovery and eliminates temporary shuffle files on disk." +
        s"  default: ${defaultParams.checkpointDir}")
        .action((x, c) => c.copy(checkpointDir = Some(x)))
      opt[Int]("checkpointInterval")
        .text(s"Iterations between each checkpoint.  Only used if checkpointDir is set." +
        s" default: ${defaultParams.checkpointInterval}")
        .action((x, c) => c.copy(checkpointInterval = x))
      arg[String]("<input>...")
        .text("input paths (directories) to plain text corpora." +
        "  Each text file line should hold 1 document.")
        .unbounded()
        .required()
        .action((x, c) => c.copy(input = c.input :+ x))
    }

    parser.parse(args, defaultParams).map { params =>
      run(params)
    }.getOrElse {
      parser.showUsageAsError
      sys.exit(1)
    }
  }

  private def run(params: Params) {
    val conf = new SparkConf().setAppName(s"LDAExample with $params")
    val sc = new SparkContext(conf)

    Logger.getRootLogger.setLevel(Level.WARN)

    // Load documents, and prepare them for LDA.
    //�����ļ�,��׼����LDA
     //ϵͳ��ʱ���ĵ�ǰֵ,�Ժ�΢��Ϊ��λ
    val preprocessStart = System.nanoTime()
    val (corpus, vocabArray, actualNumTokens) =
      preprocess(sc, params.input, params.vocabSize, params.stopwordFile)
    corpus.cache()
    val actualCorpusSize = corpus.count()
    val actualVocabSize = vocabArray.size
     //1e9��Ϊ1*(10�ľŴη�),Ҳ����ʮ��
    val preprocessElapsed = (System.nanoTime() - preprocessStart) / 1e9

    println()
    println(s"Corpus summary:")
    println(s"\t Training set size: $actualCorpusSize documents")
    println(s"\t Vocabulary size: $actualVocabSize terms")
    println(s"\t Training set size: $actualNumTokens tokens")
    println(s"\t Preprocessing time: $preprocessElapsed sec")
    println()

    // Run LDA.
    val lda = new LDA()

    val optimizer = params.algorithm.toLowerCase match {
      case "em" => new EMLDAOptimizer
      // add (1.0 / actualCorpusSize) to MiniBatchFraction be more robust on tiny datasets.
      ////miniBatchFraction�Cÿһ�ֵ���,����ѵ������������,Ĭ��1.0(ȫ������)
      case "online" => new OnlineLDAOptimizer().setMiniBatchFraction(0.05 + 1.0 / actualCorpusSize)
      case _ => throw new IllegalArgumentException(
        s"Only em, online are supported but got ${params.algorithm}.")
    }

    lda.setOptimizer(optimizer)
      .setK(params.k)//���ƶϵ�����(��)����Ŀ
      .setMaxIterations(params.maxIterations)//��������
      .setDocConcentration(params.docConcentration)//�ĵ��������⣨"theta"��������ֲ����в���
      .setTopicConcentration(params.topicConcentration)//ÿ���ĵ��Ļ������ֲ����Ƶ������
      .setCheckpointInterval(params.checkpointInterval)//���ü�����
    if (params.checkpointDir.nonEmpty) {
      sc.setCheckpointDir(params.checkpointDir.get)
    }
     //ϵͳ��ʱ���ĵ�ǰֵ,�Ժ�΢��Ϊ��λ
    val startTime = System.nanoTime()
    val ldaModel = lda.run(corpus)
     //1e9��Ϊ1*(10�ľŴη�),Ҳ����ʮ��
    val elapsed = (System.nanoTime() - startTime) / 1e9
    //���ѵ��LDAģ��,�ܽ�
    println(s"Finished training LDA model.  Summary:")
    println(s"\t Training time: $elapsed sec")

    if (ldaModel.isInstanceOf[DistributedLDAModel]) {
      val distLDAModel = ldaModel.asInstanceOf[DistributedLDAModel]
      val avgLogLikelihood = distLDAModel.logLikelihood / actualCorpusSize.toDouble
      println(s"\t Training data average log likelihood: $avgLogLikelihood")
      println()
    }

    // Print the topics, showing the top-weighted terms for each topic.
    //��ӡ����,��ʾÿ������Ķ�����Ȩ��
    val topicIndices = ldaModel.describeTopics(maxTermsPerTopic = 10)
    val topics = topicIndices.map { case (terms, termWeights) =>
      terms.zip(termWeights).map { case (term, weight) => (vocabArray(term.toInt), weight) }
    }
    println(s"${params.k} topics:")
    topics.zipWithIndex.foreach { case (topic, i) =>
      println(s"TOPIC $i")
      topic.foreach { case (term, weight) =>
        println(s"$term\t$weight")
      }
      println()
    }
    sc.stop()
  }

  /**
   * Load documents, tokenize them, create vocabulary, and prepare documents as term count vectors.
   * �����ļ�,�������,����ʻ�,��׼���ļ���Ϊ���ڼ���ʸ��
   * @return (corpus, vocabulary as array, total token count in corpus)
   */
  private def preprocess(
      sc: SparkContext,
      paths: Seq[String],
      vocabSize: Int,
      stopwordFile: String): (RDD[(Long, Vector)], Array[String], Long) = {

    // Get dataset of document texts
    //��ȡ�ĵ��ı����ݼ�
    // One document per line in each text file. If the input consists of many small files,
    //ÿ���ı��ļ��е�ÿ��һ���ĵ�,������������С���ļ�
    // this can result in a large number of small partitions, which can degrade performance.
    //����ܻᵼ�´�����С����,�����Խ�������
    // In this case, consider using coalesce() to create fewer, larger partitions.
    //�����������,���Կ���ʹ��coalesce()�������,����ķ���
    val textRDD: RDD[String] = sc.textFile(paths.mkString(","))

    // Split text into words
    //���ı��ָ�ɵ���
    val tokenizer = new SimpleTokenizer(sc, stopwordFile)
    val tokenized: RDD[(Long, IndexedSeq[String])] = textRDD.zipWithIndex().map { case (text, id) =>
      id -> tokenizer.getWords(text)
    }
    tokenized.cache()

    // Counts words: RDD[(word, wordCount)]
    //���� ��
    val wordCounts: RDD[(String, Long)] = tokenized
      .flatMap { case (_, tokens) => tokens.map(_ -> 1L) }
      .reduceByKey(_ + _)
    wordCounts.cache()
    val fullVocabSize = wordCounts.count()
    // Select vocab
    //ѡ��ʻ�
    //  (vocab: Map[word -> id], total tokens after selecting vocab)
    val (vocab: Map[String, Int], selectedTokenCount: Long) = {
      val tmpSortedWC: Array[(String, Long)] = if (vocabSize == -1 || fullVocabSize <= vocabSize) {
        // Use all terms
        //ʹ�õ�������Ŀ
        wordCounts.collect().sortBy(-_._2)
      } else {
        // Sort terms to select vocab
        //����ѡ��ʻ�
        wordCounts.sortBy(_._2, ascending = false).take(vocabSize)
      }
      (tmpSortedWC.map(_._1).zipWithIndex.toMap, tmpSortedWC.map(_._2).sum)
    }

    val documents = tokenized.map { case (id, tokens) =>
      // Filter tokens by vocabulary, and create word count vector representation of document.
      //ͨ���ʻ����˱��,���������ʼ���������ʾ�ĵ�
      val wc = new mutable.HashMap[Int, Int]()
      tokens.foreach { term =>
        if (vocab.contains(term)) {
          val termIndex = vocab(term)
          wc(termIndex) = wc.getOrElse(termIndex, 0) + 1
        }
      }
      val indices = wc.keys.toArray.sorted
      val values = indices.map(i => wc(i).toDouble)

      val sb = Vectors.sparse(vocab.size, indices, values)
      (id, sb)
    }

    val vocabArray = new Array[String](vocab.size)
    vocab.foreach { case (term, i) => vocabArray(i) = term }

    (documents, vocabArray, selectedTokenCount)
  }
}

/**
 * Simple Tokenizer.
 *�򵥵ķֽ���
 * TODO: Formalize the interface, and make this a public class in mllib.feature
 * ��ʽ�Ľӿڣ�ʹmllib.feature������
 */
private class SimpleTokenizer(sc: SparkContext, stopwordFile: String) extends Serializable {

  private val stopwords: Set[String] = if (stopwordFile.isEmpty) {
    Set.empty[String]
  } else {
    val stopwordText = sc.textFile(stopwordFile).collect()
    stopwordText.flatMap(_.stripMargin.split("\\s+")).toSet
  }

  // Matches sequences of Unicode letters
  //ƥ��Unicode��ĸ����
  private val allWordRegex = "^(\\p{L}*)$".r

  // Ignore words shorter than this length.
  //���Ե��ʶ̵ĳ���
  private val minWordLength = 3

  def getWords(text: String): IndexedSeq[String] = {

    val words = new mutable.ArrayBuffer[String]()

    // Use Java BreakIterator to tokenize text into words.
    //ʹ��java�������з��ı��͵���
    val wb = BreakIterator.getWordInstance
    wb.setText(text)

    // current,end index start,end of each word
    //��ǰ,����������ʼ,ÿ�����ʵĽ�β
    var current = wb.first()
    var end = wb.next()
    while (end != BreakIterator.DONE) {
      // Convert to lowercase ת��Сд
      val word: String = text.substring(current, end).toLowerCase
      // Remove short words and strings that aren't only letters
      //ɾ���̵ĵ��ʺ��ַ�������������ĸ
      word match {
        case allWordRegex(w) if w.length >= minWordLength && !stopwords.contains(w) =>
          words += w
        case _ =>
      }

      current = end
      try {
        end = wb.next()
      } catch {
        case e: Exception =>
          // Ignore remaining text in line.�������е�ʣ���ı�
          // This is a known bug in BreakIterator (for some Java versions),
          //���Ǵ��Ƶ�����һ����֪��bug(һЩjava�汾)
          // which fails when it sees certain characters.
          //��������ĳЩ�ַ�ʱ,��ʧ����
          end = BreakIterator.DONE
      }
    }
    words
  }

}
// scalastyle:on println
