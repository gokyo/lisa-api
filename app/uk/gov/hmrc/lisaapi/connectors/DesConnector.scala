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

package uk.gov.hmrc.lisaapi.connectors

import play.api.Logger
import uk.gov.hmrc.lisaapi.config.{AppContext, WSHttp}
import uk.gov.hmrc.lisaapi.controllers.JsonFormats
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpReads, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import play.api.libs.json.Reads

trait DesConnector extends ServicesConfig with JsonFormats {

  val httpPost:HttpPost = WSHttp
  lazy val desUrl = baseUrl("des")
  lazy val lisaServiceUrl = s"$desUrl/lifetime-isa/manager"

  val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }

  private def updateHeaderCarrier(headerCarrier: HeaderCarrier) =
    headerCarrier.copy(extraHeaders = Seq(("Environment" -> AppContext.desUrlHeaderEnv)),
          authorization = Some(Authorization(s"Bearer ${AppContext.desAuthToken}")))

  /**
    * Attempts to create a new LISA investor
    *
    * @return A tuple of the http status code and an (optional) data response
    */
  def createInvestor(lisaManager: String, request: CreateLisaInvestorRequest)(implicit hc: HeaderCarrier): Future[(Int, Option[DesCreateInvestorResponse])] = {
    val uri = s"$lisaServiceUrl/$lisaManager/investors"

    val result = httpPost.POST[CreateLisaInvestorRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc))

    result.map(r => {
      Logger.debug(s"DES Response for CreateInvestor : $r")
      // catch any NullPointerExceptions that may occur from r.json being a null
      val res = Try(r.json.asOpt[DesCreateInvestorResponse])
      Try(r.json.asOpt[DesCreateInvestorResponse]) match {
        case Success(data) => (r.status, data)
        case Failure(_) => Logger.error(s"ERROR from DES $uri + Not able to create a response"); (r.status, None)
      }
    })
  }

  /**
    * Attempts to create a new LISA account
    */
  def createAccount(lisaManager: String, request: CreateLisaAccountCreationRequest)(implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts"

    val result = httpPost.POST[CreateLisaAccountCreationRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc))

    result.map(res => {
      parseDesResponse[DesAccountResponse](res)._2
    })
  }

  /**
    * Attempts to transfer an existing LISA account
    */
  def transferAccount(lisaManager: String, request: CreateLisaAccountTransferRequest)(implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts"

    val result = httpPost.POST[CreateLisaAccountTransferRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc))

    result.map(res => {
      parseDesResponse[DesAccountResponse](res)._2
    })
  }

  /**
    * Attempts to close a LISA account
    *
    * @return A tuple of the http status code and an (optional) data response
    */
  def closeAccount(lisaManager: String, accountId: String, request: CloseLisaAccountRequest)(implicit hc: HeaderCarrier): Future[(Int, Option[DesAccountResponseOld])] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/close-account"

    val result = httpPost.POST[CloseLisaAccountRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc))

    result.map(r => {
      // catch any NullPointerExceptions that may occur from r.json being a null
      Try(r.json.asOpt[DesAccountResponseOld]) match {
        case Success(data) => Logger.debug(s"DES Success Response : ${r.json}") ;(r.status, data)
        case Failure(_) => Logger.error(s"DES failure response for $uri and response : ${r.status}" ); (r.status, None)
      }
    })
  }

  /**
    * Attempts to report a LISA Life Event
    */
  def reportLifeEvent(lisaManager: String, accountId: String, request: ReportLifeEventRequest)
                     (implicit hc: HeaderCarrier): Future[DesResponse] = {

    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/events"

    val result = httpPost.POST[ReportLifeEventRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc))

    result.map(res => {
      parseDesResponse[DesLifeEventResponse](res)._2
    })
  }

  /**
    * Attempts to request a bonus payment
    *
    * @return A tuple of the http status code and a des response
    */
  def requestBonusPayment(lisaManager: String, accountId: String, request: RequestBonusPaymentRequest)
                     (implicit hc: HeaderCarrier): Future[(Int, DesResponse)] = {

    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/transactions"

    val result = httpPost.POST[RequestBonusPaymentRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc))

    result.map(res => {
      parseDesResponse[DesTransactionResponse](res)
    })
  }

  // scalastyle:off magic.number
  def parseDesResponse[A <: DesResponse](res: HttpResponse)(implicit reads:Reads[A]): (Int, DesResponse) = {
    res.status match {
      case 201 => Try(res.json.as[A]) match {
        case Success(data) =>
          Logger.debug(s"DES success Response : ${res.json}")
                (201, data)
        case Failure(ex) => {
          Logger.error(s"DES failure response.${res} Exception: ${ex.getMessage}")
          (500, DesFailureResponse())
        }
      }
      case status:Int => Try(res.json.as[DesFailureResponse]) match {
        case Success(data) =>       Logger.debug(s"DES Response : ${res.json}")
          (status, data)
        case Failure(ex) => {
          Logger.error(s"DES failure response. ${res} Exception: ${ex.getMessage}")
          (500, DesFailureResponse())
        }
      }
    }
  }
}


object DesConnector extends DesConnector {

}