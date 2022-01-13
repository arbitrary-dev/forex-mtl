package forex.services.cache.interpreters

import cats.effect.Sync
import cats.syntax.either._
import com.github.blemale.scaffeine.Scaffeine
import forex.config.CacheServiceConfig
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Rate }
import forex.services.cache.Algebra
import forex.services.cache.errors._

import java.time.Instant
import scala.collection.concurrent.TrieMap

class CacheScaffeine[F[_]: Sync](config: CacheServiceConfig) extends Algebra[F] {

  private val cache =
    Scaffeine()
      .expireAfterWrite(config.expiration)
      .maximumSize(config.size.toLong)
      .evictionListener[Pair, Rate] { case (pair, _, _) => pairsToPreheat += pair -> () }
      .build[Pair, Rate]()

  private val pairsToPreheat =
    TrieMap.from(
      for {
        a <- Currency.All
        b <- Currency.All
      } yield Pair(a, b) -> ()
    )

  override def getPairsToPreheat(n: Int, besides: List[Pair]): F[List[Pair]] = Sync[F].delay {
    pairsToPreheat.keys.filterNot(besides.contains).take(n).toList
  }

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

  override def put(rate: Rate): F[Error Either Unit] = Sync[F].delay {
    cache.put(rate.pair, rate)
    pairsToPreheat -= rate.pair
    ().asRight
  }
}
