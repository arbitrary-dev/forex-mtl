package forex.services.cache

object errors {

  sealed trait Error
  object Error {
    final case class RetrievalFailed(msg: String) extends Error
  }

}
