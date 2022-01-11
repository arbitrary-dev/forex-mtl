package forex.services.rates.interpreters

import cats.effect.IO
import cats.effect.laws.util.TestContext
import cats.syntax.either._
import forex.config.RatesServiceConfig
import forex.domain.Currency._
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.services._
import forex.services.rates.errors.Error
import forex.services.rates.utils.Middleware
import org.http4s.Uri
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class MiddlewareSpec extends AnyWordSpec with MockFactory with Matchers {

  import MiddlewareSpec._

  "Middleware for OneFrame rates service" should {

    "batch requests by count" in {
      val service = Middleware[IO](config, impl, CacheServices.dummy).unsafeRunSync()
      ctx.tick()

      service.get(pair1).unsafeRunAsyncAndForget()
      ctx.tick(20.millis)
      service.get(pair2).unsafeRunAsyncAndForget()
      ctx.tick(20.millis)
      service.get(pair3).unsafeRunAsyncAndForget()
      ctx.tick(20.millis)
      service.get(pair4).unsafeRunAsyncAndForget()
      ctx.tick(20.millis)
      service.get(pair5).unsafeRunAsyncAndForget()

      (impl.getMany _)
        .expects(where[List[Pair]] { list =>
          list should contain allOf (pair1, pair2, pair3, pair5)
          true
        })
        .returning(IO.pure(List.empty[Rate].asRight[Error]))

      ctx.tick()
    }

    "batch requests within time interval" in {
      val service = Middleware[IO](config, impl, CacheServices.dummy).unsafeRunSync()
      ctx.tick()

      service.get(pair1).unsafeRunAsyncAndForget()
      ctx.tick(25.millis)
      service.get(pair2).unsafeRunAsyncAndForget()
      ctx.tick(25.millis)
      service.get(pair3).unsafeRunAsyncAndForget()

      (impl.getMany _)
        .expects(where[List[Pair]] { list =>
          list should contain allOf (pair1, pair2, pair3)
          true
        })
        .returning(IO.pure(List.empty[Rate].asRight[Error]))
      ctx.tick(50.millis)

      (impl.getMany _)
        .expects(where[List[Pair]] { list =>
          list should contain only pair4
          true
        })
        .returning(IO.pure(List.empty[Rate].asRight[Error]))
      service.get(pair4).unsafeRunAsyncAndForget()
      ctx.tick(100.millis)
    }
  }

  val ctx            = TestContext()
  implicit val cs    = ctx.contextShift[IO]
  implicit val timer = ctx.timer[IO]

  val config =
    RatesServiceConfig(
      uri = Uri.unsafeFromString("https://localhost"),
      token = "no token",
      batchSize = 5,
      batchLinger = 100.millis,
    )

  val impl = mock[RatesService[IO]]
}

private object MiddlewareSpec {

  val pair1 = Pair(USD, USD)
  val pair2 = Pair(EUR, EUR)
  val pair3 = Pair(NZD, NZD)
  val pair4 = Pair(AUD, AUD)
  val pair5 = Pair(CHF, CHF)
}
