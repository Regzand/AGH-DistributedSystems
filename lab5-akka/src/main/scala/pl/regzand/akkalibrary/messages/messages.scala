package pl.regzand.akkalibrary.messages

import java.util.UUID

// ==============================================
//  REQUESTS
// ==============================================
abstract class Request(val uuid: String = UUID.randomUUID().toString) {

  def notFoundResponse = NotFoundResponse(uuid)
  def successfulResponse = SuccessfulResponse(uuid)

}

case class SearchRequest(title: String) extends Request

case class OrderRequest(title: String) extends Request

case class ReadRequest(title: String) extends Request


// ==============================================
//  RESPONSES
// ==============================================
abstract class Response(uuid: String)

case class NotFoundResponse(uuid: String) extends Response(uuid)

case class SuccessfulResponse(uuid: String) extends Response(uuid)

case class PriceResponse(price: Float, uuid: String) extends Response(uuid)
