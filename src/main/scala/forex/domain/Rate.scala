package forex.domain

import cats.Show
import cats.syntax.show._
import forex.domain.Currency.show

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  implicit val showPair: Show[Pair] =
    Show.show(p => p.from.show + p.to.show)
}
