package forex.config

import org.http4s.Uri

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    ratesService: RatesServiceConfig,
    cacheService: CacheServiceConfig,
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class RatesServiceConfig(
    uri: Uri,
    token: String,
)

case class CacheServiceConfig(
    size: Int,
    expiration: FiniteDuration,
)
