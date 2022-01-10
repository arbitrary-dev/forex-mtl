package forex.services.rates.interpreters

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
import forex.services.rates.Algebra
import forex.services.rates.errors.Error
import forex.services.rates.interpreters.Batched.{ Request, Result }
import fs2.concurrent.Topic

// TODO fix "Not found" issue
class Batched[F[_]: Concurrent](topic: Topic[F, Request[F]]) extends Algebra[F] {

  override def get(pair: Pair): F[Result[Rate]] =
    for {
      d <- Deferred[F, Result[Rate]]
      _ <- topic.publish1(pair -> d)
      result <- d.get
    } yield result

  override def getMany(pairs: List[Pair]): F[Either[Error, List[Rate]]] =
    sys.error("not implemented") // TODO call impl.getMany()
}

object Batched extends StrictLogging {

  private type Result[A]     = Error Either A
  private type Request[F[_]] = (Pair, Deferred[F, Result[Rate]])

  def apply[F[_]: ConcurrentEffect: Timer](
      config: RatesServiceConfig,
      impl: Algebra[F],
  ): F[Algebra[F]] = {
    val dummy = Pair(Currency.USD, Currency.USD) -> Deferred.unsafe[F, Result[Rate]]

    Topic[F, Request[F]](dummy) flatMap { topic =>
      val F = Concurrent[F]

      val stream = topic
        .subscribe(Int.MaxValue) // TODO max?
        .drop(1) // drop dummy
        .evalTap { case (pair, _) => F.delay(logger.debug(s"Pair enqueued: ${pair.show}")) }
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

      F.start(stream) *>
        new Batched[F](topic).asInstanceOf[Algebra[F]].pure
    }
  }
}
