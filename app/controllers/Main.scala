package controllers

import java.time.Instant
import javax.inject._

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import play.api.http.{ContentTypes, MimeTypes}
import play.api.libs.EventSource.Event
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.streams.IterateeStreams
import play.api.mvc._
import services.FileStore

import scala.concurrent.ExecutionContext

/**
  * Forwards events from postChannel to aChannel via pushEvents.
  * Singleton needed to keep 'pushEvents' constant.
  */
@Singleton
class Main @Inject()(fileStore: FileStore)(
    implicit components: ControllerComponents,
    executionContext: ExecutionContext)
    extends AbstractController(components) { main =>

  def aChannel() = Action { request: Request[AnyContent] =>
    if (request.acceptedTypes.exists(_.toString() == MimeTypes.EVENT_STREAM)) {
      val events = request.headers.get(Main.LastEventIdHeader) match {
        case Some(lastId) => fileStore.eventsFrom(lastId).concat(pushEvents)
        case None => pushEvents
      }
      Ok.chunked(content = events.merge(Main.keepAliveEventSource))
        .as(ContentTypes.EVENT_STREAM)
    } else Ok.sendPath(content = fileStore.eventsPath)
  }

  private val (enumerator, channel) = Concurrent.broadcast[Event]

  private def pushEvents =
    Source.fromPublisher(IterateeStreams.enumeratorToPublisher(enumerator))

  def postChannel(): Action[String] = Action(parse.tolerantText) {
    request: Request[String] =>
      val id = Instant.now().toString
      val event = Event(
        name = request.getQueryString(Main.EventQueryParameterName),
        data = request.body,
        id = Some(id)
      )
      val eventLine =
        s"${event.id.getOrElse("")}\t${event.name.getOrElse("")}\t${event.data}"
      fileStore.appendLine(eventLine)
      channel.push(event)
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
