package socialmedia

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import org.slf4j.LoggerFactory
import socialmedia.adapter.grpc.GrpcServer
import socialmedia.adapter.grpc.post.PostServiceImpl
import socialmedia.adapter.grpc.user.UserServiceImpl
import socialmedia.adapter.repository.ScalikeJdbcSetup
import socialmedia.adapter.repository.post.PostRepositoryImpl
import socialmedia.adapter.repository.user.UserRepositoryImpl
import socialmedia.core.post.{PostEntity, PostProjection}
import socialmedia.core.user.{UserEntity, UserProjection}

import scala.util.control.NonFatal

object Main {

  val logger = LoggerFactory.getLogger("socialmedia.Main")

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "SocialMediaService")
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
    UserEntity.init(system)
    PostEntity.init(system)
    ScalikeJdbcSetup.init(system)
    val userRepository = new UserRepositoryImpl()
    val postRepository = new PostRepositoryImpl()
    UserProjection.init(system, userRepository)
    PostProjection.init(system, postRepository)
    val grpcInterface = system.settings.config.getString("social-media.grpc.interface")
    val grpcPort = system.settings.config.getInt("social-media.grpc.port")
    val userService = new UserServiceImpl(system, userRepository)
    val postService = new PostServiceImpl(system, postRepository, userService)
    GrpcServer.start(
      grpcInterface,
      grpcPort,
      system,
      userService,
      postService
    )
  }

}
