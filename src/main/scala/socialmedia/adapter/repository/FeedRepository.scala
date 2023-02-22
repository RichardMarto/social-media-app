package socialmedia.adapter.repository

import socialmedia.model.Post

trait FeedRepository {
  def post(session: ScalikeJdbcSession, post: Post): Unit
  def getPostsByAuthor(session: ScalikeJdbcSession, email: String): List[Post]
  def getPosts(session: ScalikeJdbcSession): List[Post]
}
