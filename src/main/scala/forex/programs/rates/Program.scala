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
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] =
    (for {
      pair <- EitherT(Pair(request.from, request.to).asRight[Error].pure)
      rate <- EitherT(ratesService.get(pair)).leftMap(toProgramError)
    } yield rate).value
}

object Program {

  def apply[F[_]: Monad](
      ratesService: RatesService[F],
  ): Algebra[F] = new Program[F](ratesService)
}
