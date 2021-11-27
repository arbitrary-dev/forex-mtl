package forex.http
package rates

import forex.domain.Currency.show
import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{ deriveConfiguredDecoder, deriveConfiguredEncoder }

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  // TODO test invalid Currency
  private implicit val currencyEncoder: Encoder[Currency] =
    Encoder.instance[Currency] { show.show _ andThen Json.fromString }

  private implicit val pairDecoder: Decoder[Pair] =
    deriveConfiguredDecoder[Pair]

  implicit val rateDecoder: Decoder[Rate] = (c) => {
    for {
      pair <- c.as[Pair]
      price <- c.downField("price").as[Price]
      timestamp <- c.downField("time_stamp").as[Timestamp]
    } yield {
      Rate(pair, price, timestamp)
    }
  }

  implicit val responseEncoder: Encoder[GetApiResponse] =
    deriveConfiguredEncoder[GetApiResponse]

}
