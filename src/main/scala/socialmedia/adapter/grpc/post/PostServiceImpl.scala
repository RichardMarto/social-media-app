package socialmedia.adapter.grpc.post

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.scaladsl.Source
import akka.util.Timeout
import org.slf4j.{Logger, LoggerFactory}
import socialmedia.adapter.grpc.ErrorConverter
import socialmedia.adapter.grpc.user.UserServiceImpl
import socialmedia.adapter.repository.ScalikeJdbcSession
import socialmedia.adapter.repository.post.PostRepository
import socialmedia.core.post.PostEntity
import socialmedia.model
import socialmedia.model.mappers.impl.PostMapper
import socialmedia.proto._

import scala.concurrent.Future

class PostServiceImpl(
                       system: ActorSystem[_],
                       feedRepository: PostRepository,
                       userService: UserServiceImpl
                     ) extends PostService with ErrorConverter {
  import system.executionContext

  private val log: Logger = LoggerFactory.getLogger(getClass)

  private val sharding = ClusterSharding(system)

  implicit private val timeout: Timeout =
    Timeout.create(
      system.settings.config.getDuration("social-media.ask-timeout"))

  override def postPost(in: PostPostRequest): Future[Post] = {
    userService.getByEmail(in.author) match {
      case Some(author) => {
        log.info(s"Posting for user with email, {}.", in.author)
        val entityRef = sharding.entityRefFor(PostEntity.EntityKey, in.author.hashCode.toString)
        val reply: Future[model.Post] = entityRef.askWithStatus(PostEntity.CreatePost(in.content, in.image, in.author, _))
        val response: Future[Post] = reply.map(PostMapper.toProto)
        convertError(response)(system)
      }
    }
  }

  override def updatePost(in: UpdatePostRequest): Future[Post] = {
    userService.getByEmail(in.author) match {
      case Some(author) => {
        log.info(s"Updating post, {}.", in.id)
        val entityRef = sharding.entityRefFor(PostEntity.EntityKey, in.author.hashCode.toString)
        val reply: Future[model.Post] = entityRef.askWithStatus(PostEntity.UpdatePost(in.id, in.content, in.image, in.author, _))
        val response: Future[Post] = reply.map(PostMapper.toProto)
        convertError(response)(system)
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
