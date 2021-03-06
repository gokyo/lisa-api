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

package uk.gov.hmrc.lisaapi.models

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Writes}

trait GetTransactionResponse

case object GetTransactionErrorResponse extends GetTransactionResponse
case object GetTransactionTransactionNotFoundResponse extends GetTransactionResponse
case object GetTransactionAccountNotFoundResponse extends GetTransactionResponse

case class GetTransactionSuccessResponse(transactionId: String,
                                         creationDate: DateTime,
                                         status: String,
                                         bonusDueForPeriod: Option[Amount] = None,
                                         paymentDate: Option[DateTime] = None,
                                         paymentDueDate: Option[DateTime] = None,
                                         paymentAmount: Option[Amount] = None,
                                         paymentReference: Option[String] = None,
                                         chargeReference: Option[String] = None) extends GetTransactionResponse

object GetTransactionResponse {
  val dateFormat = "yyyy-MM-dd"

  implicit val successWrites: Writes[GetTransactionSuccessResponse] = (
    (JsPath \ "transactionId").write[String] and
    (JsPath \ "creationDate").write[String].contramap[DateTime](d => d.toString(dateFormat)) and
    (JsPath \ "status").write[String] and
    (JsPath \ "bonusDueForPeriod").writeNullable[Amount] and
    (JsPath \ "paymentDate").writeNullable[String].contramap[Option[DateTime]](d => d.map(v => v.toString(dateFormat))) and
    (JsPath \ "paymentDueDate").writeNullable[String].contramap[Option[DateTime]](d => d.map(v => v.toString(dateFormat))) and
    (JsPath \ "paymentAmount").writeNullable[Amount] and
    (JsPath \ "paymentReference").writeNullable[String] and
    (JsPath \ "chargeReference").writeNullable[String]
  )(unlift(GetTransactionSuccessResponse.unapply))
}