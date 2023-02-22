package socialmedia.core.user

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import org.slf4j.LoggerFactory
import socialmedia.model.{CborSerializable, Command, Event, User}

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

object UserEntity {
  private val log = LoggerFactory.getLogger(getClass)

  val EntityKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("UserEntity")

  val tags = Vector.tabulate(5)(i => s"user-$i")

  def init(system: ActorSystem[_]): Unit = {
    val behaviorFactory: EntityContext[Command] => Behavior[Command] = {
      entityContext =>
        val i = math.abs(entityContext.entityId.hashCode % tags.size)
        val selectedTag = tags(i)
        UserEntity(entityContext.entityId, selectedTag)
    }
    ClusterSharding(system).init(Entity(EntityKey)(behaviorFactory))
  }

  def apply(userRegisterId: String, projectionTag: String): Behavior[Command] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, userRegisterId),
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

  final case class State(users: mutable.HashMap[String, User]) extends CborSerializable {

    def update(user: User): State = {
      users += (user.email -> user)
      this
    }
    def hasUser(email: String): Boolean =
      users.contains(email)
  }

  object State {
    val empty = State(users = mutable.HashMap.empty)
  }

  final case class RegisterUser(user: User, replyTo: ActorRef[StatusReply[User]]) extends Command
  final case class UserRegistered(user: User) extends Event

  private def handleCommand(state: State, command: Command): ReplyEffect[Event, State] = {
    command match {
      case RegisterUser(user, replyTo) =>
        if (state.hasUser(user.email)) Effect.reply(replyTo)(StatusReply.Error("Email already in use."))
        else Effect.persist(UserRegistered(user)).thenReply(replyTo) {
          state => StatusReply.success(state.users(user.email))
        }
    }
  }

  private def handleEvent(state: State, event: Event): State = {
    event match {
      case UserRegistered(user) => state.update(user)
    }
  }
}
