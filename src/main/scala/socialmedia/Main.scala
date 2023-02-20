package socialmedia

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import org.slf4j.LoggerFactory
import socialmedia.adapters.service.{UserRegistrationServer, UserRegistrationServiceImpl}

import scala.util.control.NonFatal

object Main {

  val logger = LoggerFactory.getLogger("socialmedia.userregister.Main")

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "UserRegistrationService")
    try {
      init(system)
    } catch {
      case NonFatal(e) =>
        logger.error("Terminating due to initialization failure.", e)
        system.terminate()
    }
  }

  def init(system: ActorSystem[_]): Unit = {
    AkkaManagement(system).start()
    ClusterBootstrap(system).start()

    val grpcInterface =
      system.settings.config.getString("user-registering-service.grpc.interface")
    val grpcPort =
      system.settings.config.getInt("user-registering-service.grpc.port")
    val grpcService = new UserRegistrationServiceImpl
    UserRegistrationServer.start(
      grpcInterface,
      grpcPort,
      system,
      grpcService
    )
  }

}
