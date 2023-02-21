package socialmedia.adapters.repository

import socialmedia.core.Post

trait FeedRepository {
  def post(session: ScalikeJdbcSession, post: Post)
  def getPostsByAuthorEmail(session: ScalikeJdbcSession, email: String): List[Post]
}
