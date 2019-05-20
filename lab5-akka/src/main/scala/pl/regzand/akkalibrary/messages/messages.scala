package pl.regzand.akkalibrary.messages

import java.util.UUID

// ==============================================
//  REQUESTS
// ==============================================
abstract class Request(val uuid: String = UUID.randomUUID().toString) {

  def notFoundResponse = NotFoundResponse(uuid)

}

case class SearchRequest(title: String) extends Request

case class OrderRequest(title: String) extends Request

case class ReadRequest(title: String) extends Request


// ==============================================
//  RESPONSES
// ==============================================
abstract class Response(uuid: String)

case class NotFoundResponse(uuid: String) extends Response(uuid)
