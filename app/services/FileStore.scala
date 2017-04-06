package services

import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString
import model.ChannelId
import play.api.libs.EventSource.Event

import scala.concurrent.Future

/**
  * Created by me on 05/04/2017.
  */
class FileStore(val eventsPath: Path) {

  Files.createDirectories(eventsPath.getParent)

  if (!Files.exists(eventsPath)) {
    Files.createFile(eventsPath)
  }


  private val raf = new java.io.RandomAccessFile(eventsPath.toFile, "rw").getChannel
  private val lock = raf.lock()

  /**
    * Typically used to achieve the Last-Event-ID part of the EventSource spec
    * that allows consumers to resume from where they left off in case of a failure
    */
  def eventsFrom(id: String): Source[Event, Future[IOResult]] = {
    FileIO
      .fromPath(eventsPath)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024))
      .map(_.utf8String)
      .map(_.split('\t').toList)
      .collect {
        case eventId :: tpe :: rest if eventId > id =>
          Event(
            id = Some(eventId),
            data = rest.mkString("\t"),
            name = Some(tpe).filter(_.nonEmpty)
          )
      }
  }

  /** Use approach from https://gist.github.com/ScalaWilliam/37c4ef3c41e656a6d3c02d992f5c6191 **/
  def appendLine(line: String): Unit = {
    val lineBytes = (line + "\n").getBytes("UTF-8")
    Files.write(eventsPath, lineBytes, StandardOpenOption.APPEND)
  }

  def close(): Unit = {
    lock.release()
  }

}

object FileStore {

  def fromChannelId(channelId: ChannelId): FileStore = {
    new FileStore(Paths.get(s"events/${channelId.value}.tsv"))
  }

}
