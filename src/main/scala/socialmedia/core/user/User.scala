package socialmedia.core.user

import socialmedia.core.CborSerializable

case class User(name: String, email: String) extends CborSerializable
