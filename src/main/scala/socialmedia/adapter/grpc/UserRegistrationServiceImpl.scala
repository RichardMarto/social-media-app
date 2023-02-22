package socialmedia.adapter.grpc

import akka.actor.typed.{ActorSystem, DispatcherSelector}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.grpc.GrpcServiceException
import akka.http.scaladsl.model.DateTime
import akka.util.Timeout
import io.grpc.Status
import org.slf4j.{Logger, LoggerFactory}
import socialmedia.adapter.repository.{FeedRepository, ScalikeJdbcSession}
import socialmedia.core.UserRegistration
import socialmedia.proto.{Feed, GetFeedFromRequest, Post, PostPostRequest, RegisterUserRequest, User, UserRegistrationService}

import scala.concurrent.{ExecutionContext, Future, TimeoutException}

class UserRegistrationServiceImpl(system: ActorSystem[_], feedRepository: FeedRepository) extends UserRegistrationService {
  import system.executionContext

  private val log: Logger = LoggerFactory.getLogger(getClass)
  private val sharding = ClusterSharding(system)

  private val blockingJdbcExecutor: ExecutionContext =
    system.dispatchers.lookup(
      DispatcherSelector
        .fromConfig("akka.projection.jdbc.blocking-jdbc-dispatcher")
    )

  implicit private val timeout: Timeout =
  Timeout.create(
  system.settings.config.getDuration("user-registration-grpc.ask-timeout"))

  override def registerUser(request: RegisterUserRequest): Future[User] = {
    val user: User = User(request.name, request.email)
    log.info(s"Registering user with email {}", user.email)
    val entityRef = sharding.entityRefFor(UserRegistration.EntityKey, request.email.substring(0, 2).hashCode.toString)
    val reply: Future[socialmedia.model.User] = entityRef.askWithStatus(UserRegistration.RegisterUser(socialmedia.model.User(user.name, user.email), _))
    val response: Future[socialmedia.proto.User] = reply.map(u => socialmedia.proto.User(u.name, user.email))
    convertError(response)
  }

  override def postPost(request: PostPostRequest): Future[Post] = {
    log.info(s"Posting for user with email {}", request.author)
    val post: socialmedia.model.Post = socialmedia.model.Post(request.content, request.image, DateTime.now.toString(), request.author)
    val entityRef = sharding.entityRefFor(UserRegistration.EntityKey, request.author.hashCode.toString)
    val reply: Future[socialmedia.model.Post] = entityRef.askWithStatus(UserRegistration.PostPost(request.author, post, _))
    val response: Future[socialmedia.proto.Post] = reply.map(u => socialmedia.proto.Post(post.content, post.image, post.date, post.author))
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

  override def getFeedFrom(request: GetFeedFromRequest): Future[Feed] = {
    Future {
      ScalikeJdbcSession.withSession {
        session => feedRepository.getPostsByAuthorEmail(session, request.email)
      }.collect(post => Post(post.content, post.image, post.date, post.author))
    }.map(a => Feed(a))
  }
}
