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

package org.apache.spark.deploy.master.ui

import javax.servlet.http.HttpServletRequest

import scala.xml.Node

import org.apache.spark.deploy.ExecutorState
import org.apache.spark.deploy.DeployMessages.{MasterStateResponse, RequestMasterState}
import org.apache.spark.deploy.master.ExecutorDesc
import org.apache.spark.ui.{UIUtils, WebUIPage}
import org.apache.spark.util.Utils

private[ui] class ApplicationPage(parent: MasterWebUI) extends WebUIPage("app") {

  private val master = parent.masterEndpointRef

  /** 
   *  Executor details for a particular application 
   *  执行详细特定应用程序
   *  */
  def render(request: HttpServletRequest): Seq[Node] = {
    val appId = request.getParameter("appId")
    val state = master.askWithRetry[MasterStateResponse](RequestMasterState)
    val app = state.activeApps.find(_.id == appId).getOrElse({
      state.completedApps.find(_.id == appId).getOrElse(null)
    })
    if (app == null) {
      val msg = <div class="row-fluid">No running application with ID {appId}</div>
      return UIUtils.basicSparkPage(msg, "Not Found")
    }

    val executorHeaders = Seq("ExecutorID", "Worker", "Cores", "Memory", "State", "Logs")
    val allExecutors = (app.executors.values ++ app.removedExecutors).toSet.toSeq
    // This includes executors that are either still running or have exited cleanly
    //这包括仍在运行或已经完全退出的执行者
    val executors = allExecutors.filter { exec =>
      !ExecutorState.isFinished(exec.state) || exec.state == ExecutorState.EXITED
    }
    val removedExecutors = allExecutors.diff(executors)
    val executorsTable = UIUtils.listingTable(executorHeaders, executorRow, executors)
    val removedExecutorsTable = UIUtils.listingTable(executorHeaders, executorRow, removedExecutors)

    val content =
      <div class="row-fluid">
        <div class="span12">
          <ul class="unstyled">
            <li><strong>ID:</strong> {app.id}</li>
            <li><strong>Name:</strong> {app.desc.name}</li>
            <li><strong>User:</strong> {app.desc.user}</li>
            <li><strong>Cores:</strong>
            {
              if (app.desc.maxCores.isEmpty) {
                "Unlimited (%s granted)".format(app.coresGranted)
              } else {
                "%s (%s granted, %s left)".format(
                  app.desc.maxCores.get, app.coresGranted, app.coresLeft)
              }
            }
            </li>
            <li>
              <strong>Executor Memory:</strong>
              {Utils.megabytesToString(app.desc.memoryPerExecutorMB)}
            </li>
            <li><strong>Submit Date:</strong> {app.submitDate}</li>
            <li><strong>State:</strong> {app.state}</li>
            <li><strong><a href={app.desc.appUiUrl}>Application Detail UI</a></strong></li>
          </ul>
        </div>
      </div>

      <div class="row-fluid"> <!-- Executors -->
        <div class="span12">
          <h4> Executor Summary </h4>
          {executorsTable}
          {
            if (removedExecutors.nonEmpty) {
              <h4> Removed Executors </h4> ++
              removedExecutorsTable
            }
          }
        </div>
      </div>;
    UIUtils.basicSparkPage(content, "Application: " + app.desc.name)
  }

  private def executorRow(executor: ExecutorDesc): Seq[Node] = {
    <tr>
      <td>{executor.id}</td>
      <td>
        <a href={executor.worker.webUiAddress}>{executor.worker.id}</a>
      </td>
      <td>{executor.cores}</td>
      <td>{executor.memory}</td>
      <td>{executor.state}</td>
      <td>
        <a href={"%s/logPage?appId=%s&executorId=%s&logType=stdout"
          .format(executor.worker.webUiAddress, executor.application.id, executor.id)}>stdout</a>
        <a href={"%s/logPage?appId=%s&executorId=%s&logType=stderr"
          .format(executor.worker.webUiAddress, executor.application.id, executor.id)}>stderr</a>
      </td>
    </tr>
  }
}
