package forex.services.rates

import cats.Applicative
import cats.effect.{ ConcurrentEffect, Timer }
import forex.config.RatesServiceConfig
import forex.services.rates.interpreters._

import scala.concurrent.ExecutionContext

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] =
    new OneFrameDummy[F]()
  def live[F[_]: ConcurrentEffect](config: RatesServiceConfig, ec: ExecutionContext): Algebra[F] =
    new OneFrameLive[F](config, ec)
  def batched[F[_]: ConcurrentEffect: Timer](
      config: RatesServiceConfig,
      impl: Algebra[F],
  ): F[Algebra[F]] =
    Batched[F](config, impl)
}
