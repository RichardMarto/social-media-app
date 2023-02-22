//package socialmedia.core.post
//
//import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
//import akka.http.scaladsl.model.DateTime
//import akka.pattern.StatusReply
//import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
//import com.typesafe.config.ConfigFactory
//import org.scalatest.BeforeAndAfterEach
//import org.scalatest.wordspec.AnyWordSpecLike
//import socialmedia.core.post.PostEntity
//import socialmedia.model.{Command, Event, Post, User}
//
//object PostEntitySpec {
//  val config = ConfigFactory
//    .parseString("""
//      akka.actor.serialization-bindings {
//        "socialmedia.model.CborSerializable" = jackson-cbor
//      }
//      """)
//    .withFallback(EventSourcedBehaviorTestKit.config)
//}
//
//class PostEntitySpec extends ScalaTestWithActorTestKit(PostEntitySpec.config)
//  with AnyWordSpecLike
//  with BeforeAndAfterEach {
//
//  private val aPost = Post(Some("asdfasdf"), "Blablabla", "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...", DateTime.now.toString(), "test_testson@testmail.com")
//
//  private val entityId = "userRegister"
//  private val eventSourcedTestKit =
//    EventSourcedBehaviorTestKit[
//      Command,
//      Event,
//      PostEntity.State](system, PostEntity(entityId, "projectionTag"))
//
//  override protected def beforeEach(): Unit = {
//    super.beforeEach()
//    eventSourcedTestKit.clear()
//  }
//
//  "The post entity" should {
//
//    "update a new post" in {
//      val result = eventSourcedTestKit.runCommand[StatusReply[Post]](replyTo => PostEntity.CreatePost(aPost, replyTo))
//      result.reply should ===(StatusReply.Success(aPost))
//      result.event should ===(PostEntity.PostCreated(aPost))
//    }
//  }
//}
