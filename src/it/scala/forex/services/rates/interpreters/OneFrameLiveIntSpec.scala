package forex.services.rates.interpreters

import cats.effect.IO
import cats.scalatest.{EitherMatchers, EitherValues}
import forex.config.Config.uriReader
import forex.config.{Config, RatesServiceConfig}
import forex.domain.Currency.{EUR, USD}
import forex.domain.Rate
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext.global

class OneFrameLiveIntSpec extends AsyncWordSpec
  with EitherMatchers
  with EitherValues
  with Matchers {

  "OneFrameLive" should {
    "get rate" in {
      (for {
        service <- serviceIO
        response <- service.get(Rate.Pair(USD, EUR))
      } yield {
        val rate = response.value
        rate.pair.from shouldBe USD
        rate.pair.to shouldBe EUR
      }).unsafeToFuture()
    }
  }

  implicit val contextShift = IO.contextShift(global)

  val serviceIO =
    for {
      cfg <- Config.load[IO, RatesServiceConfig]("app.rates-service")
    } yield {
      new OneFrameLive[IO](cfg, global)
    }
}
