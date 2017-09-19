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

package org.apache.spark;

import java.io.Serializable;

/**
 * Exposes information about Spark Stages.
 *
 * This interface is not designed to be implemented outside of Spark.  We may add additional methods
 * which may break binary compatibility with outside implementations.
 * 此接口不是设计为在Spark之外实现,我们可能会添加其他可能会破坏外部实现的二进制兼容性的方法
 */
public interface SparkStageInfo extends Serializable {
  int stageId();
  int currentAttemptId();
  long submissionTime();
  String name();
  int numTasks();
  int numActiveTasks();
  int numCompletedTasks();
  int numFailedTasks();
}
