package io.cumulus.core.json

import java.util.UUID
import scala.util.Try

import cats.data.NonEmptyList
import julienrf.json.derived
import play.api.libs.json._

object JsonFormat {

  def derivedReads[A](implicit derivedReads: derived.DerivedReads[A]): Reads[A] =
    derived.flat.reads[A]((__ \ "__type").format[String])

  def derivedWrites[A](implicit derivedOWrites: derived.DerivedOWrites[A]): OWrites[A] =
    derived.flat.owrites[A]((__ \ "__type").format[String])

  // Utility function to derive formats
  def derivedFormat[A](
    implicit
    derivedReads: derived.DerivedReads[A],
    derivedOWrites: derived.DerivedOWrites[A]
  ): OFormat[A] =
    derived.flat.oformat[A]((__ \ "__type").format[String])


  implicit def UUIDFormat: Format[UUID] = new Format[UUID] {

    override def reads(json: JsValue): JsResult[UUID] =
      Json.fromJson[String](json).flatMap { s =>
        Try(java.util.UUID.fromString(s)).toEither match {
          case Right(uuid) => JsSuccess(uuid)
          case _           => JsError("Invalid UUID")
        }
      }

    override def writes(o: UUID): JsValue =
      JsString(o.toString)

  }

  implicit def nelFormat[A](implicit format: Format[A]): Format[NonEmptyList[A]] = new Format[NonEmptyList[A]] {

    override def reads(json: JsValue): JsResult[NonEmptyList[A]] =
      Json.fromJson[List[A]](json).flatMap {
        case head :: tail => JsSuccess(NonEmptyList.of(head, tail: _*))
        case _            => JsError("Unable to parse this empty list as a JSON.")
      }

    override def writes(o: NonEmptyList[A]): JsValue =
      JsArray(o.toList.map(format.writes))

  }

}
