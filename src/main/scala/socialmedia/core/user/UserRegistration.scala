package socialmedia.core.user

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import org.slf4j.LoggerFactory
import socialmedia.core.{CborSerializable, Command, Event}

import scala.collection.mutable
import scala.collection.mutable.HashMap
import scala.concurrent.duration.DurationInt

object UserRegistration {
  private val log = LoggerFactory.getLogger(getClass)

  val EntityKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("UserRegistration")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContext =>
      UserRegistration(entityContext.entityId)
    })
  }

  def apply(userRegisterId: String): Behavior[Command] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, userRegisterId),
        emptyState = State.empty,
        commandHandler = (state, command) => handleCommand(state, command),
        eventHandler = (state, event) => handleEvent(state, event))
      .withRetention(RetentionCriteria
        .snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1)
      )
  }

  final case class State(users: mutable.HashMap[String, User], posts: mutable.HashMap[String, Post]) extends CborSerializable {

    def update(user: User): State = {
      users += (user.email -> user)
      this
    }
    def update(email: String, post: Post): State = {
      posts += (email -> post)
      this
    }
    def hasUser(email: String): Boolean = users.contains(email)

  }

  object State {
    val empty = State(users = mutable.HashMap.empty, posts = mutable.HashMap.empty)
  }

  final case class RegisterUser(user: User, replyTo: ActorRef[StatusReply[User]]) extends Command
  final case class PostPost(email: String, post: Post, replyTo: ActorRef[StatusReply[Post]]) extends Command
  final case class UserRegistered(user: User) extends Event
  final case class PostPosted(email: String, post: Post) extends Event

  private def handleCommand(state: State, command: Command): ReplyEffect[Event, State] = {
    command match {
      case RegisterUser(user, replyTo) => {
        if (state.hasUser(user.email)) Effect.reply(replyTo)(StatusReply.Error("Email already in use."))
        else Effect.persist(UserRegistered(user)).thenReply(replyTo) {
          state => StatusReply.success(state.users(user.email))
        }
      }
      case PostPost(email, post, replyTo) => {
        if (state.hasUser(email))
          Effect.persist(PostPosted(email, post)).thenReply(replyTo) {
            _ => StatusReply.success(post)
          }
        else Effect.reply(replyTo)(StatusReply.Error("User don't exist."))
      }
    }
  }

  private def handleEvent(state: State, event: Event): State = {
    event match {
      case UserRegistered(user) => state.update(user)
      case PostPosted(email, post) => state.update(email, post)
    }
  }
}
