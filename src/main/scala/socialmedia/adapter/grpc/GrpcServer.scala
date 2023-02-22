package socialmedia.adapter.grpc

import akka.actor.typed.ActorSystem
import akka.grpc.scaladsl.{ServerReflection, ServiceHandler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.slf4j.{Logger, LoggerFactory}
import socialmedia.proto.{PostService, PostServiceHandler, UserService, UserServiceHandler}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object GrpcServer {
  private val log: Logger = LoggerFactory.getLogger(getClass)
  def start(interface: String, port: Int, system: ActorSystem[_], userService: UserService, postService: PostService): Unit = {
    implicit val sys: ActorSystem[_] = system
    implicit val executionContext: ExecutionContext = system.executionContext

    val service: HttpRequest => Future[HttpResponse] = ServiceHandler.concatOrNotFound(
      UserServiceHandler.partial(userService),
      PostServiceHandler.partial(postService),
      ServerReflection.partial(List(UserService, PostService))
    )

    val bound = Http().newServerAt(interface, port).bind(service).map(_.addToCoordinatedShutdown(3.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        log.info("GRPC server is running at gRPC server {}:{}",
          address.getHostString,
          address.getPort)
      case Failure(ex) =>
        log.error("Failed to bind gRPC endpoint, terminating system", ex)
        system.terminate()
    }

  }

}
