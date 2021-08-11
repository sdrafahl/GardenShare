package com.gardenShare.gardenshare

import eu.timepit.refined.types.string

case class Bucket(n: string.NonEmptyString)
