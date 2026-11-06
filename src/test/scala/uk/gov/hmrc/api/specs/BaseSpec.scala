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
import org.scalatest.matchers.should.Matchers.*
import play.api.libs.json.*

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait BaseSpec extends AnyFeatureSpec with GivenWhenThen with Matchers with HttpClientHelper {

  val watchlistTestOnlyEndpoint  = s"$testOnlyBaseUrl/test-only/watchlist/data"
  val testOnlyEndpointDeleteData = s"$watchlistTestOnlyEndpoint/delete"
  val testOnlyEndpointCreateData = s"$watchlistTestOnlyEndpoint/create"
  val testOnlyEndpointCounts     = s"$watchlistTestOnlyEndpoint/counts"

  val countsTestOnlyEndpoint = s"$countsDatabaseUrl/test-only/occurrence-logs/data"

  val testOnlyEndpointDeleteCountData  = s"$countsTestOnlyEndpoint/delete"
  val testOnlyEndpointCreateCountData  = s"$countsTestOnlyEndpoint/create"
  val testOnlyEndpointOccurrenceCounts = s"$countsTestOnlyEndpoint/counts"

  val graphDataTestOnlyEndpoint = s"$graphDatabaseUrl/test-only/cip-risk/str/vertex-data"
  val checkInsightsEndpoint     = s"$baseUrl/check/insights"

  // val checkInsightsEndpoint = s"$baseUrl/check/insights"

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

  def createCountData(numberOfGeneratedEmails: Int, manualEmailAddresses: String): Unit = {
    val manualEmailsJson = s"[\"$manualEmailAddresses\"]"
    val request          =
      s"""{
         | "generatedEntries": {
         | "numberOfEmails": $numberOfGeneratedEmails,
         | "numberOfAttributeGroupsPerEmail": 4
         | },
         | "manualEntries": {
         | "emailAddresses" : $manualEmailsJson,
         | "numberOfAttributeGroupsPerEmail": 2
         | }
         |}""".stripMargin

    val createEmailInsightsTestOnlyData: StandaloneWSResponse =
      Await.result(
        post(testOnlyEndpointCreateCountData, request),
        10.seconds
      )
    val responseBody                                          = createEmailInsightsTestOnlyData.body
    responseBody should include regex "Created \\d+ email occurrence logs"
    assert(createEmailInsightsTestOnlyData.status == 200)
  }

  def getCounts: Int = {
    val response = Await.result(
      get(testOnlyEndpointOccurrenceCounts),
      10.seconds
    )
    val body     = if (response.status == 200 && response.body.trim.nonEmpty) response.body else "{}"
    val json     = Json.parse(body)
    (json \ "emailOccurrenceLogsCount").asOpt[Int].getOrElse(0)
  }

  def clearCountsData(): Assertion = {
    val clearDataFromEndpoint =
      Await.result(delete(testOnlyEndpointDeleteCountData), 10.seconds)
    val responseBody          = clearDataFromEndpoint.body
    responseBody should include regex "Deleted \\d+ email occurrence logs"
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

  def createGraphDataFromRequest(request: String): Unit = {
    val createEmailAddressInsightsTestOnlyData: StandaloneWSResponse =
      Await.result(
        post(graphDataTestOnlyEndpoint, request),
        10.seconds
      )

    val responseBody = createEmailAddressInsightsTestOnlyData.body
    responseBody should include regex "Generated \\d+ vertices"
    assert(createEmailAddressInsightsTestOnlyData.status == 200)
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
    assert((json \ "insights" \ "graphData" \ "avgHops").asOpt[BigDecimal].exists(_ >= 0))

    def getSeq(value: JsValue, field: String): Seq[JsValue] =
      (value \ field).asOpt[Seq[JsValue]].getOrElse(Seq.empty)

    val attributes = (json \ "insights" \ "relationship" \ "attributes").asOpt[Seq[JsValue]].getOrElse(Seq.empty)
    assert(attributes.nonEmpty, "attributes should not be empty")

    assert(attributes.forall(attr => (attr \ "attributeType").asOpt[String].isDefined))
    assert(attributes.forall(attr => (attr \ "count").asOpt[Int].exists(_ > 0)))

    val attributesWithValues = attributes.map { attr =>
      val attributeType = (attr \ "attributeType").as[String]
      val values        = getSeq(attr, "attributeValues")
      (attributeType, values)
    }

    val attributeValues = attributesWithValues.flatMap { case (_, values) => values }

    assert(attributeValues.forall(v => (v \ "attributeValue").asOpt[String].isDefined))
    assert(attributeValues.forall(v => (v \ "numberOfOccurrences").asOpt[Int].isDefined))
    assert(attributeValues.forall(v => (v \ "lastSeen").asOpt[String].isDefined))

    val expectedPrefixes: Map[String, String] = Map(
      "agent_code"       -> "agent_code_",
      "sa_utr"           -> "sa_utr_",
      "vrn"              -> "vrn_",
      "ct_utr"           -> "ct_utr_",
      "paye_ref"         -> "paye_ref_",
      "user_id"          -> "user_id_",
      "person_full_name" -> "person_full_name_"
    )

    attributesWithValues.foreach { case (attributeType, values) =>
      val expectedPrefix =
        expectedPrefixes.getOrElse(attributeType, fail(s"Unexpected attributeType: '$attributeType'"))
      values.foreach { v =>
        val attributeValue = (v \ "attributeValue").as[String]
        assert(
          attributeValue.startsWith(expectedPrefix),
          s"attributeValue '$attributeValue' does not match prefix '$expectedPrefix' for attributeType '$attributeType'"
        )
      }
    }
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

    assert((json \ "insights" \ "graphData" \ "avgHops").asOpt[BigDecimal].exists(_ >= 0))
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

  def createShortestPathGraphEmailAddress(vertexRecords: Seq[(Int, String, Int)]): Unit = {
    val request = Json.obj(
      "randomEntriesToGenerate" -> 10000,
      "batchInsertSize"         -> 1000,
      "vertexRecords"           -> vertexRecords.map { case (vertexId, attributeId, hopsToClosestRisky) =>
        Json.obj(
          "vertexId"           -> vertexId,
          "attributeId"        -> attributeId,
          "data"               -> "{}",
          "vertexType"         -> "email",
          "hopsToClosestRisky" -> hopsToClosestRisky
        )
      }
    )

    createGraphDataFromRequest(Json.stringify(request))
  }

  def validateShortestPathGraphResponse(emailAddress: String): Unit = {
    val response = postCheckInsightsRequest(emailAddress)
    val body     = response.body
    val json     = Json.parse(body)
    assert((json \ "attributeType").asOpt[String].contains("EMAIL"))
    assert((json \ "attributeValue").asOpt[String].contains(emailAddress))

    val shortestReasons = (json \ "insights" \ "resolvedShortestPathGraphResponse" \ "reason")
      .asOpt[String]
      .map(Seq(_))
      .getOrElse(Seq.empty)

    assert(shortestReasons.nonEmpty)
    assert(shortestReasons.exists(_.contains(s"Attribute $emailAddress is in the database.")))
    assert(shortestReasons.exists(_.contains("data is included.")))

    val riskPaths = (json \ "insights" \ "resolvedShortestPathGraphResponse" \ "riskPaths")
      .asOpt[Seq[play.api.libs.json.JsValue]]
      .getOrElse(Seq.empty)

    assert(riskPaths.nonEmpty)

    val shortestPath = (riskPaths.head \ "shortestPath")
      .asOpt[Seq[play.api.libs.json.JsValue]]
      .getOrElse(Seq.empty)

    assert(shortestPath.size == 3)
    assert((shortestPath.head \ "attributeId").asOpt[String].contains(emailAddress))
    assert((shortestPath.head \ "attributeType").asOpt[String].contains("email"))
    assert((shortestPath(1) \ "attributeType").asOpt[String].isDefined)
    assert((shortestPath(2) \ "attributeType").asOpt[String].isDefined)

    val riskDetails = (json \ "insights" \ "resolvedShortestPathGraphResponse" \ "riskDetails")
      .asOpt[Seq[play.api.libs.json.JsValue]]
      .getOrElse(Seq.empty)

    assert(riskDetails.nonEmpty)

    val riskDetail = riskDetails
      .find(detail => (detail \ "source" \ "attributeId").asOpt[String].contains(emailAddress))
      .getOrElse(fail(s"No risk detail has source email '$emailAddress': ${Json.stringify(Json.toJson(riskDetails))}"))
    assert((riskDetail \ "source" \ "attributeId").asOpt[String].contains(emailAddress))
    assert((riskDetail \ "source" \ "attributeType").asOpt[String].contains("email"))
    assert((riskDetail \ "destination" \ "attributeId").asOpt[String].isDefined)
    assert((riskDetail \ "destination" \ "attributeType").asOpt[String].isDefined)
    assert((riskDetail \ "weight").asOpt[Int].contains(1))
    assert((riskDetail \ "events" \ 0 \ "type").asOpt[String].exists(_.startsWith("SomeBusinessEvent-")))
  }
}
