package socialmedia

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import org.slf4j.LoggerFactory
import socialmedia.adapter.repository.{FeedRepositoryImpl, ScalikeJdbcSetup}
import socialmedia.adapter.grpc.{UserRegistrationServer, UserRegistrationServiceImpl}
import socialmedia.core.{FeedProjection, UserRegistration}

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
    UserRegistration.init(system)
    ScalikeJdbcSetup.init(system)
    val feedRepository = new FeedRepositoryImpl()
    FeedProjection.init(system, feedRepository)
    val grpcInterface =
      system.settings.config.getString("user-registration-service.grpc.interface")
    val grpcPort =
      system.settings.config.getInt("user-registration-service.grpc.port")
    val grpcService = new UserRegistrationServiceImpl(system, feedRepository)
    UserRegistrationServer.start(
      grpcInterface,
      grpcPort,
      system,
      grpcService
    )
  }

}
