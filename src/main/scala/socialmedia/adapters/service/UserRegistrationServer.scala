package socialmedia.adapters.service

import akka.actor.typed.ActorSystem
import akka.grpc.scaladsl.{ServerReflection, ServiceHandler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.slf4j.{Logger, LoggerFactory}
import socialmedia.proto.{UserRegistrationService, UserRegistrationServiceHandler}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object UserRegistrationServer {
  private val log: Logger = LoggerFactory.getLogger(getClass)
  def start(interface: String, port: Int, system: ActorSystem[_], userRegistrationService: UserRegistrationService): Unit = {
    implicit val sys: ActorSystem[_] = system
    implicit val executionContext: ExecutionContext = system.executionContext

    val service: HttpRequest => Future[HttpResponse] = ServiceHandler.concatOrNotFound(
      UserRegistrationServiceHandler.partial(userRegistrationService),
      ServerReflection.partial(List(UserRegistrationService))
    )

    val bound = Http().newServerAt(interface, port).bind(service).map(_.addToCoordinatedShutdown(3.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        log.info("UserRegistrationService is running at gRPC server {}:{}",
          address.getHostString,
          address.getPort)
      case Failure(ex) =>
        log.error("Failed to bind gRPC endpoint, terminating system", ex)
        system.terminate()
    }

  }

}
