package com.example.uart_mqtt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.BuildConfig;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {

    // Variable

    final String TAG = "MAIN_TAG";
    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.LIBRARY_PACKAGE_NAME + ".GRANT_USB";

    UsbSerialPort port;

    MQTTHelper mqttHelper;

    TextView txtData;
    Button btnSend;

    private boolean connected = false;

    Context context;
    CharSequence text;
    int duration;

    int len;
    int WRITE_WAIT_MILLIS = 500;
    int READ_WAIT_MILLIS = 2000;

    Boolean enabled = false;

    private final Handler mainLooper = new Handler(Looper.getMainLooper());

    // MQTT function
    private void startMQTT() {
        mqttHelper = new MQTTHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
            }

            @Override
            public void connectionLost(Throwable throwable) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.d("MQTT", mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
            }
        });
    }

    private void sendDataToMQTT(String topic, String data) {
        MqttMessage msg = new MqttMessage();
        msg.setId(1234);            // each message has its own id
        msg.setQos(0);              // Quality of service -> 0 is fastest
        msg.setRetained(true);

        byte[] b = data.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);
        } catch (MqttException e) {
        }
    }
    //=======================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtData = findViewById(R.id.txtData);
        btnSend = findViewById(R.id.btnSend);


        context = getApplicationContext();
        text = "DEBUG!";
        duration = Toast.LENGTH_SHORT;

        openUART();
        startMQTT();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enabled = !enabled;
                if (enabled) {
                    btnSend.setText("DISABLE");
                } else {
                    btnSend.setText("ENABLE");
                }
            }
        });

//        btnSend.setOnClickListener(v -> send(txtCmd.getText().toString()));
    }

    // UART function
    private void openUART(){
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Log.d(TAG, "UART is not available");

        } else {
            Log.d(TAG, "UART is available");

            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {

                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                manager.requestPermission(driver.getDevice(), usbPermissionIntent);

                manager.requestPermission(driver.getDevice(), PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));


                return;
            } else {

                port = driver.getPorts().get(0);
                try {
                    port.open(connection);
                    port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                    SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
                    Executors.newSingleThreadExecutor().submit(usbIoManager);
                    Log.d(TAG, "UART is opened");
                    text = "Connected!";
                    connected = true;
                    Toast.makeText(context, text, duration).show();

                } catch (Exception e) {
                    Log.d(TAG, "There is error");
                }
            }
        }

    }


    /* SEND DATA */
    private void send(String str) {
        if (!connected) {
            text = "Not Connected!";
            Toast.makeText(context, text, duration).show();
            return;
        }
        try {
            byte[] data = (str).getBytes();
            port.write(data, WRITE_WAIT_MILLIS);
        }
        catch (Exception e) {
            onRunError(e);
        }
    }

    /* READ DATA */
    @Override
    public void onNewData(byte[] data) {
        mainLooper.post(() -> {
            receive(data);
        });
    }

    @Override
    public void onRunError(Exception e) {
        mainLooper.post(() -> {
            disconnect();
        });
    }

    private void disconnect() {
        try {
            port.close();
        } catch (IOException ignored) {}
        port = null;
    }

    String buffer = "";
    String dataStr = "";
    Float temp = new Float(0);
    Float humidity = new Float(0);
    String id = "";
    Boolean begin = false;
    Boolean finish = false;

    private void receive(byte[] data) {
        if (!begin) {
            buffer = new String(data);
            int beginIdx = buffer.indexOf("<");
            if (beginIdx != -1) {
                if (beginIdx + 1 < data.length) {
                    dataStr = dataStr + buffer.substring(beginIdx + 2);
                }
                begin = true;
            }
        }
        if (!finish) {
            buffer = new String(data);
            int endIdx = buffer.indexOf(">");
            if (endIdx == -1) {
                dataStr = dataStr + buffer;
            }
            else {
                dataStr = dataStr + buffer.substring(0, endIdx);
                finish = true;
            }
        }
        if (begin && finish) {
            String[] split = dataStr.split(":");
            String display = "";
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String strDate = dateFormat.format(date);
            id = split[0].substring(1);
            temp = Float.parseFloat(split[1]);
            humidity = Float.parseFloat(split[2]);
            display = id + ": ID" + "\n"
                    + temp + " :TEMP" + "\n"
                    + humidity + " :HUMID" + "\n"
                    + strDate + "\n\n";
            txtData.setText(display);
            if (enabled) {
                sendDataToMQTT("shinyo_dc/feeds/iot-project.iot-project-humidity", humidity.toString());
                sendDataToMQTT("shinyo_dc/feeds/iot-project.iot-project-temp", temp.toString());
            }
            dataStr = "";
            begin = false;
            finish = false;
        }
    }
    //=======================
}
