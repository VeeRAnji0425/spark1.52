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

package org.apache.spark.streaming

import java.io._
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.conf.Configuration

import org.apache.spark.{SparkException, SparkConf, Logging}
import org.apache.spark.deploy.SparkHadoopUtil
import org.apache.spark.io.CompressionCodec
import org.apache.spark.util.{MetadataCleaner, Utils}
import org.apache.spark.streaming.scheduler.JobGenerator


private[streaming]
class Checkpoint(@transient ssc: StreamingContext, val checkpointTime: Time)
  extends Logging with Serializable {
  val master = ssc.sc.master
  val framework = ssc.sc.appName
  val jars = ssc.sc.jars
  val graph = ssc.graph
  val checkpointDir = ssc.checkpointDir
  val checkpointDuration = ssc.checkpointDuration
  val pendingTimes = ssc.scheduler.getPendingTimes().toArray
  val delaySeconds = MetadataCleaner.getDelaySeconds(ssc.conf)
  //返回所有配置项属性
  val sparkConfPairs = ssc.conf.getAll

  def createSparkConf(): SparkConf = {

    // Reload properties for the checkpoint application since user wants to set a reload property
    // or spark had changed its value and user wants to set it back.
    //对于检查点应用程序,因为用户希望设置重载属性,或Spark改变想使用的属性值
    val propertiesToReload = List(
      "spark.yarn.app.id",
      "spark.yarn.app.attemptId",
      "spark.driver.host",
      "spark.driver.port",
      "spark.master",
      "spark.yarn.keytab",
      "spark.yarn.principal")

    val newSparkConf = new SparkConf(loadDefaults = false).setAll(sparkConfPairs)
      .remove("spark.driver.host")
      .remove("spark.driver.port")
    val newReloadConf = new SparkConf(loadDefaults = true)
    propertiesToReload.foreach { prop =>
      newReloadConf.getOption(prop).foreach { value =>
        newSparkConf.set(prop, value)
      }
    }
    newSparkConf
  }

  def validate() {
    assert(master != null, "Checkpoint.master is null")
    assert(framework != null, "Checkpoint.framework is null")
    assert(graph != null, "Checkpoint.graph is null")
    assert(checkpointTime != null, "Checkpoint.checkpointTime is null")
    logInfo("Checkpoint for time " + checkpointTime + " validated")
  }
}

private[streaming]
object Checkpoint extends Logging {
  val PREFIX = "checkpoint-"
  val REGEX = (PREFIX + """([\d]+)([\w\.]*)""").r

  /** 
   *  Get the checkpoint file for the given checkpoint time
   *  获取给定检查点时间的检查点文件 
   *  */
  def checkpointFile(checkpointDir: String, checkpointTime: Time): Path = {
    new Path(checkpointDir, PREFIX + checkpointTime.milliseconds)
  }

  /** 
   *  Get the checkpoint backup file for the given checkpoint time
   *  获取给定检查点时间的检查点备份文件 
   *  */
  def checkpointBackupFile(checkpointDir: String, checkpointTime: Time): Path = {
    new Path(checkpointDir, PREFIX + checkpointTime.milliseconds + ".bk")
  }

  /** 
   *  Get checkpoint files present in the give directory, ordered by oldest-first 
   *  获取给目录中的检查点文件,排序旧的第一个
   *  */
  def getCheckpointFiles(checkpointDir: String, fsOption: Option[FileSystem] = None): Seq[Path] = {

    def sortFunc(path1: Path, path2: Path): Boolean = {
      val (time1, bk1) = path1.getName match { case REGEX(x, y) => (x.toLong, !y.isEmpty) }
      val (time2, bk2) = path2.getName match { case REGEX(x, y) => (x.toLong, !y.isEmpty) }
      (time1 < time2) || (time1 == time2 && bk1)
    }

    val path = new Path(checkpointDir)
    val fs = fsOption.getOrElse(path.getFileSystem(SparkHadoopUtil.get.conf))
    if (fs.exists(path)) {
      val statuses = fs.listStatus(path)
      if (statuses != null) {
        val paths = statuses.map(_.getPath)
        val filtered = paths.filter(p => REGEX.findFirstIn(p.toString).nonEmpty)
        filtered.sortWith(sortFunc)
      } else {
        logWarning("Listing " + path + " returned null")
        Seq.empty
      }
    } else {
      logInfo("Checkpoint directory " + path + " does not exist")
      Seq.empty
    }
  }

  /** 
   *  Serialize the checkpoint, or throw any exception that occurs 
   *  序列化的检查点,或将出现的任何异常
   *  */
  def serialize(checkpoint: Checkpoint, conf: SparkConf): Array[Byte] = {
    val compressionCodec = CompressionCodec.createCodec(conf)
    val bos = new ByteArrayOutputStream()
    val zos = compressionCodec.compressedOutputStream(bos)
    val oos = new ObjectOutputStream(zos)
    Utils.tryWithSafeFinally {
      oos.writeObject(checkpoint)
    } {
      oos.close()
    }
    bos.toByteArray
  }

