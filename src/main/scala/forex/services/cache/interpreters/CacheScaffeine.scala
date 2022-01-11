package forex.services.cache.interpreters

import cats.effect.Sync
import cats.syntax.either._
import com.github.blemale.scaffeine.Scaffeine
import forex.config.CacheServiceConfig
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.services.cache.Algebra
import forex.services.cache.errors._

import java.time.Instant

class CacheScaffeine[F[_]: Sync](config: CacheServiceConfig) extends Algebra[F] {

  private val cache =
    Scaffeine()
      .expireAfterWrite(config.expiration)
      .maximumSize(config.size.toLong)
      .build[Pair, Rate]()

  private def hasValidTimestamp(rate: Rate) = {
    val timestamp = rate.timestamp.value
    val off       = rate.timestamp.value.getOffset()
    val invalid   = Instant.now().atOffset(off).minusNanos(config.expiration.toNanos)
    timestamp.isAfter(invalid)
  }

  override def get(pair: Rate.Pair): F[Error Either Option[Rate]] = Sync[F].delay {
    cache
      .getIfPresent(pair)
      .filter(hasValidTimestamp)
      .asRight
  }

  override def put(pair: Pair, rate: Rate): F[Error Either Unit] = Sync[F].delay {
    cache.put(pair, rate)
    ().asRight
  }
}
