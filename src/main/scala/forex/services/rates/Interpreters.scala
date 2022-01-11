package forex.services.rates

import cats.Applicative
import cats.effect.{ConcurrentEffect, Sync, Timer}
import cats.syntax.flatMap._
import cats.syntax.functor._
import forex.config.RatesServiceConfig
import forex.services.CacheService
import forex.services.rates.interpreters._
import forex.services.rates.utils.Middleware

import scala.concurrent.ExecutionContext

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] =
    new OneFrameDummy[F]()
  def live[F[_]: ConcurrentEffect: Timer](
    config: RatesServiceConfig,
    cache: CacheService[F],
    ec: ExecutionContext,
  ): F[Algebra[F]] =
    for {
      live <- Sync[F].delay(new OneFrameLive[F](config, ec))
      result <- Middleware[F](config, live, cache)
    } yield result
}
