package com.gardenShare.gardenshare.CreateGarden

import com.gardenShare.gardenshare.GardenData.Garden

abstract class CreateGarden[F[_]] {
  def createGarden(garden: Garden): F[Garden]
}
