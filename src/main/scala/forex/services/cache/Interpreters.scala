package forex.services.cache

import cats.Applicative
import cats.effect.Sync
import forex.config.CacheServiceConfig
import forex.services.cache.interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] =
    new CacheDummy[F]()
  def scaffeine[F[_]: Sync](config: CacheServiceConfig): Algebra[F] =
    new CacheScaffeine[F](config)
}
