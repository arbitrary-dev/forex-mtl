package forex.services.cache

import forex.domain.Rate
import forex.domain.Rate.Pair

import errors._

trait Algebra[F[_]] {
  def get(pair: Pair): F[Error Either Option[Rate]]
  def put(pair: Pair, rate: Rate): F[Error Either Unit]
}
