package socialmedia.core.post

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import socialmedia.model.{Command, Event, Post}

import java.time.ZonedDateTime

object PostEntitySpec {
  val config = ConfigFactory
    .parseString("""
      akka.actor.serialization-bindings {
        "socialmedia.model.CborSerializable" = jackson-cbor
      }
      """)
    .withFallback(EventSourcedBehaviorTestKit.config)
}

class PostEntitySpec extends ScalaTestWithActorTestKit(PostEntitySpec.config)
  with AnyWordSpecLike
  with BeforeAndAfterEach {

  private val content: String = "Blablabla."
  private val image: String = ""
  private val anotherContent: String = "Loren ipsum."
  private val anotherImage: String = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD..."
  private val author: String = "test_testson@testmail.com"

  private val entityId = "userRegister"
  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[
      Command,
      Event,
      PostEntity.State](system, PostEntity(entityId, "projectionTag"))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "The post entity" should {

    "save a new post" in {
      val startingTime: String = ZonedDateTime.now.toString
      val result = eventSourcedTestKit.runCommand[StatusReply[Post]](replyTo => PostEntity.CreatePost(content, image, author , replyTo))
      result.reply shouldBe a [StatusReply[Post]]
      val post = result.reply.getValue
      post.content shouldBe content
      post.image shouldBe image
      post.author shouldBe author
      val splitedId = post.id.split(" - ")
      splitedId(0) shouldBe author
      splitedId(1).split("\\.")(0) should ===(startingTime.split("\\.")(0))
      result.event should ===(PostEntity.PostCreated(post))
    }

    "update a existing post" in {
      val oldPost = eventSourcedTestKit.runCommand[StatusReply[Post]](replyTo => PostEntity.CreatePost(content, image, author , replyTo)).reply.getValue
      val result = eventSourcedTestKit.runCommand[StatusReply[Post]](replyTo => PostEntity.UpdatePost(oldPost.id, anotherContent, anotherImage, author, replyTo))
      result.reply shouldBe a [StatusReply[Post]]
      val post = result.reply.getValue
      post.content shouldBe anotherContent
      post.image shouldBe anotherImage
      post.author shouldBe oldPost.author
      post.id shouldBe oldPost.id
      result.event should ===(PostEntity.PostUpdated(post))
    }

    "update a existing post content" in {
      val oldPost = eventSourcedTestKit.runCommand[StatusReply[Post]](replyTo => PostEntity.CreatePost(content, image, author, replyTo)).reply.getValue
      val result = eventSourcedTestKit.runCommand[StatusReply[Post]](replyTo => PostEntity.UpdatePost(oldPost.id, anotherContent, "", author, replyTo))
      val post = result.reply.getValue
      post.content shouldBe anotherContent
      post.image shouldBe oldPost.image
      post.author shouldBe oldPost.author
      post.id shouldBe oldPost.id
      result.event should ===(PostEntity.PostUpdated(post))
    }

    "update a existing post image" in {
      val oldPost = eventSourcedTestKit.runCommand[StatusReply[Post]](replyTo => PostEntity.CreatePost(content, image, author, replyTo)).reply.getValue
      val result = eventSourcedTestKit.runCommand[StatusReply[Post]](replyTo => PostEntity.UpdatePost(oldPost.id, null, anotherImage, author, replyTo))
      val post = result.reply.getValue
      post.content shouldBe oldPost.content
      post.image shouldBe anotherImage
      post.author shouldBe oldPost.author
      post.id shouldBe oldPost.id
      result.event should ===(PostEntity.PostUpdated(post))
    }
  }
}
