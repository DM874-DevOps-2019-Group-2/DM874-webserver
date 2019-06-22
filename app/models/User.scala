package models

case class User (
                id: Int,
                username: String,
                jwt: String
               )

case class UnauthedUser(
                       username: String,
                       password: String
                       )
