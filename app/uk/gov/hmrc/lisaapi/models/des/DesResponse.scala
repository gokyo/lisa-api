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

package uk.gov.hmrc.lisaapi.models.des

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.lisaapi.models._

trait DesResponse

case class DesAccountResponse(accountID: String) extends DesResponse

case class DesLifeEventResponse(lifeEventID: String) extends DesResponse
case class DesLifeEventRetrievalResponse(lifeEventID: LifeEventId, eventType: LifeEventType, eventDate: DateTime) extends DesResponse
case class DesCreateInvestorResponse(investorID: String) extends DesResponse
case class DesTransactionResponse(transactionID: String, message: String) extends DesResponse
case class DesFailureResponse(code: String = "INTERNAL_SERVER_ERROR", reason: String = "Internal Server Error") extends DesResponse
case class DesLifeEventExistResponse(code: String, reason: String, lifeEventID: String) extends DesResponse
case object DesEmptySuccessResponse extends DesResponse
case class DesUpdateSubscriptionSuccessResponse (code: String, reason: String)extends DesResponse
case class DesReinstateAccountSuccessResponse (code: String, reason: String)extends DesResponse
case class DesGetBonusPaymentResponse(lifeEventId: Option[LifeEventId],
                                      periodStartDate: DateTime,
                                      periodEndDate: DateTime,
                                      htbTransfer: Option[HelpToBuyTransfer],
                                      inboundPayments: InboundPayments,
                                      bonuses: Bonuses,
                                      creationDate: DateTime,
                                      status: String) extends DesResponse

object DesResponse {
  implicit val desCreateAccountResponseFormats: OFormat[DesAccountResponse] = Json.format[DesAccountResponse]

  implicit val desCreateInvestorResponseFormats: OFormat[DesCreateInvestorResponse] = Json.format[DesCreateInvestorResponse]
  implicit val desLifeEventResponseFormats: OFormat[DesLifeEventResponse] = Json.format[DesLifeEventResponse]
  implicit val desTransactionResponseFormats: OFormat[DesTransactionResponse] = Json.format[DesTransactionResponse]
  implicit val desUpdateSubscriptionResponseFormats: OFormat[DesUpdateSubscriptionSuccessResponse] = Json.format[DesUpdateSubscriptionSuccessResponse]
  implicit val desReinstateAccountResponseFormats: OFormat[DesReinstateAccountSuccessResponse] = Json.format[DesReinstateAccountSuccessResponse]

  implicit val desFailureReads: Reads[DesFailureResponse] = (
    (JsPath \ "code").read[String] and
    (JsPath \ "reason").read[String]
  )(DesFailureResponse.apply _)

  implicit val desFailureWrites: Writes[DesFailureResponse] = (
    (JsPath \ "code").write[String] and
    (JsPath \ "message").write[String]
  )(unlift(DesFailureResponse.unapply))

  implicit val requestLifeEventResponseReads: Reads[DesLifeEventRetrievalResponse] = (
    (JsPath \ "lifeEventID").read(JsonReads.lifeEventId) and
    (JsPath \ "eventType").read(JsonReads.lifeEventType) and
    (JsPath \ "eventDate").read(JsonReads.notFutureDate).map(new DateTime(_))
  )(DesLifeEventRetrievalResponse.apply _)

  implicit val requestLifeEventAlreadyExistResponseFormats: OFormat[DesLifeEventExistResponse] = Json.format[DesLifeEventExistResponse]

  implicit val desGetBonusPaymentResponse: Reads[DesGetBonusPaymentResponse] = (
    (JsPath \ "lifeEventId").readNullable(JsonReads.lifeEventId) and
    (JsPath \ "claimPeriodStart").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "claimPeriodEnd").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "htbInAmountForPeriod").readNullable[Amount] and
    (JsPath \ "htbInAmountYtd").readNullable[Amount] and
    (JsPath \ "newSubsInPeriod").readNullable[Amount] and
    (JsPath \ "newSubsYtd").read[Amount] and
    (JsPath \ "totalSubsInPeriod").read[Amount] and
    (JsPath \ "totalSubsYtd").read[Amount] and
    (JsPath \ "bonusDueForPeriod").read[Amount] and
    (JsPath \ "bonusDueYtd").read[Amount] and
    (JsPath \ "bonusPaidYtd").readNullable[Amount] and
    (JsPath \ "claimReason").read[String] and
    (JsPath \ "creationDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "paymentStatus").read[String]
  )(
    (lifeEventId,
     periodStartDate,
     periodEndDate,
     htbInAmountForPeriod,
     htbInAmountYtd,
     newSubsInPeriod,
     newSubsYtd,
     totalSubsInPeriod,
     totalSubsYtd,
     bonusDueForPeriod,
     bonusDueYtd,
     bonusPaidYtd,
     claimReason,
     creationDate,
     status) =>
        DesGetBonusPaymentResponse(
          lifeEventId,
          periodStartDate,
          periodEndDate,
          (htbInAmountForPeriod, htbInAmountYtd) match {
            case (Some(amountForPeriod), Some(amountYtd)) => {
              Some(HelpToBuyTransfer(amountForPeriod, amountYtd))
            }
            case _ => None
          },
          InboundPayments(newSubsInPeriod,
            newSubsYtd,
            totalSubsInPeriod,
            totalSubsYtd
          ),
          Bonuses(bonusDueForPeriod,
            bonusDueYtd,
            bonusPaidYtd,
            claimReason match {
              case "LIFE_EVENT" => "Life Event"
              case "REGULAR_BONUS" => "Regular Bonus"
            }
          ),
          creationDate,
          status match {
            case "PENDING" => "Pending"
            case "PAID" => "Paid"
            case "VOID" => "Void"
            case "CANCELLED" => "Cancelled"
          }
        )
  )

}