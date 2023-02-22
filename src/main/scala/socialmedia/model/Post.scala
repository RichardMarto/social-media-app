package socialmedia.model

import java.time.ZonedDateTime

case class Post(id: Option[String], content: String, image: String, date: ZonedDateTime, author: String) extends CborSerializable
