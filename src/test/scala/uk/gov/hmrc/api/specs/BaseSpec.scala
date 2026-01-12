/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.api.specs

import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, GivenWhenThen}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.StandaloneWSResponse
import uk.gov.hmrc.api.helpers.HttpClientHelper

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait BaseSpec extends AnyFeatureSpec with GivenWhenThen with Matchers with HttpClientHelper {

  val testOnlyEndpoint           = s"$testOnlyBaseUrl/test-only/watchlist/data"
  val testOnlyEndpointDeleteData = s"$testOnlyEndpoint/delete"
  val testOnlyEndpointCreateData = s"$testOnlyEndpoint/create"
  val testOnlyEndpointCounts     = s"$testOnlyEndpoint/counts"
  val checkInsightsEndpoint      = s"$baseUrl/check/insights"

  def createWatchlistData(numberOfGeneratedEmails: Int, manualEmailAddresses: String): Unit = {
    val request =
      s"""{
         |  "generatedEntries":{
         |    "numberOfEntries": $numberOfGeneratedEmails
         |   },
         |  "manualEntries":{
         |    "emailAddresses": ["$manualEmailAddresses"]
         |   }
         |}""".stripMargin

    val createEmailInsightsTestOnlyData: StandaloneWSResponse =
      Await.result(
        post(testOnlyEndpointCreateData, request),
        10.seconds
      )
    val responseBody                                          = createEmailInsightsTestOnlyData.body
    responseBody should include regex "Created \\d+ watchlist email addresses"
    assert(createEmailInsightsTestOnlyData.status == 200)
  }

  def getWatchlistData: Seq[String] = {
    val response = Await.result(
      get(testOnlyEndpoint),
      10.seconds
    )
    val body     = if (response.status == 200 && response.body.trim.nonEmpty) response.body else "{}"
    val json     = Json.parse(body)
    (json \\ "emailsOnWatchlistEntries").headOption
      .flatMap(_.asOpt[Seq[String]])
      .getOrElse(Seq.empty)
  }

  def getWatchlistDataCount: Int = {
    val response = Await.result(
      get(testOnlyEndpoint),
      10.seconds
    )
    val body     = if (response.status == 200 && response.body.trim.nonEmpty) response.body else "{}"
    val json     = Json.parse(body)
    (json \ "watchlistEmailCount").asOpt[Int].getOrElse(0)
  }

  def clearWatchlistData(): Assertion = {
    val clearDataFromEndpoint =
      Await.result(delete(testOnlyEndpointDeleteData), 10.seconds)
    val responseBody          = clearDataFromEndpoint.body
    responseBody should include regex "Deleted \\d+ watchlist email addresses"
  }

  def postCheckInsightsRequest(emailAddress: String): StandaloneWSResponse = {
    val request =
      s"""{
         |"email": "$emailAddress"
         |}""".stripMargin

    val response: StandaloneWSResponse =
      Await.result(
        postWithAuth(checkInsightsEndpoint, request),
        10.seconds
      )
    response
  }

  def postInvalidPayloadRequest(payload: String): StandaloneWSResponse = {
    val invalidPayload = payload
    val response       = Await.result(
      postWithAuth(checkInsightsEndpoint, invalidPayload),
      10.seconds
    )
    response
  }

  def postInvalidAuthRequest(emailAddress: String): StandaloneWSResponse = {
    val request =
      s"""{
         |"email": "$emailAddress"
         |}""".stripMargin

    val response: StandaloneWSResponse =
      Await.result(
        postWithInvalidAuth(checkInsightsEndpoint, request),
        10.seconds
      )
    response
  }

  def assertEmailIsOnWatchlist(emailAddress: String): Unit = {
    val response = postCheckInsightsRequest(emailAddress)
    val body     = response.body
    val json     = Json.parse(body)
    assert((json \ "attribute").asOpt[String].contains("EMAIL"))
    assert((json \ "value").asOpt[String].contains(emailAddress))
    assert((json \ "insights" \ "risk" \ "score").asOpt[Int].contains(100))
    assert((json \ "insights" \ "risk" \ "reason").asOpt[String].contains("ON_WATCH_LIST"))
    assert((json \ "insights" \ "watchlistData" \ "isOnWatchlist").asOpt[Boolean].contains(true))
    assert((json \ "insights" \ "graphData" \ "reasons").asOpt[Seq[String]].exists(_.contains("No risk")))
  }

  def assertEmailIsNotOnWatchlist(emailAddress: String): Unit = {
    val response = postCheckInsightsRequest(emailAddress)
    val body     = response.body
    val json     = Json.parse(body)
    assert((json \ "attribute").asOpt[String].contains("EMAIL"))
    assert((json \ "value").asOpt[String].contains(emailAddress))
    assert((json \ "insights" \ "risk" \ "score").asOpt[Int].contains(0))
    assert((json \ "insights" \ "risk" \ "reason").asOpt[String].contains("NOT_ON_WATCH_LIST. Reasons: No risk"))
    assert((json \ "insights" \ "watchlistData" \ "isOnWatchlist").asOpt[Boolean].contains(false))
    assert((json \ "insights" \ "graphData" \ "reasons").asOpt[Seq[String]].exists(_.contains("No risk")))
  }

  def postInvalidEndpoint(invalidEndpoint: String): StandaloneWSResponse = {
    val request =
      s"""{
         |"email": "john.doe@test.com"
         |}""".stripMargin

    val response: StandaloneWSResponse =
      Await.result(
        invalidPostRequest(invalidEndpoint, request),
        10.seconds
      )
    response
  }
}
