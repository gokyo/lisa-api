/*
 * Copyright 2017 HM Revenue & Customs
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

package unit.connectors

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.{CreateLisaAccountCreationRequest, CreateLisaInvestorRequest}
import uk.gov.hmrc.lisaapi.models.des.{DesCreateAccountResponse, DesCreateInvestorResponse}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class DesConnectorSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  val statusCodeSuccess = 200
  val statusCodeServiceUnavailable = 503
  val rdsCodeInvestorNotFound = 63214
  val rdsCodeAccountAlreadyExists = 63219

  "Create Lisa Investor endpoint" must {

    "Return a status code of 200" when {
      "Given a 200 response from DES" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = statusCodeSuccess, responseJson = None)))

        doCreateInvestorRequest { response =>
          response must be((
            statusCodeSuccess,
            None
          ))
        }
      }
    }

    "Return no DesCreateInvestorResponse" when {
      "The DES response has no json body" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeServiceUnavailable,
                responseJson = None
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be((
            statusCodeServiceUnavailable,
            None
          ))
        }
      }
    }

    "Return any empty DesCreateInvestorResponse" when {
      "The DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse("""[1,2,3]"""))
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be((
            statusCodeSuccess,
            Some(DesCreateInvestorResponse(None, None))
          ))
        }
      }
    }

    "Return a populated DesCreateInvestorResponse" when {
      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse(s"""{"rdsCode":$rdsCodeInvestorNotFound, "investorId": "AB123456"}"""))
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be((
            statusCodeSuccess,
            Some(DesCreateInvestorResponse(rdsCode = Some(rdsCodeInvestorNotFound), investorId = Some("AB123456")))
          ))
        }
      }
    }

  }

  "Create Lisa Account endpoint" must {

    "Return a status code of 200" when {
      "Given a 200 response from DES" in {
        when(mockHttpPost.POST[CreateLisaAccountCreationRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = statusCodeSuccess, responseJson = None)))

        doCreateAccountRequest { response =>
          response must be((
            statusCodeSuccess,
            None
          ))
        }
      }
    }

    "Return no DesCreateAccountResponse" when {
      "The DES response has no json body" in {
        when(mockHttpPost.POST[CreateLisaAccountCreationRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeServiceUnavailable,
                responseJson = None
              )
            )
          )

        doCreateAccountRequest { response =>
          response must be((
            statusCodeServiceUnavailable,
            None
          ))
        }
      }
    }

    "Return any empty DesCreateAccountResponse" when {
      "The DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[CreateLisaAccountCreationRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse("""[1,2,3]"""))
              )
            )
          )

        doCreateAccountRequest { response =>
          response must be((
            statusCodeSuccess,
            Some(DesCreateAccountResponse(None, None))
          ))
        }
      }
    }

    "Return a populated DesCreateAccountResponse" when {
      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[CreateLisaAccountCreationRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse(s"""{"rdsCode":$rdsCodeAccountAlreadyExists, "accountId": "AB123456"}"""))
              )
            )
          )

        doCreateAccountRequest { response =>
          response must be((
            statusCodeSuccess,
            Some(DesCreateAccountResponse(rdsCode = Some(rdsCodeAccountAlreadyExists), accountId = Some("AB123456")))
          ))
        }
      }
    }

  }

  private def doCreateInvestorRequest(callback: ((Int, Option[DesCreateInvestorResponse])) => Unit) = {
    val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-01-01"))
    val response = Await.result(SUT.createInvestor("Z019283", request), Duration.Inf)

    callback(response)
  }

  private def doCreateAccountRequest(callback: ((Int, Option[DesCreateAccountResponse])) => Unit) = {
    val request = CreateLisaAccountCreationRequest("1234567890", "Z019283", "9876543210", new DateTime("2000-01-01"))
    val response = Await.result(SUT.createAccount("Z019283", request), Duration.Inf)

    callback(response)
  }

  val mockHttpPost = mock[HttpPost]
  implicit val hc = HeaderCarrier()

  object SUT extends DesConnector {
    override val httpPost = mockHttpPost
  }
}