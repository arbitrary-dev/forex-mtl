package forex.programs.rates

import forex.services.rates.errors.{ Error => RatesServiceError }
import forex.services.cache.errors.{ Error => CacheServiceError }

object errors {

  sealed trait Error extends Exception
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameLookupFailed(msg) => Error.RateLookupFailed(msg)
  }

  def toProgramError(error: CacheServiceError): Error = error match {
    case CacheServiceError.RetrievalFailed(msg) => Error.RateLookupFailed(msg)
  }
}
