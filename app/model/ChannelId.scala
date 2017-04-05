package model

import play.api.mvc.PathBindable

/**
  * Created by me on 06/04/2017.
  */
case class ChannelId(value: String) {}

object ChannelId {
  private val requiredRegex = """^[A-Za-z0-9_-]{4,64}$""".r
  implicit val pathParser: PathBindable[ChannelId] =
    new PathBindable[ChannelId] {
      override def bind(key: String,
                        value: String): Either[String, ChannelId] =
        value match {
          case requiredRegex(_ @_ *) => Right(ChannelId(value))
          case _ => Left("Channel ID not matching expected format")
        }

      override def unbind(key: String, value: ChannelId): String =
        implicitly[PathBindable[String]].unbind(key, value.value)
    }
}
