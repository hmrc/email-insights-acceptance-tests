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

  val watchlistTestOnlyEndpoint  = s"$testOnlyBaseUrl/test-only/watchlist/data"
  val testOnlyEndpointDeleteData = s"$watchlistTestOnlyEndpoint/delete"
  val testOnlyEndpointCreateData = s"$watchlistTestOnlyEndpoint/create"
  val testOnlyEndpointCounts     = s"$watchlistTestOnlyEndpoint/counts"

  val graphDataTestOnlyEndpoint = s"$graphDatabaseUrl/test-only/cip-risk/str/vertex-data"

  val checkInsightsEndpoint = s"$baseUrl/check/insights"

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
      get(watchlistTestOnlyEndpoint),
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
      get(watchlistTestOnlyEndpoint),
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

  def createGraphData(numberOfEntries: Int, email: String): Unit = {
    val request =
      s"""{
         |  "randomEntriesToGenerate": $numberOfEntries,
         |  "batchInsertSize": 101,
         |  "vertexRecords": [{
         |    "vertexId": 1,
         |    "attributeId": "$email",
         |    "data": "{}",
         |    "vertexType": "email",
         |    "hopsToClosestRisky": 2
         |  }]
         |}""".stripMargin

    val createEmailInsightsTestOnlyData: StandaloneWSResponse =
      Await.result(
        post(graphDataTestOnlyEndpoint, request),
        10.seconds
      )

    val responseBody = createEmailInsightsTestOnlyData.body
    responseBody should include regex "Generated \\d+ vertices"
    assert(createEmailInsightsTestOnlyData.status == 200)
  }

  def getGraphData: Seq[String] = {
    val response = Await.result(
      get(graphDataTestOnlyEndpoint),
      10.seconds
    )
    val body     = if (response.status == 200 && response.body.trim.nonEmpty) response.body else "{}"
    val json     = Json.parse(body)
    (json \\ "emailsOnWatchlistEntries").headOption
      .flatMap(_.asOpt[Seq[String]])
      .getOrElse(Seq.empty)
  }

  def clearGraphData(): Assertion = {
    val clearDataFromEndpoint =
      Await.result(delete(graphDataTestOnlyEndpoint), 10.seconds)
    val responseBody          = clearDataFromEndpoint.body
    responseBody should include regex "Deleted \\d+ vertices"
  }

  def postCheckInsightsRequest(emailAddress: String): StandaloneWSResponse = {
    val request =
      s"""{
         |"email": "$emailAddress"
         |}""".stripMargin

    val response: StandaloneWSResponse =
      Await.result(
        post(checkInsightsEndpoint, request),
        10.seconds
      )
    response
  }

  def postInvalidPayloadRequest(payload: String): StandaloneWSResponse = {
    val invalidPayload = payload
    val response       = Await.result(
      post(checkInsightsEndpoint, invalidPayload),
      10.seconds
    )
    response
  }

  def validateRiskyEmailPayload(emailAddress: String): Unit = {
    val response = postCheckInsightsRequest(emailAddress)
    val body     = response.body
    val json     = Json.parse(body)
    assert((json \ "attributeType").asOpt[String].contains("EMAIL"))
    assert((json \ "attributeValue").asOpt[String].contains(emailAddress))
    assert((json \ "insights" \ "risk" \ "score").asOpt[Int].contains(100))
    assert((json \ "insights" \ "risk" \ "reason").asOpt[String].contains("ON_WATCH_LIST"))
    assert((json \ "insights" \ "watchlistData" \ "isOnWatchlist").asOpt[Boolean].contains(true))
    val reasons  = (json \ "insights" \ "graphData" \ "reasons").asOpt[Seq[String]].getOrElse(Seq.empty)
    assert(reasons.nonEmpty)
    assert(reasons.exists(_.contains(s"EMAIL '$emailAddress'")))
    assert(reasons.exists(_.contains("hops from something risky")))

    assert((json \ "insights" \ "graphData" \ "hops").asOpt[Int].contains(2))
    assert((json \ "insights" \ "graphData" \ "avgHops").asOpt[BigDecimal].exists(_ >= 2))
  }

  def validateSafeNumberPayload(emailAddress: String): Unit = {
    val response = postCheckInsightsRequest(emailAddress)
    val body     = response.body
    val json     = Json.parse(body)
    assert((json \ "attributeType").asOpt[String].contains("EMAIL"))
    assert((json \ "attributeValue").asOpt[String].contains(emailAddress))
    assert((json \ "insights" \ "risk" \ "score").asOpt[Int].contains(0))
    assert((json \ "insights" \ "risk" \ "reason").asOpt[String].exists(_.startsWith("NOT_ON_WATCH_LIST")))
    assert((json \ "insights" \ "watchlistData" \ "isOnWatchlist").asOpt[Boolean].contains(false))
    val reasons  = (json \ "insights" \ "graphData" \ "reasons").asOpt[Seq[String]].getOrElse(Seq.empty)
    assert(reasons.nonEmpty)
    assert(reasons.exists(_.contains(s"EMAIL '$emailAddress' is not in the database.")))
    assert(reasons.exists(_.contains("hops from something risky")))

    assert((json \ "insights" \ "graphData" \ "avgHops").asOpt[BigDecimal].exists(_ >= 2))
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
