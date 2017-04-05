package controllers

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.time.Instant
import javax.inject._

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import play.api.http.{ContentTypes, MimeTypes}
import play.api.libs.EventSource.Event
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.streams.IterateeStreams
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
  * Forwards events from postChannel to aChannel via pushEvents.
  * Singleton needed to keep 'pushEvents' constant.
  */
@Singleton
class Main(eventsPath: Path)(implicit components: ControllerComponents,
                             executionContext: ExecutionContext)
    extends AbstractController(components) { main =>

  @Inject
  def this()(implicit components: ControllerComponents,
             executionContext: ExecutionContext) =
    this(Paths.get(Main.DefaultFile))

  def aChannel() = Action { request: Request[AnyContent] =>
    if (request.acceptedTypes.exists(_.toString() == MimeTypes.EVENT_STREAM)) {
      Ok.chunked(content = pushEvents.merge(Main.keepAliveEventSource))
        .as(ContentTypes.EVENT_STREAM)
    } else Ok.sendPath(content = eventsPath)
  }

  private val (enumerator, channel) = Concurrent.broadcast[Event]

  private def pushEvents =
    Source.fromPublisher(IterateeStreams.enumeratorToPublisher(enumerator))

  Files.createDirectories(eventsPath.getParent)

  /** Use approach from https://gist.github.com/ScalaWilliam/37c4ef3c41e656a6d3c02d992f5c6191 **/
  private val raf =
    new java.io.RandomAccessFile(eventsPath.toFile, "rw").getChannel

  def appendLine(line: String): Unit = {
    val lock = raf.lock()
    val lineBytes = (line + "\n").getBytes("UTF-8")
    try Files.write(eventsPath, lineBytes, StandardOpenOption.APPEND)
    finally lock.release()
  }

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
      appendLine(eventLine)
      channel.push(event)
      Created(id)
  }

}

object Main {

  val DefaultFile = "events/aChannel.log"

  val EventQueryParameterName = "event"

  val TsvMimeType = "text/tab-separated-values"

  /** Needed to prevent premature close of connection if not enough events coming through **/
  val keepAliveEventSource: Source[Event, Cancellable] = {
    import concurrent.duration._
    Source.tick(10.seconds, 10.seconds, Event(""))
  }

}
