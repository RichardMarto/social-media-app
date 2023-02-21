package socialmedia.adapters.repository

import scalikejdbc.{DBSession, scalikejdbcSQLInterpolationImplicitDef}
import socialmedia.core.Post

class FeedRepositoryImpl extends FeedRepository {

  override def post(session: ScalikeJdbcSession, post: Post): Unit = {
    session.db.withinTx { implicit dbSession =>
      sql"""
           INSERT INTO post (content, image, date, author) VALUES (${post.content}, ${post.image}, ${post.date}, ${post.author})
         """.executeUpdate().apply()
    }
  }

  override def getPostsByAuthorEmail(session: ScalikeJdbcSession, email: String): List[Post] = {
    if (session.db.isTxAlreadyStarted) {
      session.db.withinTx { implicit dbSession =>
        select(email)
      }
    } else {
      session.db.readOnly { implicit dbSession =>
        select(email)
      }
    }
  }

  private def select(email: String)(implicit dbSession: DBSession) = {
    dbSession.list("SELECT * FROM post WHERE author = ?", email) { rs =>
      Post(content = rs.string("content"), image = rs.string("image"), date = rs.string("date"), author = rs.string("author"))
    }
  }
}
