package socialmedia.model

import socialmedia.core.CborSerializable

case class Post(content: String, image: String, date: String, author: String) extends CborSerializable
