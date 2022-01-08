package forex.config

import cats.effect.Sync
import fs2.Stream
import org.http4s.Uri
import pureconfig.error.CannotConvert
import pureconfig.generic.auto._
import pureconfig.{ ConfigReader, ConfigSource, Derivation }

import scala.reflect.ClassTag

object Config {

  implicit val uriReader: ConfigReader[Uri] = cur =>
    cur.asString.flatMap { s =>
      Uri.fromString(s).left.flatMap(t => cur.failed(CannotConvert(s, "Uri", t.message)))
    }

  /** @param path
    *   the property path inside the default configuration
    */
  def stream[F[_]: Sync](path: String): Stream[F, ApplicationConfig] =
    Stream.eval(load[F, ApplicationConfig](path))

  /** @param path
    *   the property path inside the default configuration
    */
  def load[F[_]: Sync, A: ClassTag](path: String)(implicit reader: Derivation[ConfigReader[A]]): F[A] =
    Sync[F].delay(ConfigSource.default.at(path).loadOrThrow[A])
}
