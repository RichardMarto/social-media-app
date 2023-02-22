package socialmedia.core.post

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import org.slf4j.LoggerFactory
import socialmedia.model.{CborSerializable, Command, Event, Post}

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

object PostEntity {
  private val log = LoggerFactory.getLogger(getClass)

  val EntityKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("PostEntity")

  val tags = Vector.tabulate(5)(i => s"post-$i")

  def init(system: ActorSystem[_]): Unit = {
    val behaviorFactory: EntityContext[Command] => Behavior[Command] = {
      entityContext =>
        val i = math.abs(entityContext.entityId.hashCode % tags.size)
        val selectedTag = tags(i)
        PostEntity(entityContext.entityId, selectedTag)
    }
    ClusterSharding(system).init(Entity(EntityKey)(behaviorFactory))
  }

  def apply(entityId: String, projectionTag: String): Behavior[Command] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, entityId),
        emptyState = State.empty,
        commandHandler = (state, command) => handleCommand(state, command),
        eventHandler = (state, event) => handleEvent(state, event))
      .withTagger(_ => Set(projectionTag))
      .withRetention(RetentionCriteria
        .snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1)
      )
  }

  final case class State(posts: mutable.HashMap[String, Post]) extends CborSerializable {
    def update(post: Post): State = {
      posts += (post.author -> post)
      this
    }
  }

  object State {
    val empty = State(posts = mutable.HashMap.empty)
  }

  final case class PostPost(post: Post, replyTo: ActorRef[StatusReply[Post]]) extends Command
  final case class PostPosted(post: Post) extends Event

  private def handleCommand(state: State, command: Command): ReplyEffect[Event, State] = {
    command match {
      case PostPost(post, replyTo) =>
          Effect.persist(PostPosted(post)).thenReply(replyTo) {
            _ => StatusReply.success(post)
          }
    }
  }

  private def handleEvent(state: State, event: Event): State = {
    event match {
      case PostPosted(post) => state.update(post)
    }
  }
}
