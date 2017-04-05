package services

import javax.inject.{Inject, Singleton}

import akka.NotUsed
import akka.agent.Agent
import akka.stream.scaladsl.Source
import model.ChannelId
import play.api.libs.EventSource.Event
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.streams.IterateeStreams

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by me on 06/04/2017.
  */
@Singleton
class ChannelStore @Inject()()(implicit executionContext: ExecutionContext) {
  cs =>

  type Channel = (FileStore, Source[Event, NotUsed], Concurrent.Channel[Event])
  private val channelsData = Agent(Map.empty[ChannelId, Channel])

  def getChannel(channelId: ChannelId): Future[Channel] = {
    channelsData
      .alter { map =>
        map.get(channelId) match {
          case None =>
            map.updated(
              key = channelId,
              value = {
                val (enumerator, channel) = Concurrent.broadcast[Event]
                val fileStore = FileStore.fromChannelId(channelId)
                val source = Source.fromPublisher(
                  IterateeStreams.enumeratorToPublisher(enumerator))
                (fileStore, source, channel)
              }
            )
          case _ => map
        }
      }
      .map(_(channelId))
  }

  case class AtChannel(channelId: ChannelId) {
    def push(event: Event): Future[Unit] = {
      val eventLine =
        s"${event.id.getOrElse("")}\t${event.name.getOrElse("")}\t${event.data}"
      getChannel(channelId).map {
        case (fileStore, _, pushChannel) =>
          fileStore.appendLine(eventLine)
          pushChannel.push(event)
      }
    }

    def fileStore: Future[FileStore] = getChannel(channelId).map {
      case (fileStore, _, _) => fileStore
    }

    def pushEvents: Future[Source[Event, NotUsed]] =
      getChannel(channelId).map {
        case (_, source, _) => source
      }
  }

}
