package forex.services.rates

import cats.Applicative
import cats.effect.ConcurrentEffect
import forex.config.RatesServiceConfig

import scala.concurrent.ExecutionContext

import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] =
    new OneFrameDummy[F]()
  def live[F[_]: ConcurrentEffect](config: RatesServiceConfig, ec: ExecutionContext): Algebra[F] =
    new OneFrameLive[F](config, ec)
}
