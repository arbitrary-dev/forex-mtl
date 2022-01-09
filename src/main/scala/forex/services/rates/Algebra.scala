package forex.services.rates

import forex.domain.Rate
import forex.domain.Rate.Pair

import errors._

trait Algebra[F[_]] {
  def get(pair: Pair): F[Error Either Rate]
  def getMany(pairs: List[Pair]): F[Error Either List[Rate]]
}
