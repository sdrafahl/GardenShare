package com.gardenShare.gardenshare.domain.Entities

import eu.timepit.refined.api.Refined
import eu.timepit.refined.types.string.NonEmptyString

case class GetproductDescriptionCommand(key: NonEmptyString)