  /** 
   *  Deserialize a checkpoint from the input stream, or throw any exception that occurs
   *  从一个输入流的反序列化一个检查点,或抛出任何异常
   *   */
  def deserialize(inputStream: InputStream, conf: SparkConf): Checkpoint = {
    val compressionCodec = CompressionCodec.createCodec(conf)
    var ois: ObjectInputStreamWithLoader = null
    Utils.tryWithSafeFinally {

      // ObjectInputStream uses the last defined user-defined class loader in the stack
      // to find classes, which maybe the wrong class loader. Hence, a inherited version
      // 对象输入流使用堆栈中的最后定义自定义类查找类加载器,这可能是错误的类装载器
      // of ObjectInputStream is used to explicitly use the current thread's default class
      // loader to find and load classes. This is a well know Java issue and has popped up
      //一个继承对象输入流用于显式地使用当前线程的默认类查找加载器
      // in other places (e.g., http://jira.codehaus.org/browse/GROOVY-1627)
      //这是一个众所周知的java的问题,已经出现在其他地方
      val zis = compressionCodec.compressedInputStream(inputStream)
      ois = new ObjectInputStreamWithLoader(zis,
        //Thread.currentThread().getContextClassLoader,可以获取当前线程的引用,getContextClassLoader用来获取线程的上下文类加载器
        Thread.currentThread().getContextClassLoader)
      val cp = ois.readObject.asInstanceOf[Checkpoint]
      cp.validate()
      cp
    } {
      if (ois != null) {
        ois.close()
      }
    }
  }
}


/**
 * Convenience class to handle the writing of graph checkpoint to file
 * 检查点的写入文件
 */
private[streaming]
class CheckpointWriter(
    jobGenerator: JobGenerator,
    conf: SparkConf,
    checkpointDir: String,
    hadoopConf: Configuration
  ) extends Logging {
  val MAX_ATTEMPTS = 3
  //用于保存等待执行的任务的阻塞队列,
  //LinkedBlockingQueue：一个基于链表结构的阻塞队列，此队列按FIFO(先进先出)排序元素，吞吐量通常要高于ArrayBlockingQueue
  //Executors.newFixedThreadPool()使用了这个队列
  val executor = Executors.newFixedThreadPool(1)
  val compressionCodec = CompressionCodec.createCodec(conf)
  private var stopped = false
  private var fs_ : FileSystem = _

  @volatile private var latestCheckpointTime: Time = null

  class CheckpointWriteHandler(
      checkpointTime: Time,
      bytes: Array[Byte],
      clearCheckpointDataLater: Boolean) extends Runnable {
    def run() {
      if (latestCheckpointTime == null || latestCheckpointTime < checkpointTime) {
        latestCheckpointTime = checkpointTime
      }
      var attempts = 0
      val startTime = System.currentTimeMillis()
      val tempFile = new Path(checkpointDir, "temp")
      // We will do checkpoint when generating a batch and completing a batch. When the processing
      // time of a batch is greater than the batch interval, checkpointing for completing an old
      // batch may run after checkpointing of a new batch. If this happens, checkpoint of an old
      // batch actually has the latest information, so we want to recovery from it. Therefore, we
      // also use the latest checkpoint time as the file name, so that we can recovery from the
      // latest checkpoint file.
      //
      // Note: there is only one thread writting the checkpoint files, so we don't need to worry
      // about thread-safety.
      val checkpointFile = Checkpoint.checkpointFile(checkpointDir, latestCheckpointTime)
      val backupFile = Checkpoint.checkpointBackupFile(checkpointDir, latestCheckpointTime)

      while (attempts < MAX_ATTEMPTS && !stopped) {
        attempts += 1
        try {
          logInfo("Saving checkpoint for time " + checkpointTime + " to file '" + checkpointFile
            + "'")

          // Write checkpoint to temp file
          //写入临时文件的检查点
          if (fs.exists(tempFile)) {
            fs.delete(tempFile, true)   // just in case it exists以防万一它存在
          }
          val fos = fs.create(tempFile)
          Utils.tryWithSafeFinally {
            fos.write(bytes)
          } {
            fos.close()
          }

          // If the checkpoint file exists, back it up
          // If the backup exists as well, just delete it, otherwise rename will fail
          //如果检查点文件存在,则备份,如果备份也存在,只需删除它,否则重命名将失败
          if (fs.exists(checkpointFile)) {
            if (fs.exists(backupFile)){
              fs.delete(backupFile, true) // just in case it exists
            }
            if (!fs.rename(checkpointFile, backupFile)) {
              logWarning("Could not rename " + checkpointFile + " to " + backupFile)
            }
          }

          // Rename temp file to the final checkpoint file
          //将临时文件重命名为最终的检查点文件
          if (!fs.rename(tempFile, checkpointFile)) {
            logWarning("Could not rename " + tempFile + " to " + checkpointFile)
          }

          // Delete old checkpoint files
          //删除检查点文件
          val allCheckpointFiles = Checkpoint.getCheckpointFiles(checkpointDir, Some(fs))
          if (allCheckpointFiles.size > 10) {
            allCheckpointFiles.take(allCheckpointFiles.size - 10).foreach(file => {
              logInfo("Deleting " + file)
              fs.delete(file, true)
            })
          }

          // All done, print success打印成功
          val finishTime = System.currentTimeMillis()
          logInfo("Checkpoint for time " + checkpointTime + " saved to file '" + checkpointFile +
            "', took " + bytes.length + " bytes and " + (finishTime - startTime) + " ms")
          jobGenerator.onCheckpointCompletion(checkpointTime, clearCheckpointDataLater)
          return
        } catch {
          case ioe: IOException =>
            logWarning("Error in attempt " + attempts + " of writing checkpoint to "
              + checkpointFile, ioe)
            reset()
        }
      }
      logWarning("Could not write checkpoint for time " + checkpointTime + " to file "
        + checkpointFile + "'")
    }
  }

  def write(checkpoint: Checkpoint, clearCheckpointDataLater: Boolean) {
    try {
      val bytes = Checkpoint.serialize(checkpoint, conf)
      executor.execute(new CheckpointWriteHandler(
        checkpoint.checkpointTime, bytes, clearCheckpointDataLater))
      logDebug("Submitted checkpoint of time " + checkpoint.checkpointTime + " writer queue")
    } catch {
      case rej: RejectedExecutionException =>
        logError("Could not submit checkpoint task to the thread pool executor", rej)
    }
  }

  def stop(): Unit = synchronized {
    if (stopped) return

    executor.shutdown()
    val startTime = System.currentTimeMillis()
    val terminated = executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)
    if (!terminated) {
      executor.shutdownNow()
    }
    val endTime = System.currentTimeMillis()
    logInfo("CheckpointWriter executor terminated ? " + terminated +
      ", waited for " + (endTime - startTime) + " ms.")
    stopped = true
  }

  private def fs = synchronized {
    if (fs_ == null) fs_ = new Path(checkpointDir).getFileSystem(hadoopConf)
    fs_
  }

  private def reset() = synchronized {
    fs_ = null
  }
}


