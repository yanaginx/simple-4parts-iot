package com.example.datafetcher;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    // Variable
    TextView txtTemp;
    TextView txtHumidity;
    Button btnFetch;
    RequestQueue mQueue;

    MQTTHelper mqttHelper;

    LineChart lineChart;
    PieChart pieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtHumidity = findViewById(R.id.txtHumidity);
        txtTemp = findViewById(R.id.txtTemp);

        lineChart = findViewById(R.id.lineChart);
        pieChart = findViewById(R.id.pieChart);

        btnFetch = findViewById(R.id.btnFetch);

        mQueue = Volley.newRequestQueue(this);

        btnFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnFetch.setText("FETCHING");
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        getTemperature("shinyo_dc", "iot-project.iot-project-temp", txtTemp, lineChart);
                        getHumidity("shinyo_dc", "iot-project.iot-project-humidity", txtHumidity, pieChart);
                    }
                }, 0, 2000);
            }
        });
    }

//    // MQTT function
//    private void startMQTT() {
//        mqttHelper = new MQTTHelper(getApplicationContext());
//        mqttHelper.setCallback(new MqttCallbackExtended() {
//            @Override
//            public void connectComplete(boolean b, String s) {
//            }
//
//            @Override
//            public void connectionLost(Throwable throwable) {
//            }
//
//            @Override
//            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
//                getTemperature("shinyo_dc", "iot-project.iot-project-temp", txtTemp, lineChart);
//                txtHumidity.setText("MESSAGE ARRIVED");
////                getFeedData("shinyo_dc", "iot-project.iot-project-humidity", txtHumidity);
//                Log.d("MQTT", mqttMessage.toString());
//            }
//
//            @Override
//            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
//            }
//        });
//    }

    private void getTemperature(String userName, String feedKey, TextView view, LineChart chart){
        // Get current 20 data points
        // id, value, feed_id, feed_key, created_at, created_epoch, expiration
        String apiURL = "https://io.adafruit.com/api/v2/" + userName + "/feeds/" + feedKey + "/data?limit=20";

        JsonArrayRequest request = new JsonArrayRequest(apiURL, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                ArrayList<Entry> dataSet = new ArrayList<Entry>();
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject entry = response.getJSONObject(i);
                        String value = entry.getString("value");
                        dataSet.add(new Entry(i, Float.parseFloat(value)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        view.setText("PARSING ERROR");
                    }
                }
                java.util.Date date=new java.util.Date();
                view.setText("TEMP FETCHED AT: " + date.toString());
                LineDataSet lineDataSet = new LineDataSet(dataSet, "oC");

                lineDataSet.setColor(Color.BLUE);
                lineDataSet.setCircleColor(Color.CYAN);
                lineDataSet.setDrawCircles(true);
                lineDataSet.setDrawCircleHole(true);

                ArrayList<ILineDataSet> iLineDataSets = new ArrayList<>();
                iLineDataSets.add(lineDataSet);
                LineData lineData = new LineData(iLineDataSets);

                chart.setData(lineData);
                chart.getDescription().setEnabled(false);
                chart.invalidate();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                view.setText("PARSING ERROR");
            }
        });

        mQueue.add(request);
    }

    private void getHumidity(String userName, String feedKey, TextView view, PieChart chart){
        // Get lasted data points
        // id, value, feed_id, feed_key, created_at, created_epoch, expiration
        String apiURL = "https://io.adafruit.com/api/v2/" + userName + "/feeds/" + feedKey + "/data?limit=1";

        JsonArrayRequest request = new JsonArrayRequest(apiURL, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                ArrayList<PieEntry> dataSet = new ArrayList<PieEntry>();
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject entry = response.getJSONObject(i);
                        String value = entry.getString("value");
                        dataSet.add(new PieEntry(Float.parseFloat(value), "Moisture"));
                        dataSet.add(new PieEntry(100-Float.parseFloat(value), ""));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        view.setText("PARSING ERROR");
                    }
                }
                java.util.Date date=new java.util.Date();
                view.setText("HUMIDITY FETCHED AT: " + date.toString());
                PieDataSet pieDataSet = new PieDataSet(dataSet, "");
                pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
                pieDataSet.setValueTextColor(Color.BLACK);
                pieDataSet.setValueTextSize(16f);

                PieData pieData = new PieData(pieDataSet);

                chart.setData(pieData);
                chart.getDescription().setEnabled(false);
                chart.animate();
                chart.invalidate();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                view.setText("PARSING ERROR");
            }
        });

        mQueue.add(request);
    }

}