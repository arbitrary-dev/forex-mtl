package forex.services.cache.interpreters

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.option._
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.services.cache.Algebra
import forex.services.cache.errors._

class CacheDummy[F[_]: Applicative] extends Algebra[F] {

  override def get(pair: Pair): F[Error Either Option[Rate]] =
    none.asRight.pure

  override def put(rate: Rate): F[Error Either Unit] =
    ().asRight.pure

  override def getPairsToPreheat(n: Int, besides: List[Pair]): F[List[Pair]] =
    List.empty.pure
}
