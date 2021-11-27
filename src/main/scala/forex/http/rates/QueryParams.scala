package forex.http.rates

import forex.domain.{ Currency, Rate }
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.{ QueryParamDecoder, QueryParamEncoder }

object QueryParams {

  private[http] implicit val currencyQueryParamDecoder: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].map(Currency.fromString)

  implicit val pairQueryParamEncoder: QueryParamEncoder[Rate.Pair] =
    QueryParamEncoder.fromShow

  object FromQueryParam extends QueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends QueryParamDecoderMatcher[Currency]("to")

}
