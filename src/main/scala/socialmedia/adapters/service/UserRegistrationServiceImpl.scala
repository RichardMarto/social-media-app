package socialmedia.adapters.service

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.grpc.GrpcServiceException
import akka.util.Timeout
import io.grpc.Status
import org.slf4j.{Logger, LoggerFactory}
import socialmedia.core.user.{UserRegistration}
import socialmedia.proto.{RegisterUserRequest, User, UserRegistrationService}

import scala.concurrent.{Future, TimeoutException}

class UserRegistrationServiceImpl(system: ActorSystem[_]) extends UserRegistrationService {
  import system.executionContext

  private val log: Logger = LoggerFactory.getLogger(getClass)

  implicit private val timeout: Timeout =
    Timeout.create(
      system.settings.config.getDuration("user-registration-service.ask-timeout"))

  private val sharding = ClusterSharding(system)

  override def registerUser(request: RegisterUserRequest): Future[User] = {
    val user: User = User(request.name, request.email)
    log.info(s"Registering user with email {}", user.email)
    val entityRef = sharding.entityRefFor(UserRegistration.EntityKey, request.email.substring(0, 2).hashCode.toString)
    val reply: Future[socialmedia.core.user.User] = entityRef.askWithStatus(UserRegistration.RegisterUser(socialmedia.core.user.User(user.name, user.email), _))
    val response: Future[socialmedia.proto.User] = reply.map(u => socialmedia.proto.User(u.name, user.email))
    convertError(response)
  }

  private def convertError[T](response: Future[T]): Future[T] = {
    response.recoverWith {
      case _: TimeoutException =>
        Future.failed(
          new GrpcServiceException(
            Status.UNAVAILABLE.withDescription("Operation timed out")))
      case exc =>
        Future.failed(
          new GrpcServiceException(
            Status.INVALID_ARGUMENT.withDescription(exc.getMessage)))
    }
  }
}
