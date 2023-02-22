package socialmedia.adapter.grpc

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.model.DateTime
import akka.stream.scaladsl.Source
import akka.util.Timeout
import org.slf4j.{Logger, LoggerFactory}
import socialmedia.adapter.repository.{FeedRepository, ScalikeJdbcSession}
import socialmedia.core.UserRegistration
import socialmedia.proto._

import scala.concurrent.Future

class PostServiceImpl(system: ActorSystem[_], feedRepository: FeedRepository) extends PostService with ErrorConverter {
  import system.executionContext

  private val log: Logger = LoggerFactory.getLogger(getClass)

  private val sharding = ClusterSharding(system)

  implicit private val timeout: Timeout =
    Timeout.create(
      system.settings.config.getDuration("social-media.ask-timeout"))

  override def postPost(in: PostPostRequest): Future[Post] = {
    log.info(s"Posting for user with email {}", in.author)
    val post: socialmedia.model.Post = socialmedia.model.Post(in.content, in.image, DateTime.now.toString(), in.author)
    val entityRef = sharding.entityRefFor(UserRegistration.EntityKey, in.author.hashCode.toString)
    val reply: Future[socialmedia.model.Post] = entityRef.askWithStatus(UserRegistration.PostPost(in.author, post, _))
    val response: Future[socialmedia.proto.Post] = reply.map(u => socialmedia.proto.Post(post.content, post.image, post.date, post.author))
    convertError(response)(system)
  }

  override def getFeed(in: socialmedia.proto.GetFeedRequest): Source[Post, NotUsed] = {
    in.author match {
      case Some(author) => Source(
        ScalikeJdbcSession.withSession {
          session => feedRepository.getPostsByAuthor(session, author)
        }.map(post => Post(post.content, post.image, post.date, post.author))
      )
      case None => Source(
        ScalikeJdbcSession.withSession {
          session => feedRepository.getPosts(session)
        }.map(post => Post(post.content, post.image, post.date, post.author))
      )
    }
  }
}