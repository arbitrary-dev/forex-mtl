package forex.services.rates

import forex.domain.Rate
import fs2.Stream

import errors._

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]
  def get2(pairs: Rate.Pair*): Stream[F, Error Either Rate] = ???
}
