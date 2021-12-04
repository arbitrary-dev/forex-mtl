package forex.services.rates

object errors {

  sealed trait Error
  object Error {
    // TODO ServiceUnavailable
    final case class OneFrameLookupFailed(msg: String) extends Error
  }

}
