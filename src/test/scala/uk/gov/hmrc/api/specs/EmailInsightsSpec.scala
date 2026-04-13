/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers.*
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class EmailInsightsSpec extends BaseSpec with BeforeAndAfterEach with BeforeAndAfterAll {

  val riskyEmailAddress = "john.doe@test.com"
  val safeEmailAddress  = "james.hughes@test.com"

  val invalidPayload          = "{}"
  val invalidInsightsEndpoint = s"$baseUrl/check/invalid-endpoint"

  override def beforeEach(): Unit = {
    clearWatchlistData()
    clearGraphData()
  }

  override def afterEach(): Unit = {
    clearWatchlistData()
    clearGraphData()
    super.afterAll()
  }

  Feature("[EI-1]- Email Insights - Check if an email exists/does not exist") {
    Scenario("[EI.1.1] - Email exists on the watchlist & graph database") {
      Given("the watchlist & graph database is empty")
      assert(getWatchlistData.isEmpty)
      assert(getGraphData.isEmpty)
      assert(getCountData.isEmpty)


      When(s"I add the email '$riskyEmailAddress' to the watchlist & graph database")
      createWatchlistData(0, riskyEmailAddress)
      createGraphData(1000, riskyEmailAddress)
      createCountData(0, riskyEmailAddress)

      And("I send a POST request to the check/insights endpoint")
      postCheckInsightsRequest(riskyEmailAddress)

      Then(
        "the response should indicate that the email exists on the watchlist & graph database & the payload is correct"
      )
      validateRiskyEmailPayload(riskyEmailAddress)
    }
    Scenario("[EI.1.2] - Email does not exist on the watchlist & graph database") {
      Given("the watchlist & graph database is empty")
      assert(getWatchlistData.isEmpty)
      assert(getGraphData.isEmpty)
      assert(getCountData.isEmpty)

      When(s"I add the email '$riskyEmailAddress' to the watchlist & graph database")
      createWatchlistData(0, riskyEmailAddress)
      createGraphData(1000, riskyEmailAddress)
      createCountData(0, riskyEmailAddress)

      And("I send a POST request to the check/insights endpoint")
      postCheckInsightsRequest(safeEmailAddress)

      Then(
        s"the response should indicate that the email does not exist on the watchlist & graph database & the payload is correct"
      )
      validateSafeNumberPayload(safeEmailAddress)
    }
  }

  Feature("[EI-2]- Email Insights - POST Invalid requests to check/insights endpoint") {
    Scenario("[EI.2.1] - POST to check/insights with invalid payload and return a 400 HTTP response") {
      Given("the watchlist is empty")
      assert(getWatchlistData.isEmpty)

      When("a POST to check/insights endpoint with an invalid payload")
      val response = postInvalidPayloadRequest(invalidPayload)

      Then("a 400 HTTP response is returned")
      assert(response.status == 400)
      assert(response.body.contains("\"message\":\"Invalid InsightsRequest payload"))
    }
    Scenario("[EI.2.2] - POST to check/insights endpoint with an invalid endpoint and return a 404 HTTP response") {
      Given("the watchlist is empty")
      assert(getWatchlistData.isEmpty)

      When("a POST request is sent via an invalid endpoint")
      val response = postInvalidEndpoint(invalidInsightsEndpoint)

      Then("a 404 HTTP response is returned")
      assert(response.status == 404)
      assert(response.body.contains("URI not found"))
    }
  }
}
