package socialmedia.adapter.grpc

import akka.actor.typed.ActorSystem
import akka.grpc.GrpcServiceException
import io.grpc.Status

import scala.concurrent.{Future, TimeoutException}

trait ErrorConverter {
  def convertError[T](response: Future[T])(system: ActorSystem[_]): Future[T] = {
    import system.executionContext
    response.recoverWith {
      case _: TimeoutException =>
        Future.failed(
          new GrpcServiceException(
            Status.UNAVAILABLE.withDescription("Operation timed out")))
      case exc =>
        Future.failed(
          new GrpcServiceException(
            Status.INVALID_ARGUMENT.withDescription(exc.getMessage)))
    }
  }
}
