package socialmedia.model

case class Post(id: Long, content: String, image: String, date: String, author: String) extends CborSerializable
