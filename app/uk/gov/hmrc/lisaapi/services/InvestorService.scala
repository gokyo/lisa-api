/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.services

import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.{DesCreateInvestorResponse, DesFailureResponse}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

trait InvestorService  {
  val desConnector: DesConnector


  def createInvestor(lisaManager: String, request: CreateLisaInvestorRequest)(implicit hc: HeaderCarrier) : Future[CreateLisaInvestorResponse] = {
    val response = desConnector.createInvestor(lisaManager, request)

    response map {
      case (409, existsResponse: DesCreateInvestorResponse) => CreateLisaInvestorAlreadyExistsResponse(existsResponse.investorID)
      case (_, successResponse: DesCreateInvestorResponse) => CreateLisaInvestorSuccessResponse(successResponse.investorID)
      case (status: Int, errorResponse: DesFailureResponse) => CreateLisaInvestorErrorResponse(status, errorResponse)
                                                          }
  }

}

object InvestorService extends InvestorService {
  override val desConnector: DesConnector = DesConnector
}