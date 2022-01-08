package forex.services.rates.interpreters

import cats.effect.ConcurrentEffect
import cats.syntax.functor._
import forex.config.RatesServiceConfig
import forex.domain.Rate
import forex.http.rates.Protocol.rateDecoder
import forex.http.rates.QueryParams.pairQueryParamEncoder
import forex.services.rates.Algebra
import forex.services.rates.errors._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{ Header, Headers, Request }

import scala.concurrent.ExecutionContext

class OneFrameLive[F[_]: ConcurrentEffect](
    config: RatesServiceConfig,
    ec: ExecutionContext,
) extends Algebra[F] {

  private val headers = Headers.of(Header("token", config.token))

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    BlazeClientBuilder[F](ec).resource.use { client =>
      val request = Request[F](
        uri = config.uri +? ("pair", pair),
        headers = headers,
      )
      // TODO log errors
      // TODO log calls
      client
        .expect[List[Rate]](request)
        .map(_.headOption.toRight(Error.OneFrameLookupFailed("no rate for pair")))
    }
}
