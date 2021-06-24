package com.gardenShare.gardenshare

abstract class RoutesTypes
case class TestingAndProductionRoutes() extends RoutesTypes
case class OnlyProductionRoutes() extends RoutesTypes