private[streaming]
object CheckpointReader extends Logging {

  /**
   * Read checkpoint files present in the given checkpoint directory. If there are no checkpoint
   * files, then return None, else try to return the latest valid checkpoint object. If no
   * checkpoint files could be read correctly, then return None.
   * 读取给定检查点目录中的检查点文件,如果没有检查点文件,则返回无检查点文件
   */
  def read(checkpointDir: String): Option[Checkpoint] = {
    read(checkpointDir, new SparkConf(), SparkHadoopUtil.get.conf, ignoreReadError = true)
  }

  /**
   * Read checkpoint files present in the given checkpoint directory. If there are no checkpoint
   * files, then return None, else try to return the latest valid checkpoint object. If no
   * checkpoint files could be read correctly, then return None (if ignoreReadError = true),
   * or throw exception (if ignoreReadError = false).
   * 读取给定检查点目录中的检查点文件,如果没有检查点文件,则返回无检查点文件,否则尝试返回最新的有效的检查点对象
   * 如果没有检查点文件,可以正确读取
   */
  def read(
      checkpointDir: String,
      conf: SparkConf,
      hadoopConf: Configuration,
      ignoreReadError: Boolean = false): Option[Checkpoint] = {
    val checkpointPath = new Path(checkpointDir)

    // TODO(rxin): Why is this a def?!
    def fs: FileSystem = checkpointPath.getFileSystem(hadoopConf)

    // Try to find the checkpoint files
    //尝试找到检查点文件
    val checkpointFiles = Checkpoint.getCheckpointFiles(checkpointDir, Some(fs)).reverse
    if (checkpointFiles.isEmpty) {
      return None
    }

    // Try to read the checkpoint files in the order
    //尝试读取命令中的检查点文件
    logInfo("Checkpoint files found: " + checkpointFiles.mkString(","))
    var readError: Exception = null
    checkpointFiles.foreach(file => {
      logInfo("Attempting to load checkpoint from file " + file)
      try {
        val fis = fs.open(file)
        val cp = Checkpoint.deserialize(fis, conf)
        logInfo("Checkpoint successfully loaded from file " + file)
        logInfo("Checkpoint was generated at time " + cp.checkpointTime)
        return Some(cp)
      } catch {
        case e: Exception =>
          readError = e
          logWarning("Error reading checkpoint from file " + file, e)
      }
    })

    // If none of checkpoint files could be read, then throw exception
    //如果没有一个检查点文件可以读取,则抛出异常
    if (!ignoreReadError) {
      throw new SparkException(
        s"Failed to read checkpoint from directory $checkpointPath", readError)
    }
    None
  }
}

private[streaming]
class ObjectInputStreamWithLoader(inputStream_ : InputStream, loader: ClassLoader)
  extends ObjectInputStream(inputStream_) {

  override def resolveClass(desc: ObjectStreamClass): Class[_] = {
    try {
      return loader.loadClass(desc.getName())
    } catch {
      case e: Exception =>
    }
    super.resolveClass(desc)
  }
}
