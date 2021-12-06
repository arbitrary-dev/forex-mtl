package forex

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  final val RatesServices = rates.Interpreters

  type CacheService[F[_]] = cache.Algebra[F]
  final val CacheServices = cache.Interpreters

  // TODO BatchServices (using fs2.Stream)
  //      To utilize OneFrame API's possibility of querying several pairs at once.
}
