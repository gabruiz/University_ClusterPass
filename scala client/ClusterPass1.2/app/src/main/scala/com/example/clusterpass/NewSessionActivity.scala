package com.example.clusterpass

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.github.mikephil.charting.R
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson


object NewSessionActivity {
  private val MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1 // in Meters

  private val MINIMUM_TIME_BETWEEN_UPDATES = 1000 // in Milliseconds

}

class NewSessionActivity extends AppCompatActivity with LocationListener {

  var locationManager: LocationManager = null
  var startSessionButton: Button = null
  var endSessionButton: Button = null
  var tv_x_coord: TextView = null
  var tv_y_coord: TextView = null
  var tv_timestamp: TextView = null
  var jsonParam: JSONObject = null
  var stepList: util.ArrayList[Step] = null

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_new_session)

    getSupportActionBar.setDisplayHomeAsUpEnabled(true)

    startSessionButton = findViewById(R.id.startSessionButton).asInstanceOf[Button]
    endSessionButton = findViewById(R.id.endSessionButton).asInstanceOf[Button]
    tv_x_coord = findViewById(R.id.tv_x_coord).asInstanceOf[TextView]
    tv_y_coord = findViewById(R.id.tv_y_coord).asInstanceOf[TextView]
    tv_timestamp = findViewById(R.id.tv_timestamp).asInstanceOf[TextView]
    jsonParam = new JSONObject
    stepList = new util.ArrayList[Step]
    locationManager = getSystemService(Context.LOCATION_SERVICE).asInstanceOf[LocationManager]

    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { // TODO: Consider calling
      //    Activity#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for Activity#requestPermissions for more details.
      Log.i("clusterpass", "non ho i permessi")

      return
    }
    val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    locationManager.requestLocationUpdates("network", 1000L, 0.0f, this)

    startSessionButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View): Unit = {
        Log.i("clusterpass", "Monitoring started")

        showCurrentLocation()
      }
    })

    endSessionButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View): Unit = {
        Log.i("clusterpass", "Monitoring ended")

        sendPost()
      }
    })

  }

  override def onPause(): Unit = {
    super.onPause()
    locationManager.removeUpdates(this)
  }

  override def onDestroy(): Unit = {
    locationManager.removeUpdates(this)
    super.onDestroy()
  }

  protected def showCurrentLocation(): Unit = {
    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      Log.i("clusterpass", "non ho i permessi")

      return
    }
    var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    if (location == null) location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    if (location != null) {
      tv_x_coord.setText("" + location.getLongitude)
      tv_y_coord.setText("" + location.getLatitude)
      val timestamp = System.currentTimeMillis
      val ts = timestamp.toString
      tv_timestamp.setText("" + ts)
    }
  }

  def sendPost(): Unit = {
    Log.i("clusterpass", "send post invoked")
    val thread = new Thread(new Runnable() {
      override def run(): Unit = {
        try {
          Log.i("clusterpass", "try")
          val url = new URL("http://151.97.159.126:8000/api/createSteps/")
          val conn = url.openConnection.asInstanceOf[HttpURLConnection]
          Log.i("clusterpass", "connection....")
          conn.setRequestMethod("POST")
          conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
          conn.setRequestProperty("Accept", "application/json")
          conn.setDoOutput(true)
          conn.setDoInput(true)
          val gson = new Gson
          var json = gson.toJson(stepList)
          json = json.replace(":", ":\"")
          json = json.replace(",\"", "\",\"")
          json = json.replace("}", "\"}")
          Log.i("clusterpass", "json after replacement: " + json)
          val os = new DataOutputStream(conn.getOutputStream)
          //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
          os.writeBytes(json)
          os.flush()
          os.close()
          Log.i("clusterpass", String.valueOf(conn.getResponseCode))
          Log.i("clusterpass", conn.getResponseMessage)
          conn.disconnect()
        } catch {
          case e: Exception =>
            Log.i("clusterpass", "" + e)
            e.printStackTrace()
        }
      }
    })
    thread.start()
  }

  override def onLocationChanged(location: Location): Unit = { // TODO Auto-generated method stub
    var lat = .0
    var lon = .0
    var timestamp = 0L
    lon = location.getLongitude
    lat = location.getLatitude
    timestamp = System.currentTimeMillis
    tv_x_coord.setText("" + lon)
    tv_y_coord.setText("" + lat)
    val ts = timestamp.toString
    tv_timestamp.setText("" + ts)
    val step = new Step(lon, lat, timestamp)
    stepList.add(step)
    //Log.i("clusterpass", ""+stepList);
    Log.i("clusterpass", "json")

    tv_timestamp.setText(stepList.toString)
  }

  override def onStatusChanged(s: String, i: Int, b: Bundle): Unit = {

  }

  override def onProviderDisabled(s: String): Unit = {

  }

  override def onProviderEnabled(s: String): Unit = {
    System.out.println("==onProviderEnabled=" + s)

  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = { // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater.inflate(R.menu.main, menu)
    true
  }

  override def onSupportNavigateUp: Boolean = {
    onBackPressed()
    true
  }
}