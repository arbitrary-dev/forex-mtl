package forex.services.cache

import cats.Applicative
import forex.config.CacheServiceConfig

import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] =
    new CacheDummy[F]()
  def scaffeine[F[_]: Applicative](config: CacheServiceConfig): Algebra[F] =
    new CacheScaffeine[F](config)
}
