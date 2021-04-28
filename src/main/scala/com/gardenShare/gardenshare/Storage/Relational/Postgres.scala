package com.gardenShare.gardenshare

import slick.jdbc.PostgresProfile.api._

object PostGresSetup {
  def createPostgresClient = Database.forConfig("postgres")
}
