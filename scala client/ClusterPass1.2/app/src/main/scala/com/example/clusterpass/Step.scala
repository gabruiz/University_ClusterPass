package com.example.clusterpass


class Step() {
  private var lon = .0
  private var lat = .0
  private var timestamp = 0L

  def this(lon: Double, lat: Double, timestamp: Long) {
    this()
    this.lon = lon
    this.lat = lat
    this.timestamp = timestamp
  }

  def getLon: Double = lon

  def getLat: Double = lat

  def getTimestamp: Long = timestamp

  def setLon(lon: Double): Unit = {
    this.lon = lon
  }

  def setLat(lat: Double): Unit = {
    this.lat = lat
  }

  def setTimestamp(timestamp: Long): Unit = {
    this.timestamp = timestamp
  }

  override def toString: String = "lon=" + lon.toString + ", lat=" + lat.toString + ", timestamp=" + timestamp.toString
}
