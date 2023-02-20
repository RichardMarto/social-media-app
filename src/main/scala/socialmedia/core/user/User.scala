package socialmedia.core.user

import socialmedia.core.CborSerializable

case class User(name: String, email: String) extends CborSerializable
case class Post(content: String, image: String, date: String, author: String) extends CborSerializable
