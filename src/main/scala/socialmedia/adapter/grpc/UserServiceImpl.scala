package socialmedia.adapter.grpc

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import org.slf4j.{Logger, LoggerFactory}
import socialmedia.core.UserRegistration
import socialmedia.proto.{RegisterUserRequest, User, UserService}

import scala.concurrent.Future

class UserServiceImpl(system: ActorSystem[_]) extends UserService with ErrorConverter {
  import system.executionContext

  private val log: Logger = LoggerFactory.getLogger(getClass)

  private val sharding = ClusterSharding(system)

  implicit private val timeout: Timeout =
  Timeout.create(
  system.settings.config.getDuration("social-media.ask-timeout"))

  override def registerUser(request: RegisterUserRequest): Future[User] = {
    val user: User = User(request.name, request.email)
    log.info(s"Registering user with email {}", user.email)
    val entityRef = sharding.entityRefFor(UserRegistration.EntityKey, request.email.substring(0, 2).hashCode.toString)
    val reply: Future[socialmedia.model.User] = entityRef.askWithStatus(UserRegistration.RegisterUser(socialmedia.model.User(user.name, user.email), _))
    val response: Future[socialmedia.proto.User] = reply.map(u => socialmedia.proto.User(u.name, user.email))
    convertError(response)(system)
  }
}
