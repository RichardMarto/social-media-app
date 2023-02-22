package socialmedia.model

import socialmedia.core.CborSerializable

case class Post(id: Long, content: String, image: String, date: String, author: String) extends CborSerializable
