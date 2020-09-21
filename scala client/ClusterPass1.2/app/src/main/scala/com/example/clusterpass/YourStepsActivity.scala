package com.example.clusterpass

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.github.mikephil.charting.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.Bucket
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.util
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import scala.collection.JavaConversions._
import scala.util.control.Breaks._

class YourStepsActivity extends AppCompatActivity with View.OnClickListener {
  var dailyBtn: Button = null
  var weeklyBtn: Button  = null
  var monthlyBtn: Button  = null
  var buttonList: util.List[Button]  = null
  var tvSteps: TextView = null
  var monthlySteps = 0
  var weeklySteps = 0
  var dailySteps = 0

  override protected def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_your_steps)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    dailyBtn = findViewById(R.id.dailyBtn).asInstanceOf[Button]
    weeklyBtn = findViewById(R.id.weeklyBtn).asInstanceOf[Button]
    monthlyBtn = findViewById(R.id.monthlyBtn).asInstanceOf[Button]
    buttonList = new util.ArrayList[Button]
    buttonList.add(dailyBtn)
    buttonList.add(weeklyBtn)
    buttonList.add(monthlyBtn)
    import scala.collection.JavaConversions._
    for (b <- buttonList) {
      b.setOnClickListener(this)
    }
    tvSteps = findViewById(R.id.tv_steps).asInstanceOf[TextView]
    val cal = Calendar.getInstance
    val now = new Date
    // till now
    cal.setTime(now)
    val endTime = cal.getTimeInMillis
    cal.add(Calendar.DAY_OF_YEAR, -30)
    val startTime = cal.getTimeInMillis

    val ESTIMATED_STEP_DELTAS = new DataSource.Builder()
      .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
      .setType(DataSource.TYPE_DERIVED).setStreamName("estimated_steps")
      .setAppPackageName("com.google.android.gms").build

    val readRequest = new DataReadRequest.Builder().aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
      .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
      .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
      .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
      .bucketByTime(1, TimeUnit.DAYS).setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
      .build

    Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
      .readData(readRequest)
      .addOnSuccessListener(new OnSuccessListener[DataReadResponse]() {
      override def onSuccess(dataReadResponse: DataReadResponse): Unit = {
        val bucketList = dataReadResponse.getBuckets
        Log.i("clusterpass", "Data Response: " + dataReadResponse)
        var cumulativeSteps = 0
        var i = 0
        while ( i < bucketList.size) {

          //Log.i("clusterpass", "bucket: " +b);
          cumulativeSteps = cumulativeSteps + bucketList.get(i).getDataSet(DataType.TYPE_STEP_COUNT_DELTA).getDataPoints.get(0).getValue(Field.FIELD_STEPS).asInt
          if (i == 0) dailySteps = cumulativeSteps
          else if (i == 7) weeklySteps = cumulativeSteps

          i += 1
        }

        monthlySteps = cumulativeSteps
        Log.i("clusterpass", "passi di oggi: " + dailySteps)
        Log.i("clusterpass", "passi di questa settimana: " + weeklySteps)
        Log.i("clusterpass", "passi di questo mese: " + monthlySteps)
        dailyBtn.performClick
      }
    })
      .addOnFailureListener(new OnFailureListener() {
      override def onFailure(@NonNull e: Exception): Unit = {
        Log.w("clusterpass", "There was a problem getting the step count.", e)
      }
    })
  }

  override def onSupportNavigateUp: Boolean = {
    onBackPressed()
    true
  }

  override def onClick(v: View): Unit = {
    Log.i("clusterpass", "v:" + v.getId)


    for (b <- buttonList) {
      Log.i("clusterpass", "b:" + b.getId)
      if (b.getId == v.getId) {
        b.setTextColor(ContextCompat.getColor(this, R.color.clusterpassWhite))
        b.setBackgroundColor(ContextCompat.getColor(this, R.color.clusterpassOrange))

        breakable {


        b.getId match {
          case R.id.dailyBtn =>
            tvSteps.setText("" + dailySteps)
            break //todo: break is not supported
          case R.id.weeklyBtn =>
            tvSteps.setText("" + weeklySteps)
            break //todo: break is not supported
          case R.id.monthlyBtn =>
            tvSteps.setText("" + monthlySteps)
            break //todo: break is not supported
        }

      }
      }
      else {
        b.setTextColor(ContextCompat.getColor(this, R.color.clusterpassOrange))
        b.setBackgroundColor(ContextCompat.getColor(this, R.color.clusterpassWhite))
      }
    }

  }
}
