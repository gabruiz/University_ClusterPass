package com.example.clusterpass

import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.github.mikephil.charting.R
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONArray
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL
import java.security.Key
import java.util
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

import scala.collection.JavaConversions._

import scala.util.control.Breaks._

class ShowPathActivity extends AppCompatActivity with OnMapReadyCallback with AsyncGetListener with OnChartValueSelectedListener {

  var mapFragment: SupportMapFragment = null
  var googleMap: GoogleMap = null
  var latLngList: util.List[LatLng] = null
  var barChart: BarChart = null

  override protected def onCreate(savedInstanceState: Bundle): Unit = {

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_show_path)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)

    mapFragment = getSupportFragmentManager.findFragmentById(R.id.map2).asInstanceOf[SupportMapFragment]
    mapFragment.getMapAsync(this)



    val b = getIntent.getExtras
    var lat = 0d
    var lon = 0d
    if (b != null) {
      lat = b.getDouble("lat")
      lon = b.getDouble("lon")
    }

    val apiEndpoint = "http://151.97.159.126:8000/api/getClusterByPosition"
    var url : URL = null
    try
      url = new URL(apiEndpoint + "/" + lon + "/" + lat)
    //url = new URL ("https://api.myjson.com/bins/1bvgjq");
    catch {
      case e: MalformedURLException =>
        Log.i("clusterpass", "errore: " + e)
    }


    val getClusterByPosition = new AsynchttpGet(this, url)
    getClusterByPosition.get()
    val apiEndpoint2 = "http://151.97.159.126:8000/api/getTimestampsByCluster"
    var url2 : URL = null
    try
      url2 = new URL(apiEndpoint2 + "/" + lon + "/" + lat)
    //url2 = new URL ("https://api.myjson.com/bins/beeka");
    catch {
      case e: MalformedURLException =>
        Log.i("clusterpass", "errore: " + e)
    }
    val getTimestamps = new AsynchttpGet(this, url2)
    getTimestamps.get()
    barChart = findViewById(R.id.chart_view).asInstanceOf[BarChart]
  }

  def drawPath(googleMap: GoogleMap, latLngList: util.List[LatLng]): Unit = {
    Log.i("clusterpass", "drawPath called, map: " + googleMap)
    mapFragment.getMapAsync(this)
    // Position the map's camera near Alice Springs in the center of Australia,
    // and set the zoom factor so most of Australia shows on the screen.
  }

  def drawChart(entries: util.ArrayList[BarEntry]): Unit = {
    Log.i("clusterpass", "drawChart called, entries: " + entries)
    barChart = findViewById(R.id.chart_view).asInstanceOf[BarChart]
    val barDataSet = new BarDataSet(entries, "")
    barDataSet.setBarBorderWidth(0)
    barDataSet.setFormLineWidth(0)
    barDataSet.setColors(Color.BLUE)
    val barData = new BarData(barDataSet)
    val xAxis = barChart.getXAxis
    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM)
    val x_axis_values = new Array[String](entries.size)
    var i = 0

    for (entry <- entries) {
      x_axis_values({
        i += 1; i - 1
      }) = "" + entry.getX.toInt
    }
    val formatter = new IndexAxisValueFormatter(x_axis_values)
    xAxis.setGranularity(1f)
    xAxis.setValueFormatter(formatter)
    barChart.setData(barData)
    barChart.setFitBars(false)
    barChart.getXAxis.setDrawGridLines(false)
    barChart.getAxisLeft.setDrawGridLines(false)
    barChart.getAxisRight.setDrawGridLines(false)
    barChart.animateXY(0, 1000)
    barChart.invalidate()
  }

  override def onSupportNavigateUp: Boolean = {
    onBackPressed()
    true
  }

  override def onMapReady(googleMap: GoogleMap): Unit = {
    Log.i("clusterpass", "google map ready called")
    this.googleMap = googleMap
    val polylineOptions = new PolylineOptions
    polylineOptions.color(Color.BLUE)

    for (ll <- latLngList) {
      polylineOptions.add(ll)
    }
    polylineOptions.clickable(true)
    try
      googleMap.addPolyline(polylineOptions)
    catch {
      case e: Exception =>
        Log.i("clusterpass", "errore: " + e)
    }
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngList.get(0), 20))
    Log.i("clusterpass", "drawing polyline...")
  }

  def onGetResponseReceived(response: StringBuilder): Unit = {
    Log.i("clusterpass", "response received: " + response)
    try { //JSONArray first = (JSONArray) new JSONArray(response.toString());
      val jsonObject = new JSONObject(response.toString).asInstanceOf[JSONObject]
      val keys = jsonObject.keys
      breakable{
        while (keys.hasNext) {
          val key = keys.next.asInstanceOf[String]
          if (key.equalsIgnoreCase("steps")) {
            latLngList = new util.ArrayList[LatLng]
            val jsonArray = jsonObject.get("steps").asInstanceOf[JSONArray]
            var i = 0
            while (i < jsonArray.length) {
              val jSONObject = jsonArray.get(i).asInstanceOf[JSONObject]
              val lat = jSONObject.get("lat").toString.toDouble
              val lon = jSONObject.get("lon").toString.toDouble
              val ll = new LatLng(lat, lon)
              latLngList.add(ll)

              i += 1
            }
            drawPath(googleMap, latLngList)
            break //todo: break is not supported
          }
          else if (key.equalsIgnoreCase("timestamps")) {
            Log.i("clusterpass", "ricevuti timestamp: " + jsonObject)
            val jsonArray = jsonObject.get("timestamps").asInstanceOf[JSONArray]
            Log.i("clusterpass", "array: " + jsonArray)
            val entries = new util.ArrayList[BarEntry]
            var i = 0
            while (i < jsonArray.length) {
              Log.i("clusterpass", "x= " + i + " y= " + jsonArray.get(i))
              val x = i.toFloat
              val y = jsonArray.get(i).toString.toFloat
              entries.add(new BarEntry(x, y))

              i += 1
            }
            drawChart(entries)
            break //todo: break is not supported
          }
          else Log.i("clusterpass", "key received: " + key)
        }
      }
    } catch {
      case ex: Exception =>
        Log.i("clusterpass", "errore: " + ex)
        ex.printStackTrace()
    }
  }

  override def onValueSelected(e: Entry, h: Highlight): Unit = {
  }

  override def onNothingSelected(): Unit = {
  }
}
