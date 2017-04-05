package controllers

import javax.inject.Inject

import play.api.http.{ContentTypes, MimeTypes}
import play.api.mvc._

class Main @Inject()(components: ControllerComponents)
    extends AbstractController(components) {

  def aChannel() = Action { request: Request[AnyContent] =>
    if (request.acceptedTypes.exists(_.toString() == MimeTypes.EVENT_STREAM)) {
      Ok("").as(ContentTypes.EVENT_STREAM)
    } else Ok("").as(ContentTypes.withCharset(Main.TsvMimeType))
  }

}

object Main {

  val TsvMimeType = "text/tab-separated-values"

}
