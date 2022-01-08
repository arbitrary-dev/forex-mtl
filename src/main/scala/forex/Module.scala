package forex

import cats.effect.{ ConcurrentEffect, Timer }
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }

import scala.concurrent.ExecutionContext

class Module[F[_]: ConcurrentEffect: Timer](config: ApplicationConfig, ec: ExecutionContext) {

  private val ratesService: RatesService[F] = RatesServices.live[F](config.ratesService, ec)
  private val cacheService: CacheService[F] = CacheServices.scaffeine[F](config.cacheService)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService, cacheService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = { http: HttpRoutes[F] =>
    AutoSlash(http)
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
