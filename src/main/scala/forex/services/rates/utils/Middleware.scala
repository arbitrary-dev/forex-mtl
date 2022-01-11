package forex.services.rates.utils

import cats.data.EitherT
import cats.effect.concurrent.Deferred
import cats.effect.{ Concurrent, ConcurrentEffect, Timer }
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import cats.syntax.traverse._
import com.typesafe.scalalogging.StrictLogging
import forex.config.RatesServiceConfig
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Rate }
import forex.services.cache.errors.{ Error => CacheServiceError }
import forex.services.rates.errors.Error
import forex.services.rates.utils.Middleware.{ Request, Result, toRatesError }
import forex.services.{CacheService, RatesService}
import fs2.concurrent.Topic
import cats.effect.Sync

/** Middleware utility that wraps rates service to provide additional goodies.
 *
 * @param topic A topic to publish requests for batching
 * @param cache Cache service for results caching
 */
class Middleware[F[_]: Concurrent](
  topic: Topic[F, Request[F]],
  cache: CacheService[F],
) extends RatesService[F] {

  override def get(pair: Pair): F[Result[Rate]] =
    (for {
      cached <- EitherT(cache.get(pair)).leftMap(toRatesError)
      result <- EitherT(cached.fold(publishForBatching(pair))(_.asRight.pure))
    } yield result).value

  private def publishForBatching(pair: Pair): F[Result[Rate]] =
    for {
      d <- Deferred[F, Result[Rate]]
      _ <- topic.publish1(pair -> d)
      result <- d.get
      // cache the result
      result <- result.fold(
        _.asLeft.pure,
        rate => EitherT(cache.put(pair, rate)).bimap(toRatesError, _ => rate).value
      )
    } yield result

  override def getMany(pairs: List[Pair]): F[Either[Error, List[Rate]]] =
    sys.error("not implemented") // TODO call impl.getMany()
}

object Middleware extends StrictLogging {

  private type Result[A]     = Error Either A
  private type Request[F[_]] = (Pair, Deferred[F, Result[Rate]])

  def apply[F[_]: ConcurrentEffect: Timer](
      config: RatesServiceConfig,
      impl: RatesService[F],
      cache: CacheService[F],
  ): F[RatesService[F]] = {
    val dummy = Pair(Currency.USD, Currency.USD) -> Deferred.unsafe[F, Result[Rate]]

    Topic[F, Request[F]](dummy) flatMap { topic =>
      val stream = topic
        .subscribe(Int.MaxValue) // TODO max?
        .drop(1) // drop dummy
        .evalTap { case (pair, _) => Sync[F].delay(logger.debug(s"Pair enqueued: ${pair.show}")) }
        .groupWithin(config.batchSize, config.batchLinger)
        .evalTap { chunk =>
          logger.debug(s"Chunk size: ${chunk.size}")
          val pairDefers = chunk.toList.groupBy(_._1).view.mapValues(_.map(_._2)).toMap
          val pairs      = pairDefers.keys.toList
          (EitherT(impl.getMany(pairs)) flatMap { rates =>
            logger.debug(s"${rates.size} rates received.")
            val rateDefers = (rates map { rate =>
              rate -> pairDefers(rate.pair)
            }).toMap
            EitherT(
              rateDefers
                .flatMap { case (rate, defers) => defers.map(_.complete(rate.asRight[Error])) }
                .toList
                .sequence
                .map(_.asRight[Error])
            )
          }).value
        }
        .compile
        .drain

      Concurrent[F].start(stream) *>
        new Middleware[F](topic, cache).asInstanceOf[RatesService[F]].pure
    }
  }

  private def toRatesError(error: CacheServiceError): Error = error match {
    case CacheServiceError.RetrievalFailed(msg) => Error.OneFrameLookupFailed(msg)
  }
}
