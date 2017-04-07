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

package unit.services

import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.{RequestBonusPaymentResponse, _}
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesLifeEventResponse, DesTransactionResponse}
import uk.gov.hmrc.lisaapi.services.{BonusPaymentService, LifeEventService}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class BonusPaymentServiceSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  "BonusPaymentService" must {

    "return a Success Response" when {
      "given a success response from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(),any())(any())).
          thenReturn(Future.successful((201, DesTransactionResponse("AB123456"))))

        doRequest{response =>
          response mustBe RequestBonusPaymentSuccessResponse("AB123456")
        }
      }
    }

    "return an Error Response" when {
      "given an error response from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(),any())(any())).
          thenReturn(Future.successful((500, DesFailureResponse("code1", "reason1"))))

        doRequest{response =>
          response mustBe RequestBonusPaymentErrorResponse(500, DesFailureResponse("code1", "reason1"))
        }
      }
    }
  }

  private def doRequest(callback: (RequestBonusPaymentResponse) => Unit) = {
    val request = RequestBonusPaymentRequest(
      lifeEventID = Some("1234567891"),
      periodStartDate = new DateTime("2016-05-22"),
      periodEndDate = new DateTime("2017-05-22"),
      transactionType = "Bonus",
      htbTransfer = Some(HelpToBuyTransfer(0f, 0f)),
      inboundPayments = InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
      bonuses = Bonuses(1000f, 1000f, None, "Life Event")
    )

    val response = Await.result(SUT.requestBonusPayment("Z019283", "192837", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val mockDesConnector = mock[DesConnector]
  object SUT extends BonusPaymentService {
    override val desConnector: DesConnector = mockDesConnector
  }
}