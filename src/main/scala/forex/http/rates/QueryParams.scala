package forex.http.rates

import forex.domain.{ Currency, Rate }
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.{ParseFailure, QueryParamDecoder, QueryParamEncoder}

object QueryParams {

  private[http] implicit val currencyQueryParamDecoder: QueryParamDecoder[Currency] =
    QueryParamDecoder[String] emap { s =>
      Currency.fromString(s).toRight(ParseFailure("Unsupported currency", s"Currency not supported: $s"))
    }

  implicit val pairQueryParamEncoder: QueryParamEncoder[Rate.Pair] =
    QueryParamEncoder.fromShow

  object FromQueryParam extends QueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends QueryParamDecoderMatcher[Currency]("to")

}
