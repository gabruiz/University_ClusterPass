package com.example.clusterpass

import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class AsynchttpGet(var listener: AsyncGetListener, var url: URL) {

  var response: StringBuilder = null


  def get(): Unit = {
    response = new StringBuilder

    val thread = new Thread(new Runnable() {
      override def run(): Unit = {
        try { //URL url = new URL(urls+"/"+lon+"/"+lat);
          Log.i("clusterpass", "connecting to: " + url)
          val urlConnection = url.openConnection.asInstanceOf[HttpURLConnection]
          urlConnection.setRequestMethod("GET")
          val statusCode = urlConnection.getResponseCode
          Log.i("clusterpass", "status code:  " + statusCode)
          if (statusCode == 200) {

            response.synchronized{
            val it = new BufferedInputStream(urlConnection.getInputStream)
            val read = new InputStreamReader(it)
            val buff = new BufferedReader(read)
            var chunks : String = null
            Log.i("clusterpass", "getting response in scala...")

            chunks = buff.readLine
            while ( {chunks != null}){
              Log.i("clusterpass", ""+chunks)
              response.append(chunks)
              chunks = buff.readLine
            }
              Log.i("clusterpass", "notify response in scala...")
            response.notify()
            }
          }
          else Log.i("clusterpass", "error: " + statusCode)
        } catch {
          case e: Exception =>
            Log.i("clusterpass", "exception: " + e)
            e.printStackTrace()
        }
      }
    })
    thread.start()
    try {
        Log.i("clusterpass", "wait for response in scala...")
        response.synchronized{ response.wait()
          Log.i("clusterpass", "send response in scala...")
        sendResponse()
      }


    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }

  private[clusterpass] def sendResponse(): Unit = {
    listener.onGetResponseReceived(response)
  }
}
