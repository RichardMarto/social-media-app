package socialmedia.adapter.repository

import socialmedia.model.Post

trait FeedRepository {
  def post(session: ScalikeJdbcSession, post: Post): Unit
  def getPostsByAuthorEmail(session: ScalikeJdbcSession, email: String): List[Post]
}
