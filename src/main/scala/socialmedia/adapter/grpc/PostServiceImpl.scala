package socialmedia.adapter.grpc

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.grpc.GrpcServiceException
import akka.http.scaladsl.model.DateTime
import akka.stream.scaladsl.Source
import akka.util.Timeout
import io.grpc.Status
import org.slf4j.{Logger, LoggerFactory}
import socialmedia.adapter.repository.ScalikeJdbcSession
import socialmedia.adapter.repository.post.PostRepository
import socialmedia.core.post.PostEntity
import socialmedia.model.PostMapper
import socialmedia.proto._

import scala.concurrent.Future

class PostServiceImpl(system: ActorSystem[_], feedRepository: PostRepository, userService: UserServiceImpl) extends PostService with ErrorConverter {
  import system.executionContext

  private val log: Logger = LoggerFactory.getLogger(getClass)

  private val sharding = ClusterSharding(system)

  implicit private val timeout: Timeout =
    Timeout.create(
      system.settings.config.getDuration("social-media.ask-timeout"))

  override def postPost(in: PostPostRequest): Future[Post] = {
    in.post match {
      case Some(protoPost) => {
        if (userService.exists(protoPost.author)) {
          log.info(s"Posting for user with email {}", protoPost.author)
          val post: socialmedia.model.Post = PostMapper.toModel(protoPost, DateTime.now)
          val entityRef = sharding.entityRefFor(PostEntity.EntityKey, post.author.hashCode.toString)
          val reply: Future[socialmedia.model.Post] = entityRef.askWithStatus(PostEntity.PostPost(post, _))
          val response: Future[socialmedia.proto.Post] = reply.map(PostMapper.toProto)
          convertError(response)(system)
        } else {
          Future.failed(
            new GrpcServiceException(
              Status.INVALID_ARGUMENT.withDescription("Author don't exists")))        }
      }
    }
  }

  override def getFeed(in: socialmedia.proto.GetFeedRequest): Source[Post, NotUsed] = {
    in.author match {
      case Some(author) => Source(
        ScalikeJdbcSession.withSession {
          session => feedRepository.getPostsByAuthor(session, author)
        }.map(PostMapper.toProto)
      )
      case None => Source(
        ScalikeJdbcSession.withSession {
          session => feedRepository.getPosts(session)
        }.map(PostMapper.toProto)
      )
    }
  }
}
