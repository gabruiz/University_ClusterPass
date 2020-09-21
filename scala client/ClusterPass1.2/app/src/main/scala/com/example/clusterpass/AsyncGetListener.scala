package com.example.clusterpass


trait AsyncGetListener {
  def onGetResponseReceived(result: StringBuilder): Unit
}
