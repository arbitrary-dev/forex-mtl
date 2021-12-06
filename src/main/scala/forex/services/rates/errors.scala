package forex.services.rates

object errors {

  sealed trait Error
  object Error {
    // TODO ServiceUnavailable
    // TODO QuotaReached
    final case class OneFrameLookupFailed(msg: String) extends Error
  }

}
