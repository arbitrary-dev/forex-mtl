package forex.services.cache

import forex.domain.Rate
import forex.domain.Rate.Pair

import errors._

trait Algebra[F[_]] {
  def get(pair: Pair): F[Error Either Option[Rate]]
  def put(rate: Rate): F[Error Either Unit]

  def getPairsToPreheat(n: Int, besides: List[Pair]): F[List[Pair]]
}
