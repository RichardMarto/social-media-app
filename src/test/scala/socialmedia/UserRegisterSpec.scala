package socialmedia

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import socialmedia.user.{User, UserRegister}

object UserRegisterSpec {
  val config = ConfigFactory
    .parseString("""
      akka.actor.serialization-bindings {
        "socialmedia.CborSerializable" = jackson-cbor
      }
      """)
    .withFallback(EventSourcedBehaviorTestKit.config)
}

class UserRegisterSpec extends ScalaTestWithActorTestKit(UserRegisterSpec.config)
  with AnyWordSpecLike
  with BeforeAndAfterEach {

  private val aUser = User("Test Testson", "test_testson@testmail.com")
  private val anotherUser = User("Test Testson jr", "test.jr@testmail.com")
  private val anotherUserWithDuplicatedEmail = User("Test Testson 3rd", "test_testson@testmail.com")

  private val userRegisterId = "userRegister"
  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[
      Command,
      Event,
      UserRegister.State](system, UserRegister(userRegisterId))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "The User Register" should {

    "register a new user" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[User]](replyTo => UserRegister.RegisterUser(aUser, replyTo))
      result.reply should ===(StatusReply.Success(aUser))
      result.event should ===(UserRegister.UserRegistered(userRegisterId, aUser))
    }

    "register multiple users with different emails" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[User]](replyTo => UserRegister.RegisterUser(aUser, replyTo))
      result.reply should ===(StatusReply.Success(aUser))
      result.event should ===(UserRegister.UserRegistered(userRegisterId, aUser))
      val secondResult = eventSourcedTestKit.runCommand[StatusReply[User]](replyTo => UserRegister.RegisterUser(anotherUser, replyTo))
      secondResult.reply should ===(StatusReply.Success(anotherUser))
      secondResult.event should ===(UserRegister.UserRegistered(userRegisterId, anotherUser))
    }

    "throw an error when registering multiple users with duplicated email" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[User]](replyTo => UserRegister.RegisterUser(aUser, replyTo))
      result.reply should ===(StatusReply.Success(aUser))
      result.event should ===(UserRegister.UserRegistered(userRegisterId, aUser))
      val secondResult = eventSourcedTestKit.runCommand[StatusReply[User]](replyTo => UserRegister.RegisterUser(anotherUserWithDuplicatedEmail, replyTo))
      secondResult.reply.isError should ===(true)
    }
  }
}
