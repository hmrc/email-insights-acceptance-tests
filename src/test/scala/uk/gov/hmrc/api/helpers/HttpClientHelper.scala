/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.api.helpers

import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.StandaloneWSResponse
import uk.gov.hmrc.api.conf.TestEnvironment
import uk.gov.hmrc.api.utils.Logging
import uk.gov.hmrc.apitestrunner.http.HttpClient

import scala.concurrent.Future

trait HttpClientHelper extends HttpClient with Logging {

  val baseUrl: String         = TestEnvironment.url("email-gateway")
  val testOnlyBaseUrl: String = TestEnvironment.url("email-insights-proxy")

  def headers: Seq[(String, String)] =
    Seq(
      "X-Correlation-ID" -> "acceptance-tests"
    )

  def get(url: String, headers: (String, String)*): Future[StandaloneWSResponse] = {
    val allHeaders = this.headers ++ headers
    mkRequest(url)
      .withHttpHeaders(allHeaders: _*)
      .withBody(Json.parse("{}"))
      .get()
  }

  def delete(url: String, headers: (String, String)*): Future[StandaloneWSResponse] = {
    val allHeaders = this.headers ++ headers
    mkRequest(url)
      .withHttpHeaders(allHeaders: _*)
      .withBody(Json.parse("""{}"""))
      .delete()
  }

  def post(url: String, body: String, headers: (String, String)*): Future[StandaloneWSResponse] = {
    val allHeaders = this.headers ++ headers
    mkRequest(url)
      .withHttpHeaders(allHeaders: _*)
      .post(Json.parse(body))
  }

  def invalidPostRequest(url: String, body: String, headers: (String, String)*): Future[StandaloneWSResponse] = {
    val allHeaders = this.headers ++ headers
    mkRequest(url)
      .withHttpHeaders(allHeaders: _*)
      .post(Json.parse(body))
  }
}
