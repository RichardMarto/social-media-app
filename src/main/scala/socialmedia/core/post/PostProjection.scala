package socialmedia.core.post

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
import socialmedia.adapter.repository.post.PostRepository
import socialmedia.model.Event

object PostProjection {
  def init(system: ActorSystem[_], repository: PostRepository): Unit = {
    ShardedDaemonProcess(system).init(
      name = "PostProjection",
      PostEntity.tags.size,
      index => ProjectionBehavior(createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop)
    )
  }

  private def createProjectionFor(
                                   system: ActorSystem[_],
                                   repository: PostRepository,
                                   index: Int
                                 ): ExactlyOnceProjection[Offset, EventEnvelope[Event]] = {
    val tag = PostEntity.tags(index)

    val sourceProvider
    : SourceProvider[Offset, EventEnvelope[Event]] =
      EventSourcedProvider.eventsByTag[Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag)

    JdbcProjection.exactlyOnce(
      projectionId = ProjectionId("PostProjection", tag),
      sourceProvider,
      handler = () =>
        new PostProjectionHandler(tag, system, repository),
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }
}
