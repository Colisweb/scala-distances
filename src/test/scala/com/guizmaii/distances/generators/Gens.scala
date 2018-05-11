package com.guizmaii.distances.generators

import com.guizmaii.distances.Types.{DirectedPath, LatLong, NonAmbigueAddress, PostalCode, TravelMode}
import com.guizmaii.distances.providers.Toto
import org.scalacheck.{Arbitrary, Gen}

object Gens {

  final val nameGen: Gen[String] = Gen.oneOf("toto", "tata", "titi", "tutu")
  final val ageGen: Gen[Int]     = Gen.choose(0, 150)

  final val latLongGen: Gen[LatLong] = for {
    lat: Double  <- Gen.choose(0.0, 180.0)
    long: Double <- Gen.choose(0.0, 180.0)
  } yield LatLong(latitude = lat, longitude = long)

  import enumeratum.scalacheck._

  final val travelModeGen: Gen[TravelMode] = implicitly[Arbitrary[TravelMode]].arbitrary

  final val directedPathGen: Gen[DirectedPath] = for {
    origin      <- latLongGen
    destination <- latLongGen
    travelModes <- Gen.nonEmptyListOf(travelModeGen)
  } yield
    DirectedPath(
      origin = origin,
      destination = destination,
      travelModes = travelModes
    )

  final val postalCodeGen: Gen[PostalCode] = Gen.listOfN(5, Gen.alphaNumChar).map(cs => PostalCode(cs.mkString))

  final val nonAmbigueAddressGen: Gen[NonAmbigueAddress] = for {
    line1      <- Gen.alphaNumStr
    line2      <- Gen.alphaNumStr
    postalCode <- Gen.listOfN(5, Gen.alphaNumChar).map(_.mkString)
    town       <- Gen.alphaNumStr
    country    <- Gen.alphaNumStr
  } yield
    NonAmbigueAddress(
      line1 = line1,
      line2 = line2,
      postalCode = postalCode,
      town = town,
      country = country
    )

  final val totoGen: Gen[Toto] = for {
    name              <- nameGen
    age               <- ageGen
    directedPath      <- directedPathGen
    postalCode        <- postalCodeGen
    nonAmbigueAddress <- nonAmbigueAddressGen
  } yield
    Toto(
      name = name,
      age = age,
      directedPath = directedPath,
      postalCode = postalCode,
      nonAmbigueAddress = nonAmbigueAddress
    )
}
