package socialmedia.adapter.repository.post

import socialmedia.adapter.repository.ScalikeJdbcSession
import socialmedia.model.Post

trait PostRepository {
  def update(session: ScalikeJdbcSession, post: Post): Unit
  def getPostsByAuthor(session: ScalikeJdbcSession, email: String): List[Post]
  def getPosts(session: ScalikeJdbcSession): List[Post]
}
