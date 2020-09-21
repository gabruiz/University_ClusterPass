package com.example.clusterpass

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.github.mikephil.charting.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline
import com.google.gson.JsonParser
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL
import java.util

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

import scala.collection.JavaConversions._

class FindPathActivity extends AppCompatActivity with LocationListener with OnMapReadyCallback with GoogleMap.OnPolylineClickListener with GoogleMap.OnPolygonClickListener with AsyncGetListener with GoogleMap.OnMarkerClickListener {

  var locationManager: LocationManager = null

  var et_lon: EditText = null
  var et_lat: EditText = null
  var mapFragment: SupportMapFragment = null
  var googleMap: GoogleMap = null
  val result: StringBuilder = null
  val jsonParam = null
  val stepList = null

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_find_path)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    et_lon = findViewById(R.id.input_lon).asInstanceOf[EditText]
    et_lat = findViewById(R.id.input_lat).asInstanceOf[EditText]
    mapFragment = getSupportFragmentManager.findFragmentById(R.id.map).asInstanceOf[SupportMapFragment]
    mapFragment.getMapAsync(this)
    /*
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.find_path_drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view_find_path);
            navigationView.bringToFront();
            navigationView.setCheckedItem(R.id.nav_find_path);
            navigationView.setNavigationItemSelectedListener(this);


    */
    val fab: FloatingActionButton = findViewById(R.id.fab)
    fab.setOnClickListener(new View.OnClickListener() {
      override def onClick(view: View): Unit = {
        Log.i("clusterpass", "sending coordinates...")
        getCenters()
      }
    })


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
      et_lon.setText("" + location.getLongitude)
      et_lat.setText("" + location.getLatitude)
      val timestamp = System.currentTimeMillis
      val ts = timestamp.toString
    }
  }

  def getCenters(): Unit = {
    Log.i("clusterpass", "send post invoked")
    val lon = et_lon.getText.toString.toDouble
    val lat = et_lat.getText.toString.toDouble
    val apiEndpoint = "http://151.97.159.126:8000/api/getCenters"
    var url: URL = null
    try
      url = new URL(apiEndpoint + "/" + lon + "/" + lat)
    //url = new URL ("https://api.myjson.com/bins/7ogq6");
    catch {
      case e: MalformedURLException =>
        e.printStackTrace()
    }
    val getter = new AsynchttpGet(this, url)
    getter.get()
  }

  override def onLocationChanged(location: Location): Unit = { // TODO Auto-generated method stub
    var lat = .0
    var lon = .0
    val timestamp = 0L
    lon = location.getLongitude
    lat = location.getLatitude
    et_lon.setText("" + lon)
    et_lat.setText("" + lat)
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

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    Log.i("clusterpass", "setting button pressed")
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    val id = item.getItemId
    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) return true
    super.onOptionsItemSelected(item)
  }

  def drawMap(centersList: util.List[LatLng]): Unit = { //mapFragment.getMapAsync(this);
    var lat = 0.0
    var lon = 0.0
    try {
      lat = et_lat.getText.toString.toDouble
      lon = et_lon.getText.toString.toDouble
    } catch {
      case e: Exception =>

    }
    Log.i("clusterpass", "lat: " + lat + " " + "lon: " + lon)
    val latlng_user = new LatLng(lat, lon)
    Log.i("clusterpass", "drawing markers...")
    Log.i("clusterpass", "" + centersList)
    val markerOptions = new MarkerOptions
    markerOptions.position(latlng_user)
    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.ic_user_marker)))
    markerOptions.title("current_position")
    googleMap.addMarker(markerOptions)

    for (ll <- centersList) {
      googleMap.addMarker(new MarkerOptions().position(ll).title(""))
    }
    // Position the map's camera near Alice Springs in the center of Australia,
    // and set the zoom factor so most of Australia shows on the screen.
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng_user, 10))
    // Set listeners for click events.
    googleMap.setOnPolylineClickListener(this)
    googleMap.setOnPolygonClickListener(this)
  }

  private def getBitmap(drawableRes: Int) = {
    val drawable = getResources.getDrawable(drawableRes, null)
    val canvas = new Canvas
    val bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth, drawable.getIntrinsicHeight, Bitmap.Config.ARGB_8888)
    canvas.setBitmap(bitmap)
    drawable.setBounds(0, 0, drawable.getIntrinsicWidth, drawable.getIntrinsicHeight)
    drawable.draw(canvas)
    bitmap
  }

  override def onMarkerClick(marker: Marker): Boolean = {
    Log.i("clusterpass", "you clicked the marker " + marker.getPosition)
    if (!marker.getTitle.equalsIgnoreCase("current_position")) {
      val lon = marker.getPosition.longitude
      val lat = marker.getPosition.latitude
      val intent = new Intent(this, classOf[ShowPathActivity])
      val b = new Bundle
      b.putDouble("lon", lon) //Your id

      b.putDouble("lat", lat)
      intent.putExtras(b) //Put your id to your next Intent

      startActivity(intent)
    }
    //finish();
    true
  }

  override def onMapReady(googleMap: GoogleMap): Unit = {
    Log.i("clusterpass", "google map ready called")
    this.googleMap = googleMap
    this.googleMap.setOnMarkerClickListener(this)
  }

  override def onPolygonClick(polygon: Polygon): Unit = {
  }

  override def onPolylineClick(polyline: Polyline): Unit = {
  }

  def onGetResponseReceived(response: StringBuilder): Unit = {
    Log.i("clusterpass", "response received: " + response)
    var jsonArray: JSONArray = null
    val latLngList = new util.ArrayList[LatLng]
    try {
      jsonArray = new JSONArray(response.toString)
      Log.i("clusterpass", "result: " + jsonArray)
      var i = 0
      while ( i < jsonArray.length) {
        Log.i("clusterpass", "jsonObject: " + jsonArray.get(i))
        val parser = new JsonParser
        val jSONObject = jsonArray.get(i).asInstanceOf[JSONObject]
        val lat = jSONObject.get("lat").toString.toDouble
        val lon = jSONObject.get("lon").toString.toDouble
        val ll = new LatLng(lat, lon)
        latLngList.add(ll)

        i += 1
      }
    } catch {
      case ex: JSONException =>
        ex.printStackTrace()
    }
    Log.i("clusterpass", "draw map con " + latLngList)
    drawMap(latLngList)
  }

  override def onSupportNavigateUp: Boolean = {
    onBackPressed()
    true
  }
}