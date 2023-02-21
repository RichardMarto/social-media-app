package socialmedia.core

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.{ProjectionBehavior, ProjectionId}
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.JdbcProjection
import akka.projection.scaladsl.{ExactlyOnceProjection, SourceProvider}
import socialmedia.adapters.repository.{FeedRepository, ScalikeJdbcSession}

object FeedProjection {
  def init(system: ActorSystem[_], repository: FeedRepository): Unit = {
    ShardedDaemonProcess(system).init(
      name = "FeedProjection",
      UserRegistration.tags.size,
      index => ProjectionBehavior(createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop)
    )
  }

  private def createProjectionFor(
                                   system: ActorSystem[_],
                                   repository: FeedRepository,
                                   index: Int
                                 ): ExactlyOnceProjection[Offset, EventEnvelope[Event]] = {
    val tag = UserRegistration.tags(index)

    val sourceProvider
    : SourceProvider[Offset, EventEnvelope[Event]] =
      EventSourcedProvider.eventsByTag[Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag)

    JdbcProjection.exactlyOnce(
      projectionId = ProjectionId("FeedProjection", tag),
      sourceProvider,
      handler = () =>
        new FeedProjectionHandler(tag, system, repository),
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }
}
