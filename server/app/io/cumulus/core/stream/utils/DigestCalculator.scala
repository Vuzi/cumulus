package io.cumulus.core.stream.utils

import java.security.MessageDigest

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString
import io.cumulus.core.utils.Base64

/**
  * Compute the digest of the provided stream.
  *
  * @param algorithm The algorithm to use
  */
class DigestCalculator(algorithm: String) extends GraphStage[FlowShape[ByteString, String]] {

  val in = Inlet[ByteString]("DigestCalculator.in")
  val out = Outlet[String]("DigestCalculator.out")
  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    val digest = MessageDigest.getInstance(algorithm)

    setHandler(out, new OutHandler {
      override def onPull() = {
        pull(in)
      }
    })

    setHandler(in, new InHandler {
      override def onPush() = {
        val chunk = grab(in)
        digest.update(chunk.toArray)
        pull(in)
      }

      override def onUpstreamFinish() = {
        emit(out, Base64.encode(digest.digest()))
        completeStage()
      }
    })

  }
}

object DigestCalculator {

  def apply(algorithm: String): DigestCalculator =
    new DigestCalculator(algorithm)

  /**
    * Compute the MD5 hash of the stream.
    */
  def md5: DigestCalculator =
    DigestCalculator("MD5")

  /**
    * Compute the SHA-1 hash of the stream.
    */
  def sha1: DigestCalculator =
    DigestCalculator("SHA-1")

  /**
    * Compute the SHA-256 hash of the stream.
    */
  def sha256: DigestCalculator =
    DigestCalculator("SHA-256")

}
