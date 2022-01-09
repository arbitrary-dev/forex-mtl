package forex.services.rates.interpreters

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.Rate.Pair
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.Algebra
import forex.services.rates.errors._

class OneFrameDummy[F[_]: Applicative] extends Algebra[F] {

  override def get(pair: Pair): F[Error Either Rate] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.now)
      .asRight[Error]
      .pure[F]

  override def getMany(pairs: List[Pair]): F[Error Either List[Rate]] =
    pairs
      .map(Rate(_, Price(BigDecimal(100)), Timestamp.now))
      .asRight[Error]
      .pure[F]
}
