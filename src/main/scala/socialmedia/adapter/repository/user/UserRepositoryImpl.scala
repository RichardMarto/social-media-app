package socialmedia.adapter.repository.user

import scalikejdbc.{DBSession, WrappedResultSet, scalikejdbcSQLInterpolationImplicitDef}
import socialmedia.adapter.repository.ScalikeJdbcSession
import socialmedia.model.User

class UserRepositoryImpl extends UserRepository {

  override def save(session: ScalikeJdbcSession, user: User): Unit = {
    session.db.withinTx { implicit dbSession =>
      sql"""
           INSERT INTO public.user (email, name) VALUES (${user.email}, ${user.name})
           ON CONFLICT (email) DO UPDATE SET name = ${user.name}
         """.executeUpdate().apply()
    }
  }

  override def getByEmail(session: ScalikeJdbcSession, email: String): Option[User] = {
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
    dbSession.first(s"SELECT * FROM public.user WHERE email = '$email'") {
      rs: WrappedResultSet => User(name = rs.string("name"), email = rs.string("email"))
    }
  }
}
