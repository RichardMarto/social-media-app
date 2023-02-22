package socialmedia.model
import akka.http.scaladsl.model.DateTime
import socialmedia.proto

object PostMapper extends Mapper[Post, proto.Post] {
  override def toModel(post: proto.Post): Post = {
    post.date match {
      case Some(date) => Post(post.id, post.content, post.image, date.toString, post.author)
    }
  }

  def toModel(post: proto.Post, date: DateTime): Post = {
    Post(post.id, post.content, post.image, date.toString(), post.author)
  }

  override def toProto(post: Post): proto.Post = {
    DateTime.fromIsoDateTimeString(post.date) match {
      case Some(date) => proto.Post(
        post.id, post.content, post.image, Some(proto.Date(date.day, date.month, date.year)), post.author
      )
    }
  }
}
