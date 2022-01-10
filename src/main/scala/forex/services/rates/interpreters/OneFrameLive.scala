package forex.services.rates.interpreters

import cats.data.EitherT
import cats.effect.ConcurrentEffect
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.functor._
import com.typesafe.scalalogging.StrictLogging
import forex.config.RatesServiceConfig
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.http.rates.Protocol.rateDecoder
import forex.http.rates.QueryParams.pairQueryParamEncoder
import forex.services.rates.Algebra
import forex.services.rates.errors._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{ Header, Headers, Request }

import scala.concurrent.ExecutionContext

// TODO log errors
class OneFrameLive[F[_]: ConcurrentEffect](
    config: RatesServiceConfig,
    ec: ExecutionContext,
) extends Algebra[F]
    with StrictLogging {

  private val headers = Headers.of(Header("token", config.token))

  override def getMany(pairs: List[Pair]): F[Error Either List[Rate]] =
    BlazeClientBuilder[F](ec).resource.use { client =>
      val uri     = config.uri +? ("pair", pairs)
      val request = Request[F](uri = uri, headers = headers)
      logger.debug(s"Request for: $uri")
      client
        .expect[List[Rate]](request)
        .map(_.asRight[Error])
        .handleError(t => Error.OneFrameLookupFailed(t.getMessage).asLeft)
    }

  override def get(pair: Pair): F[Error Either Rate] =
    EitherT(getMany(List(pair))).map(_.head).value
}
