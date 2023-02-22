package socialmedia.model.mappers.impl

import socialmedia.model.User
import socialmedia.model.mappers.Mapper
import socialmedia.proto

object UserMapper extends Mapper[User, proto.User] {
  override def toModel(user: proto.User): User = {
    User(user.name, user.email)
  }

  override def toProto(user: User): proto.User = {
    proto.User(user.name, user.name)
  }
}
