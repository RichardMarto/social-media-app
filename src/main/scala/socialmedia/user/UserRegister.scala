package socialmedia.user

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.{PersistenceId}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import org.slf4j.LoggerFactory
import socialmedia.{CborSerializable, Command, Event}

import scala.collection.mutable.HashMap
import scala.concurrent.duration.DurationInt

object UserRegister {
  private val log = LoggerFactory.getLogger(getClass)

  val EntityKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("UserRegister")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContext =>
      UserRegister(entityContext.entityId)
    })
  }

  def apply(userRegisterId: String): Behavior[Command] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, userRegisterId),
        emptyState = State.empty,
        commandHandler = (state, command) => handleCommand(userRegisterId, state, command),
        eventHandler = (state, event) => handleEvent(state, event))
      .withRetention(RetentionCriteria
        .snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1)
      )
  }

  final case class State(users: HashMap[String, User]) extends CborSerializable {

    def update(user: User): State = {
      users += (user.email -> user)
      this
    }

    def hasUser(user: User): Boolean = users.contains(user.email)
  }

  object State {
    val empty = State(users = HashMap.empty)
  }

  final case class RegisterUser(user: User, replyTo: ActorRef[StatusReply[User]]) extends Command
  final case class UserRegistered(userRegisterId:String, user: User) extends Event

  private def handleCommand(userRegisterId: String, state: State, command: Command): ReplyEffect[Event, State] = {
    command match {
      case RegisterUser(user, replyTo) => {
        if (state.hasUser(user)) Effect.reply(replyTo)(StatusReply.Error("Email already in use."))
        else Effect.persist(UserRegistered(userRegisterId, user)).thenReply(replyTo) {
          state => StatusReply.success(state.users(user.email))
        }
      }
    }
  }

  private def handleEvent(state: State, event: Event): State = {
    event match {
      case UserRegistered(_, user) => state.update(user)
    }
  }
}
