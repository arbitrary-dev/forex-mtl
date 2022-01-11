package forex.programs.rates

import cats.effect.IO
import cats.scalatest.EitherValues
import forex.config.Config.uriReader
import forex.config.{CacheServiceConfig, Config, RatesServiceConfig}
import forex.domain.Currency._
import forex.services._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext.global

class ProgramIntSpec extends AnyWordSpec
  with EitherValues
  with ScalaCheckPropertyChecks
  with Matchers {

  implicit val config =
    PropertyCheckConfiguration(
      minSize = 10000,
      sizeRange = 10000,
      minSuccessful = 10000,
    )

  "Program for rates" should {
    "handle 10000 requests" in {
      val program = programIO.unsafeRunSync()
      forAll { (req: Protocol.GetRatesRequest) =>
        val response = program.get(req).unsafeRunSync()
        val rate = response.value
        rate.pair.from shouldBe req.from
        rate.pair.to shouldBe req.to
      }
    }
  }

  val currencyGen = Gen.oneOf(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD)

  val reqGen =
    for {
      from <- currencyGen
      to <- currencyGen.suchThat(_ != from)
    } yield Protocol.GetRatesRequest(from, to)

  implicit val reqArb = Arbitrary(reqGen)

  implicit val contextShift = IO.contextShift(global)
  implicit val timer = IO.timer(global)

  val programIO =
    for {
      cacheConfig <- Config.load[IO, CacheServiceConfig]("app.cache-service")
      cacheService = CacheServices.scaffeine[IO](cacheConfig)
      ratesConfig <- Config.load[IO, RatesServiceConfig]("app.rates-service")
      ratesService <- RatesServices.live[IO](ratesConfig, cacheService, global)
    } yield {
      Program(ratesService)
    }
}
