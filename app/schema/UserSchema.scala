package schema

import javax.inject.Inject
import models.{UnauthedUser, User}
import play.api.db.slick._
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class DBUser(id: Option[Int], username: String, password: String)

class UsersTable(tag: Tag) extends Table[DBUser](tag, "users") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username")
  def password = column[String]("password")
  def * = (id.?, username, password) <> (DBUser.tupled, DBUser.unapply)
}

class UsersDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {
  val users = TableQuery[UsersTable]

  def insert(dbUser: DBUser): Future[Int] = db.run(users += dbUser)

  def get(id: Int): Future[Option[DBUser]] = db.run(users.filter(_.id === id).result.headOption)
  def get(unauthedUser: UnauthedUser): Future[Option[DBUser]] = db.run(users.filter(_.username === unauthedUser.username).filter(_.password === unauthedUser.password).result.headOption)
  def get(username: String): Future[Option[DBUser]] = db.run(users.filter(_.username === username).result.headOption)

  def check(user: User) = db.run(users.filter(_.id === user.id).filter(_.username === user.username).exists.result)
}
