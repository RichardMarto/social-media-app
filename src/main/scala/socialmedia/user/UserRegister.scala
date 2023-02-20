package socialmedia.user

import akka.persistence.typed.scaladsl.Effect
import org.slf4j.LoggerFactory
import socialmedia.{CborSerializable, Command, Event}

import scala.collection.immutable.HashMap

object UserRegister {
  private val log = LoggerFactory.getLogger(getClass)

  final case class State(users: HashMap[String, User]) extends CborSerializable {

    def registerOrUpdate(user: User): Unit = {
      users += (user.email, user.name)
    }
  }

  object State {
    val empty = State(users = HashMap.empty)
  }

  final case class RegisterUser(user: User) extends Command
  final case class UserRegistered(user: User) extends Event

  def handleCommand(command: Command): Unit = {
    command match {
      case RegisterUser(_) => Effect.persist(UserRegistered(_))
    }
  }

  def handleEvent(event: Event, state: State): Unit = {
    event match {
      case UserRegistered(_) => state.registerOrUpdate(_)
    }
  }
}
