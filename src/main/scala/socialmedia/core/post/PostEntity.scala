package socialmedia.core.post

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import org.slf4j.LoggerFactory
import socialmedia.model.{CborSerializable, Command, Event, Post}

import java.time.ZonedDateTime
import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.util.Try

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

  final case class State(posts: mutable.HashMap[String, mutable.HashMap[String, Post]]) extends CborSerializable {
    def update(post: Post): State = {
      val authorPosts: mutable.HashMap[String, Post] = Try(posts(post.author)).getOrElse(mutable.HashMap.empty)
      authorPosts += (post.id -> post)
      posts += (post.author -> authorPosts)
      this
    }
  }

  object State {
    val empty = State(posts = mutable.HashMap.empty)
  }

  final case class CreatePost(content: String, image: String, author: String, replyTo: ActorRef[StatusReply[Post]]) extends Command
  final case class UpdatePost(id: String, content: String, image: String, author: String, replyTo: ActorRef[StatusReply[Post]]) extends Command
  final case class PostCreated(post: Post) extends Event
  final case class PostUpdated(post: Post) extends Event

  private def handleCommand(state: State, command: Command): ReplyEffect[Event, State] = {
    command match {
      case CreatePost(content, image, author , replyTo) =>
          val now = ZonedDateTime.now
          val postWithId = Post(id = author + " - " + now.toString(), content, image, now, author)
          Effect.persist(PostCreated(postWithId)).thenReply(replyTo) {
            _ => StatusReply.success(postWithId)
          }
      case UpdatePost(id, content, image, author, replyTo) =>
        val post: Post = state.posts(author)(id)
          val updatePost = post.copy(
            content = if (content == null || content.isBlank || content.isEmpty) post.content else content,
            image = if (image == null || image.isBlank || image.isEmpty) post.image else image,
          )
          Effect.persist(PostUpdated(updatePost)).thenReply(replyTo) {
            _ => StatusReply.success(updatePost)
          }
    }
  }

  private def handleEvent(state: State, event: Event): State = {
    event match {
      case PostCreated(post) => state.update(post)
      case PostUpdated(post) => state.update(post)
    }
  }
}
