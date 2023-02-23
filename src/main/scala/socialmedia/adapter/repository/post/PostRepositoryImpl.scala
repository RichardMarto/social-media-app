package socialmedia.adapter.repository.post

import scalikejdbc.{DBSession, WrappedResultSet, scalikejdbcSQLInterpolationImplicitDef}
import socialmedia.adapter.repository.ScalikeJdbcSession
import socialmedia.model.Post

class PostRepositoryImpl extends PostRepository {

  override def update(session: ScalikeJdbcSession, post: Post): Unit = {
    session.db.withinTx { implicit dbSession =>
      sql"""
            INSERT INTO post (content, image, date, author) VALUES (${post.content}, ${post.image}, ${post.date}, ${post.author})
            ON CONFLICT (author, date) DO UPDATE SET (content, image) = (${post.content}, ${post.image})
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
    dbSession.list(s"SELECT * FROM post WHERE author = '$email' LIMIT 10")(toPost)
  }

  private def select()(implicit dbSession: DBSession) = {
    dbSession.list("SELECT * FROM post LIMIT 10")(toPost)
  }

  private def toPost = {
    rs: WrappedResultSet =>
      Post(
        id = rs.string("author") + " - "+ rs.zonedDateTime("date"),
        content = rs.string("content"),
        image = rs.string("image"),
        date = rs.zonedDateTime("date"),
        author = rs.string("author")
      )
  }
}
