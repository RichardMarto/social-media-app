package socialmedia.core.post

import akka.actor.typed.ActorSystem
import akka.projection.eventsourced.EventEnvelope
import akka.projection.jdbc.scaladsl.JdbcHandler
import socialmedia.adapter.repository.ScalikeJdbcSession
import socialmedia.adapter.repository.post.PostRepository
import socialmedia.core.post.PostEntity.PostPosted
import socialmedia.model.Event

class PostProjectionHandler(tag: String, system: ActorSystem[_], repo: PostRepository) extends JdbcHandler[EventEnvelope[Event], ScalikeJdbcSession] {
  override def process(session: ScalikeJdbcSession, envelope: EventEnvelope[Event]): Unit = {
    envelope.event match {
      case PostPosted(post) =>
        repo.save(session, post)
    }
  }
}
