package socialmedia.adapter.repository.user

import socialmedia.adapter.repository.ScalikeJdbcSession
import socialmedia.model.User

trait UserRepository {
  def save(session: ScalikeJdbcSession, user: User): Unit
  def getByEmail(session: ScalikeJdbcSession, email: String): Option[User]
}
