package socialmedia.core.user

import akka.actor.typed.ActorSystem
import akka.projection.eventsourced.EventEnvelope
import akka.projection.jdbc.scaladsl.JdbcHandler
import socialmedia.adapter.repository.ScalikeJdbcSession
import socialmedia.adapter.repository.user.UserRepository
import socialmedia.core.user.UserEntity.UserRegistered
import socialmedia.model.Event

class UserProjectionHandler(tag: String, system: ActorSystem[_], repo: UserRepository) extends JdbcHandler[EventEnvelope[Event], ScalikeJdbcSession] {
  override def process(session: ScalikeJdbcSession, envelope: EventEnvelope[Event]): Unit = {
    envelope.event match {
      case UserRegistered(user) => repo.save(session, user)
    }
  }
}
