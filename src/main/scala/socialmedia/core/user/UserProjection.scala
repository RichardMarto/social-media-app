package socialmedia.core.user

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.JdbcProjection
import akka.projection.scaladsl.{ExactlyOnceProjection, SourceProvider}
import akka.projection.{ProjectionBehavior, ProjectionId}
import socialmedia.adapter.repository.ScalikeJdbcSession
import socialmedia.adapter.repository.user.UserRepository
import socialmedia.model.Event

object UserProjection {
  def init(system: ActorSystem[_], repository: UserRepository): Unit = {
    ShardedDaemonProcess(system).init(
      name = "UserProjection",
      UserEntity.tags.size,
      index => ProjectionBehavior(createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop)
    )
  }

  private def createProjectionFor(
                                   system: ActorSystem[_],
                                   repository: UserRepository,
                                   index: Int
                                 ): ExactlyOnceProjection[Offset, EventEnvelope[Event]] = {
    val tag = UserEntity.tags(index)

    val sourceProvider
    : SourceProvider[Offset, EventEnvelope[Event]] =
      EventSourcedProvider.eventsByTag[Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag)

    JdbcProjection.exactlyOnce(
      projectionId = ProjectionId("UserProjection", tag),
      sourceProvider,
      handler = () =>
        new UserProjectionHandler(tag, system, repository),
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }
}
