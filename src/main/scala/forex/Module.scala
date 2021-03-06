package forex

import cats.effect.{ ConcurrentEffect, Timer }
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import fs2.Stream
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }

import scala.concurrent.ExecutionContext

class Module[F[_]: ConcurrentEffect: Timer](
    config: ApplicationConfig,
    ratesService: RatesService[F],
) {

  private val ratesProgram: RatesProgram[F]  = RatesProgram[F](ratesService)
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

object Module {

  def stream[F[_]: ConcurrentEffect: Timer](
      config: ApplicationConfig,
      ec: ExecutionContext,
  ): Stream[F, Module[F]] =
    for {
      cache <- Stream(CacheServices.scaffeine[F](config.cacheService))
      ratesService <- Stream.eval(RatesServices.live[F](config.ratesService, cache, ec))
    } yield new Module(config, ratesService)
}
