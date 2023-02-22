package socialmedia.model.mappers.impl

import socialmedia.model.Post
import socialmedia.model.mappers.Mapper
import socialmedia.proto

object PostMapper extends Mapper[Post, proto.Post] {
  override def toModel(post: proto.Post): Post = ???

  override def toProto(post: Post): proto.Post = {
    proto.Post(
      post.id, post.content, post.image, Some(proto.Date(post.date.getDayOfYear, post.date.getMonthValue, post.date.getYear)), post.author
    )
  }
}
