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

package org.apache.spark.streaming.scheduler

import scala.collection.mutable.HashSet
import scala.util.Failure

import org.apache.spark.streaming.Time
import org.apache.spark.util.Utils

/** Class representing a set of Jobs
  * belong to the same batch.
  * 表示属于同一批的一组作业的
  */
private[streaming]
case class JobSet(
    time: Time,
    jobs: Seq[Job],
    streamIdToInputInfo: Map[Int, StreamInputInfo] = Map.empty) {

  private val incompleteJobs = new HashSet[Job]()
  //当这jobset提交
  private val submissionTime = System.currentTimeMillis() // when this jobset was submitted
  //当这jobset第一份工作开始处理
  private var processingStartTime = -1L // when the first job of this jobset started processing
  //当这jobset最后的工作处理完
  private var processingEndTime = -1L // when the last job of this jobset finished processing

  jobs.zipWithIndex.foreach { case (job, i) => job.setOutputOpId(i) }
  incompleteJobs ++= jobs

  def handleJobStart(job: Job) {
    if (processingStartTime < 0) processingStartTime = System.currentTimeMillis()
  }

  def handleJobCompletion(job: Job) {
    incompleteJobs -= job
    if (hasCompleted) processingEndTime = System.currentTimeMillis()
  }

  def hasStarted: Boolean = processingStartTime > 0

  def hasCompleted: Boolean = incompleteJobs.isEmpty

  // Time taken to process all the jobs from the time they started processing
  //从他们开始处理的时间来处理所有的工作
  // (i.e. not including the time they wait in the streaming scheduler queue)
  def processingDelay: Long = processingEndTime - processingStartTime

  // Time taken to process all the jobs from the time they were submitted
  //从提交的时间来处理所有的工作时间
  // (i.e. including the time they wait in the streaming scheduler queue)
  def totalDelay: Long = {
    processingEndTime - time.milliseconds
  }

  def toBatchInfo: BatchInfo = {
    val failureReasons: Map[Int, String] = {
      if (hasCompleted) {
        jobs.filter(_.result.isFailure).map { job =>
          (job.outputOpId, Utils.exceptionString(job.result.asInstanceOf[Failure[_]].exception))
        }.toMap
      } else {
        Map.empty
      }
    }
    val binfo = new BatchInfo(
      time,
      streamIdToInputInfo,
      submissionTime,
      if (processingStartTime >= 0) Some(processingStartTime) else None,
      if (processingEndTime >= 0) Some(processingEndTime) else None
    )
    binfo.setFailureReason(failureReasons)
    binfo.setNumOutputOp(jobs.size)
    binfo
  }
}
