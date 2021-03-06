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

package org.apache.spark

import java.util.concurrent.Semaphore

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.future

import org.scalatest.BeforeAndAfter
import org.scalatest.Matchers

import org.apache.spark.scheduler.{SparkListener, SparkListenerTaskStart}

/**
 * Test suite for cancelling running jobs. We run the cancellation tasks for single job action
 * (e.g. count) as well as multi-job action (e.g. take). We test the local and cluster schedulers
 * in both FIFO and fair scheduling modes.
  * 取消运行作业,我们执行单作业操作的取消任务(例如计数),以及多工作行动(例如,获取take),我们测试的本地和集群调度程序FIFO和公平调度模式
 */
class JobCancellationSuite extends SparkFunSuite with Matchers with BeforeAndAfter
  with LocalSparkContext {

  override def afterEach() {
    super.afterEach()
    resetSparkContext()
  }

  test("local mode, FIFO scheduler") {//先进先出调度模式
  //Spark的任务调度模式,先进先出原则
    val conf = new SparkConf().set("spark.scheduler.mode", "FIFO")
    sc = new SparkContext("local[2]", "test", conf)
    testCount()
    testTake()
    // Make sure we can still launch tasks.
    //确保可以启动任务
    assert(sc.parallelize(1 to 10, 2).count === 10)
  }

  test("local mode, fair scheduler"){//公平模式调试
    //Spark的任务调度模式,公平调度
    val conf = new SparkConf().set("spark.scheduler.mode", "FAIR")
    val xmlPath = getClass.getClassLoader.getResource("fairscheduler.xml").getFile()
    conf.set("spark.scheduler.allocation.file", xmlPath)
    sc = new SparkContext("local[2]", "test", conf)
    testCount()
    testTake()
    // Make sure we can still launch tasks.
    //确保可以启动任务
    assert(sc.parallelize(1 to 10, 2).count === 10)
  }
  //Spark的任务调度模式,先进先出原则

  test("cluster mode, FIFO scheduler") {
    val conf = new SparkConf().set("spark.scheduler.mode", "FIFO")
    sc = new SparkContext("local-cluster[2,1,1024]", "test", conf)
    testCount()
    testTake()
    // Make sure we can still launch tasks.
    //确保可以启动任务
    assert(sc.parallelize(1 to 10, 2).count === 10)
  }
  //SparkContext对job进行调度所采用的模式

  test("cluster mode, fair scheduler") {
    val conf = new SparkConf().set("spark.scheduler.mode", "FAIR")//FAIR 公平
    val xmlPath = getClass.getClassLoader.getResource("fairscheduler.xml").getFile()
    conf.set("spark.scheduler.allocation.file", xmlPath)
    sc = new SparkContext("local-cluster[2,1,1024]", "test", conf)
    testCount()
    testTake()
    // Make sure we can still launch tasks.
    //确保可以启动任务
    assert(sc.parallelize(1 to 10, 2).count === 10)
  }

  test("do not put partially executed partitions into cache") {//不要将部分执行的分区放在缓存中
    // In this test case, we create a scenario in which a partition is only partially executed,
    // and make sure CacheManager does not put that partially executed partition into the
    // BlockManager.
    //在这个测试用例中,创建一个场景,其中一个分区只执行了一个分区,确保缓存管理器不把部分执行分区
    import JobCancellationSuite._
    sc = new SparkContext("local", "test")

    // Run from 1 to 10, and then block and wait for the task to be killed.
    //从1运行到10,然后阻止和等待任务被杀死
    val rdd = sc.parallelize(1 to 1000, 2).map { x =>
      if (x > 10) {
        taskStartedSemaphore.release()//释放资源
        taskCancelledSemaphore.acquire()//获取资源
      }
      x
    }.cache()

    val rdd1 = rdd.map(x => x)

    future {//Future 表示一个可能还没有实际完成的异步任务的结果
      taskStartedSemaphore.acquire()
      sc.cancelAllJobs()
      taskCancelledSemaphore.release(100000)
    }

    intercept[SparkException] { rdd1.count() }
    // If the partial block is put into cache, rdd.count() would return a number less than 1000.
    //如果部分块放入缓存,RDD.count()将返回一个数小于1000。
    assert(rdd.count() === 1000)
  }

  test("job group") {//工作分组
    sc = new SparkContext("local[2]", "test")

    // Add a listener to release the semaphore once any tasks are launched.
    //添加一个监听器一旦启动任务来释放信号
    val sem = new Semaphore(0)
    sc.addSparkListener(new SparkListener {
      override def onTaskStart(taskStart: SparkListenerTaskStart) {
        sem.release()
      }
    })

    // jobA is the one to be cancelled.
    //工作A是被取消的一个
    val jobA = future {//Future 表示一个可能还没有实际完成的异步任务的结果,
      sc.setJobGroup("jobA", "this is a job to be cancelled")
      sc.parallelize(1 to 10000, 2).map { i => Thread.sleep(10); i }.count()
    }

    // Block until both tasks of job A have started and cancel job A.
    //阻止工作A的两个任务都已经开始和取消工作A
    sem.acquire(2)

    sc.clearJobGroup()
    val jobB = sc.parallelize(1 to 100, 2).countAsync()
    sc.cancelJobGroup("jobA")
    //Await.result或者Await.ready会导致当前线程被阻塞,并等待actor通过它的应答来完成Future
    val e = intercept[SparkException] { Await.result(jobA, Duration.Inf) }
    assert(e.getMessage contains "cancel")

    // Once A is cancelled, job B should finish fairly quickly.
    //一旦A被取消,工作B应该很快完成
    assert(jobB.get() === 100)
  }

  test("inherited job group (SPARK-6629)") {//继承工作组
    sc = new SparkContext("local[2]", "test")

    // Add a listener to release the semaphore once any tasks are launched.
    //添加一个监听器一旦启动任务释放信号量
    val sem = new Semaphore(0)
    sc.addSparkListener(new SparkListener {
      override def onTaskStart(taskStart: SparkListenerTaskStart) {
        sem.release()
      }
    })

    sc.setJobGroup("jobA", "this is a job to be cancelled")
    @volatile var exception: Exception = null
    val jobA = new Thread() {
      // The job group should be inherited by this thread
      //工作组应该被这个线程继承
      override def run(): Unit = {
        exception = intercept[SparkException] {
          sc.parallelize(1 to 10000, 2).map { i => Thread.sleep(10); i }.count()
        }
      }
    }
    jobA.start()

    // Block until both tasks of job A have started and cancel job A.
    //阻止工作A的两个任务都已经开始和取消工作A
    sem.acquire(2)
    sc.cancelJobGroup("jobA")
    jobA.join(10000)
    assert(!jobA.isAlive)
    assert(exception.getMessage contains "cancel")

    // Once A is cancelled, job B should finish fairly quickly.
    //一旦A被取消,工作B应该很快完成。
    val jobB = sc.parallelize(1 to 100, 2).countAsync()
    assert(jobB.get() === 100)
  }

  test("job group with interruption") {
    sc = new SparkContext("local[2]", "test")

    // Add a listener to release the semaphore once any tasks are launched.
    val sem = new Semaphore(0)
    sc.addSparkListener(new SparkListener {
      override def onTaskStart(taskStart: SparkListenerTaskStart) {
        sem.release()
      }
    })

    // jobA is the one to be cancelled.
    //工作A是被取消的一个
    val jobA = future {
      sc.setJobGroup("jobA", "this is a job to be cancelled", interruptOnCancel = true)
      sc.parallelize(1 to 10000, 2).map { i => Thread.sleep(100000); i }.count()
    }

    // Block until both tasks of job A have started and cancel job A.
    sem.acquire(2)

    sc.clearJobGroup()
    val jobB = sc.parallelize(1 to 100, 2).countAsync()
    sc.cancelJobGroup("jobA")
    //Await.result或者Await.ready会导致当前线程被阻塞,并等待actor通过它的应答来完成Future
    val e = intercept[SparkException] { Await.result(jobA, 5.seconds) }
    assert(e.getMessage contains "cancel")

    // Once A is cancelled, job B should finish fairly quickly.
    assert(jobB.get() === 100)
  }

  test("two jobs sharing the same stage") {//两个共享同一阶段的Job
    // sem1: make sure cancel is issued after some tasks are launched
    //在启动某些任务后,请确认取消是发出的
    // twoJobsSharingStageSemaphore:
    //   make sure the first stage is not finished until cancel is issued
    //确保第一阶段没有完成,直到取消发行
    val sem1 = new Semaphore(0)

    sc = new SparkContext("local[2]", "test")
    sc.addSparkListener(new SparkListener {
      override def onTaskStart(taskStart: SparkListenerTaskStart) {
        sem1.release()
      }
    })

    // Create two actions that would share the some stages.
    //创建两个将共享一些阶段的操作
    val rdd = sc.parallelize(1 to 10, 2).map { i =>
      JobCancellationSuite.twoJobsSharingStageSemaphore.acquire()
      (i, i)
    }.reduceByKey(_ + _)
    val f1 = rdd.collectAsync()
    val f2 = rdd.countAsync()

    // Kill one of the action.
    // 杀死一个动作
    future {
      sem1.acquire()
      f1.cancel()
      JobCancellationSuite.twoJobsSharingStageSemaphore.release(10)
    }

    // Expect f1 to fail due to cancellation,
    //希望F1由于取消失败
    intercept[SparkException] { f1.get() }
    // but f2 should not be affected
    //但F2应不受影响
    f2.get()
  }

  def testCount() {
    // Cancel before launching any tasks
    //取消之前启动任何任务
    {
      val f = sc.parallelize(1 to 10000, 2).map { i => Thread.sleep(10); i }.countAsync()
      future { f.cancel() }
      val e = intercept[SparkException] { f.get() }
      assert(e.getMessage.contains("cancelled") || e.getMessage.contains("killed"))
    }

    // Cancel after some tasks have been launched
    //取消一些任务后已经启动
    {
      // Add a listener to release the semaphore once any tasks are launched.
      //添加一个监听器来释放信号量一旦启动任务
      val sem = new Semaphore(0)
      sc.addSparkListener(new SparkListener {
        override def onTaskStart(taskStart: SparkListenerTaskStart) {
          println("testCount:"+taskStart.stageId+"==="+taskStart.taskInfo.id+"==executorId=="+taskStart.taskInfo.executorId+"=="
            +taskStart.taskInfo.host+"=="+taskStart.taskInfo.taskLocality+"==="+taskStart.taskInfo.status+"=="+
            taskStart.taskInfo.gettingResult+"=="+taskStart.taskInfo.successful+"=="+taskStart.taskInfo.launchTime)
          sem.release()
        }
      })

      val f = sc.parallelize(1 to 10000, 2).map { i => Thread.sleep(10); i }.countAsync()
      future {
        // Wait until some tasks were launched before we cancel the job.
        //等到一些任务开始前,我们取消了工作
        sem.acquire()
        f.cancel()
      }
      val e = intercept[SparkException] { f.get() }
      assert(e.getMessage.contains("cancelled") || e.getMessage.contains("killed"))
    }
  }

  def testTake() {
    // Cancel before launching any tasks
    //取消之前启动任何任务
    {
      val f = sc.parallelize(1 to 10000, 2).map { i => Thread.sleep(10); i }.takeAsync(5000)
      future { f.cancel() }
      val e = intercept[SparkException] { f.get() }
      assert(e.getMessage.contains("cancelled") || e.getMessage.contains("killed"))
    }

    // Cancel after some tasks have been launched
    //取消一些任务后已经启动
    {
      // Add a listener to release the semaphore once any tasks are launched.
      val sem = new Semaphore(0)
      sc.addSparkListener(new SparkListener {
        override def onTaskStart(taskStart: SparkListenerTaskStart) {
          println("testTake:"+taskStart.stageId+"===="+taskStart.taskInfo)
          sem.release()
        }
      })
      val f = sc.parallelize(1 to 10000, 2).map { i => Thread.sleep(10); i }.takeAsync(5000)
      future {
        sem.acquire()
        f.cancel()
      }
      val e = intercept[SparkException] { f.get() }
      assert(e.getMessage.contains("cancelled") || e.getMessage.contains("killed"))
    }
  }
}


object JobCancellationSuite {
  val taskStartedSemaphore = new Semaphore(0)
  val taskCancelledSemaphore = new Semaphore(0)
  val twoJobsSharingStageSemaphore = new Semaphore(0)
}
