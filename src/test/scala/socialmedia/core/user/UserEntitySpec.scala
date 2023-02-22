package socialmedia.core.user

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import socialmedia.model.{Command, Event, User}

object UserEntitySpec {
  val config = ConfigFactory
    .parseString("""
      akka.actor.serialization-bindings {
        "socialmedia.model.CborSerializable" = jackson-cbor
      }
      """)
    .withFallback(EventSourcedBehaviorTestKit.config)
}

class UserEntitySpec extends ScalaTestWithActorTestKit(UserEntitySpec.config)
  with AnyWordSpecLike
  with BeforeAndAfterEach {

  private val aUser = User("Test Testson", "test_testson@testmail.com")
  private val anotherUser = User("Test Testson jr", "test.jr@testmail.com")
  private val anotherUserWithDuplicatedEmail = User("Test Testson 3rd", "test_testson@testmail.com")

  private val entityId = "userRegister"
  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[
      Command,
      Event,
      UserEntity.State](system, UserEntity(entityId, "projectionTag"))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "The user entity" should {

    "register a new user" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[User]](replyTo => UserEntity.RegisterUser(aUser, replyTo))
      result.reply should ===(StatusReply.Success(aUser))
      result.event should ===(UserEntity.UserRegistered(aUser))
    }

    "register multiple users with different emails" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[User]](replyTo => UserEntity.RegisterUser(anotherUser, replyTo))
      result.reply should ===(StatusReply.Success(anotherUser))
      result.event should ===(UserEntity.UserRegistered(anotherUser))
    }

    "throw an error when registering multiple users with duplicated email" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[User]](replyTo => UserEntity.RegisterUser(anotherUserWithDuplicatedEmail, replyTo))
      result.reply.isError should ===(true)
    }
  }
}
