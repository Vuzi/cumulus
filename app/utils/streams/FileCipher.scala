package utils.streams

import javax.crypto._

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString
import utils.Utils

/**
  * Helpers to generate AES/CBC cipher/decipher for Flow[FileChunk, FileChunk]
  */
object AESCipher {

  /**
    * Generate an AES/CBC cipher to encrypt a stream
    * @param keySpec The secret key
    * @param ivBytes The initialization vector bytes
    * @return The stream
    */
  def encryptor(
    keySpec: SecretKey,
    ivBytes: ByteString
  ): FileCipher = {
    FileCipher(Utils.Crypto.createAESCipher(Cipher.ENCRYPT_MODE, keySpec, ivBytes))
  }

  /**
    * Generate an AES/CBC cipher to decrypt a stream
    * @param keySpec The secret key
    * @param ivBytes The initialization vector bytes
    * @return The stream
    */
  def decryptor(
     keySpec: SecretKey,
     ivBytes: ByteString
   ): FileCipher = {
    FileCipher(Utils.Crypto.createAESCipher(Cipher.DECRYPT_MODE, keySpec, ivBytes))
  }
}

/**
  * Custom cipherer for an Akka stream of BytString. Any cipher could be used with this method. According to the nature of
  * the cipher, the data will be streamed by block of varying size even if the data is streamed in with blocks of different
  * sizes. The behavior is to pull upstream until a block is completed, then push downstream, for each downstream pull.
  *
  * @param cipher The cipher to use
  */
class FileCipher(cipher: Cipher) extends GraphStage[FlowShape[ByteString, ByteString]] {
  val in = Inlet[ByteString]("FileCipher.in")
  val out = Outlet[ByteString]("FileCipher.out")
  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val bytes = grab(in)

          if (bytes.isEmpty)
            pull(in) // Should not happen, request more bytes
          else {
            val ciphered = ByteString(cipher.update(bytes.toArray))

            // En/Decryption (e.g. AES with CBC) will work with blocks, if the block is not completed
            // the cipher will return an empty ByteString. We won't send it because it mess up with
            // chucked encoding, so we pull to complete our block and push it when ready
            if(ciphered.nonEmpty)
              push(out, ciphered)
            else
              pull(in)
          }
        }

        override def onUpstreamFinish(): Unit = {
          val bs = ByteString(cipher.doFinal())
          if (bs.nonEmpty)
            emit(out, bs) // Complete if necessary
          complete(out)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })
    }
}

object FileCipher {
  def apply(cipher: Cipher): FileCipher = new FileCipher(cipher)
}