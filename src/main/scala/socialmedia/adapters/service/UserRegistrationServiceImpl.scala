package socialmedia.adapters.service

import org.slf4j.{Logger, LoggerFactory}
import socialmedia.proto.{RegisterUserRequest, User, UserRegistrationService}

import scala.concurrent.Future

class UserRegistrationServiceImpl extends UserRegistrationService {
  private val log: Logger = LoggerFactory.getLogger(getClass)
  override def registerUser(request: RegisterUserRequest): Future[User] = {
    val user: User = User(request.name, request.email)
    log.info(s"Registering user with email {}", user.email)
    Future.successful(user)
  }
}
