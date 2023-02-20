package socialmedia.user

import socialmedia.CborSerializable

case class User(name: String, email: String) extends CborSerializable
