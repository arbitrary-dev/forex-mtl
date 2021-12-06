package forex.programs.rates

import cats.Monad
import cats.data.EitherT
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.services._

import errors._

// TODO ProgramSpec
// TODO test a case where requests for all pairs made after cache expiration
//      24*60/5*(9^2) = 23328 requests/day
class Program[F[_]: Monad](
    ratesService: RatesService[F],
    cacheService: CacheService[F],
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    val pair = Pair(request.from, request.to)
    (for {
      cached <- fromCache(pair)
      result <- cached.fold(fromService(pair))(r => EitherT(r.asRight.pure))
    } yield result).value
  }

  private def fromCache(pair: Pair) =
    EitherT(cacheService.get(pair)).leftMap(toProgramError)

  private def fromService(pair: Pair) =
    for {
      rate <- EitherT(ratesService.get(pair)).leftMap(toProgramError)
      _ <- EitherT(cacheService.put(pair, rate)).leftMap(toProgramError)
    } yield rate
}

object Program {

  def apply[F[_]: Monad](
      ratesService: RatesService[F],
      cacheService: CacheService[F],
  ): Algebra[F] = new Program[F](ratesService, cacheService)

}
