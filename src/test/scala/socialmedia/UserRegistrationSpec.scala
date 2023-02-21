package socialmedia

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import socialmedia.core.{Command, Event, Post, User, UserRegistration}

object UserRegistrationSpec {
  val config = ConfigFactory
    .parseString("""
      akka.actor.serialization-bindings {
        "socialmedia.core.CborSerializable" = jackson-cbor
      }
      """)
    .withFallback(EventSourcedBehaviorTestKit.config)
}

class UserRegistrationSpec extends ScalaTestWithActorTestKit(UserRegistrationSpec.config)
  with AnyWordSpecLike
  with BeforeAndAfterEach {

  private val aUser = User("Test Testson", "test_testson@testmail.com")
  private val anotherUser = User("Test Testson jr", "test.jr@testmail.com")
  private val anotherUserWithDuplicatedEmail = User("Test Testson 3rd", "test_testson@testmail.com")
  private val aPost = Post("Blablabla", "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...", "20/02/2022", "test_testson@testmail.com")

  private val userRegisterId = "userRegister"
  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[
      Command,
      Event,
      UserRegistration.State](system, UserRegistration(userRegisterId, "projectionTag"))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "The User Register" should {

    "register a new user" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[User]](replyTo => UserRegistration.RegisterUser(aUser, replyTo))
      result.reply should ===(StatusReply.Success(aUser))
      result.event should ===(UserRegistration.UserRegistered(aUser))
    }

    "register multiple users with different emails" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[User]](replyTo => UserRegistration.RegisterUser(anotherUser, replyTo))
      result.reply should ===(StatusReply.Success(anotherUser))
      result.event should ===(UserRegistration.UserRegistered(anotherUser))
    }

    "throw an error when registering multiple users with duplicated email" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[User]](replyTo => UserRegistration.RegisterUser(anotherUserWithDuplicatedEmail, replyTo))
      result.reply.isError should ===(true)
    }
  }

  "The User Register" should {

    "post a new post" in {
      eventSourcedTestKit.runCommand[StatusReply[User]](replyTo => UserRegistration.RegisterUser(aUser, replyTo))
      val result = eventSourcedTestKit.runCommand[StatusReply[Post]](replyTo => UserRegistration.PostPost(aUser.email, aPost, replyTo))
      result.reply should ===(StatusReply.Success(aPost))
      result.event should ===(UserRegistration.PostPosted(aPost))
    }

    "throw an error when trying to post for a email not registered" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[Post]](replyTo => UserRegistration.PostPost("unregisteredemail@unregisteredemails.com", aPost, replyTo))
      result.reply.isError should ===(true)
    }
  }
}
