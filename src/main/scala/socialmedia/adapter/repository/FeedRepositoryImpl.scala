package socialmedia.adapter.repository

import scalikejdbc.{DBSession, WrappedResultSet, scalikejdbcSQLInterpolationImplicitDef}
import socialmedia.model.Post

class FeedRepositoryImpl extends FeedRepository {

  override def post(session: ScalikeJdbcSession, post: Post): Unit = {
    session.db.withinTx { implicit dbSession =>
      sql"""
           INSERT INTO post (content, image, date, author) VALUES (${post.content}, ${post.image}, ${post.date}, ${post.author})
         """.executeUpdate().apply()
    }
  }

  override def getPostsByAuthor(session: ScalikeJdbcSession, email: String): List[Post] = {
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

  override def getPosts(session: ScalikeJdbcSession): List[Post] = {
    if (session.db.isTxAlreadyStarted) {
      session.db.withinTx { implicit dbSession =>
        select()
      }
    } else {
      session.db.readOnly { implicit dbSession =>
        select()
      }
    }
  }

  private def select(email: String)(implicit dbSession: DBSession) = {
    dbSession.list("SELECT * FROM post WHERE author = ?", email)(toPost)
  }

  private def select()(implicit dbSession: DBSession) = {
    dbSession.list("SELECT * FROM post")(toPost)
  }

  private def toPost = {
    rs: WrappedResultSet =>
      Post(id = rs.int("id"), content = rs.string("content"), image = rs.string("image"), date = rs.string("date"), author = rs.string("author"))
  }
}
