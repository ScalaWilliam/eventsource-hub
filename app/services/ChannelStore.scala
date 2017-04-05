package services

import javax.inject.{Inject, Singleton}

import akka.NotUsed
import akka.stream.scaladsl.Source
import model.ChannelId
import play.api.libs.EventSource.Event
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.streams.IterateeStreams

/**
  * Created by me on 06/04/2017.
  */
@Singleton
class ChannelStore @Inject()() { cs =>

  private val (enumerator, channel) = Concurrent.broadcast[Event]

  private def pushEvents =
    Source.fromPublisher(IterateeStreams.enumeratorToPublisher(enumerator))

  case class AtChannel(channelId: ChannelId) {
    def push(event: Event): Unit = {
      val eventLine =
        s"${event.id.getOrElse("")}\t${event.name.getOrElse("")}\t${event.data}"
      fileStore.appendLine(eventLine)
      channel.push(event)
    }
    def fileStore: FileStore = FileStore.fromChannelId(channelId)
    def pushEvents: Source[Event, NotUsed] = cs.pushEvents
  }

}
