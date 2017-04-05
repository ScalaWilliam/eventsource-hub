package controllers

import java.time.Instant
import javax.inject._

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import model.ChannelId
import play.api.http.{ContentTypes, MimeTypes}
import play.api.libs.EventSource.Event
import play.api.mvc._
import services.ChannelStore

import scala.concurrent.ExecutionContext

/**
  * Forwards events from postChannel to aChannel via pushEvents.
  * Singleton needed to keep 'pushEvents' constant.
  */
@Singleton
class Main @Inject()(channelStore: ChannelStore)(
    implicit components: ControllerComponents,
    executionContext: ExecutionContext)
    extends AbstractController(components) { main =>

  def getChannel(channelId: ChannelId) = Action {
    request: Request[AnyContent] =>
      val atChannel = channelStore
        .AtChannel(channelId)
      if (request.acceptedTypes
            .exists(_.toString() == MimeTypes.EVENT_STREAM)) {
        val events = request.headers.get(Main.LastEventIdHeader) match {
          case Some(lastId) =>
            atChannel.fileStore
              .eventsFrom(lastId)
              .concat(atChannel.pushEvents)
          case None => atChannel.pushEvents
        }
        Ok.chunked(content = events.merge(Main.keepAliveEventSource))
          .as(ContentTypes.EVENT_STREAM)
      } else
        Ok.sendPath(content = atChannel.fileStore.eventsPath)
  }

  def postChannel(channelId: ChannelId): Action[String] =
    Action(parse.tolerantText) { request: Request[String] =>
      val id = Instant.now().toString
      val event = Event(
        name = request.getQueryString(Main.EventQueryParameterName),
        data = request.body,
        id = Some(id)
      )
      channelStore.AtChannel(channelId).push(event)
      Created(id)
    }

}

object Main {

  val LastEventIdHeader = "Last-Event-ID"

  val EventQueryParameterName = "event"

  val TsvMimeType = "text/tab-separated-values"

  /** Needed to prevent premature close of connection if not enough events coming through **/
  val keepAliveEventSource: Source[Event, Cancellable] = {
    import concurrent.duration._
    Source.tick(10.seconds, 10.seconds, Event(""))
  }

}
