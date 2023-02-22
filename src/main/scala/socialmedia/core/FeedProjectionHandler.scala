package socialmedia.core

import akka.actor.typed.ActorSystem
import akka.projection.eventsourced.EventEnvelope
import akka.projection.jdbc.scaladsl.JdbcHandler
import socialmedia.adapter.repository.{FeedRepository, ScalikeJdbcSession}
import UserRegistration.{PostPosted, UserRegistered}

class FeedProjectionHandler(tag: String, system: ActorSystem[_], repo: FeedRepository) extends JdbcHandler[EventEnvelope[Event], ScalikeJdbcSession] {
  override def process(session: ScalikeJdbcSession, envelope: EventEnvelope[Event]): Unit = {
    envelope.event match {
      case UserRegistered(user) => {
        //TODO: Materialize user registrations
      }
      case PostPosted(post) =>
        repo.post(session, post)
    }
  }
}
