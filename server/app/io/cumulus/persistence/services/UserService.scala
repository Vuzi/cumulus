package io.cumulus.persistence.services

import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

import io.cumulus.core.Logging
import io.cumulus.core.persistence.CumulusDB
import io.cumulus.core.persistence.query.{QueryBuilder, QueryE}
import io.cumulus.core.validation.AppError
import io.cumulus.models.User
import io.cumulus.models.fs.Directory
import io.cumulus.persistence.stores.UserStore._
import io.cumulus.persistence.stores.{FsNodeStore, UserStore}
import org.mindrot.jbcrypt.BCrypt
import play.api.libs.json.__

/**
  * User service, which handle the business logic and validations of the users.
  */
class UserService(
  userStore: UserStore,
  fsNodeStore: FsNodeStore
)(
  implicit
  qb: QueryBuilder[CumulusDB]
) extends Logging {

  /**
    * Finds an user by its ID.
    * @param id The user of the user
    */
  def find(id: String): Future[Either[AppError, User]] = {

    for {
      // Validate the provided UUID
      uuid <- QueryE.pure {
        Try(UUID.fromString(id))
          .toEither
          .left.map(_ => AppError.validation("validation.user.uuid-invalid", id))
      }

      // Find the user by its ID
      user <- QueryE.getOrNotFound(userStore.find(uuid))
    } yield user

  }.commit()

  /**
    * Finds and user by its email.
    * @param email The email of the user
    */
  def findByEmail(email: String): Future[Either[AppError, User]] =
    QueryE
      .getOrNotFound(userStore.findBy(emailField, email))
      .commit()

  /**
    * Finds an user by its login.
    * @param login The login of the user
    */
  def findByLogin(login: String): Future[Either[AppError, User]] =
    QueryE
      .getOrNotFound(userStore.findBy(loginField, login))
      .commit()

  /**
    * Checks an user password and login, and return either an error or the found user. Use this method to validate
    * an user logging in.
    * @param login The user's login
    * @param password The user's password
    */
  def loginUser(login: String, password: String): Future[Either[AppError, User]] = {

    // Search an user by the login & check the hashed password
    userStore.findBy(loginField, login).map {
      case Some(user) if BCrypt.checkpw(password, user.password) =>
        Right(user)
      case _ =>
        Left(AppError.validation("validation.user.invalid-login-or-password"))
    }

  }.commit()

  /**
    * Creates a new user. The provided user should have an unique ID, email and login ; otherwise the creation will
    * return an error.
    * @param user The user to be created
    */
  def createUser(user: User): Future[Either[AppError, User]] = {

    for {
      // Check for duplicated UUID. Should not really happen...
      _ <- QueryE(userStore.find(user.id).map {
        case Some(_) => Left(AppError.validation(__ \ "id", "validation.user.uuid-already-exists", user.id.toString))
        case None    => Right(())
      })

      // Also check for user with the same login or email
      _ <- QueryE(userStore.findBy(emailField, user.email).map {
        case Some(_) => Left(AppError.validation(__ \ "email", "validation.user.email-already-exists", user.email))
        case None    => Right(())
      })
      _ <- QueryE(userStore.findBy(loginField, user.login).map {
        case Some(_) => Left(AppError.validation(__ \ "login", "validation.user.login-already-exists", user.login))
        case None    => Right(())
      })

      // Finally, create the new user
      _ <- QueryE.lift(userStore.create(user))

      // Also create the root element of its own file-system
      _ <- QueryE.lift(fsNodeStore.create(Directory(user.id, "/")))
    } yield user

  }.commit()

}
