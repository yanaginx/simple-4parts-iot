## Simple 4 parts IOT system

The implementation of a IOT system consists of 4 main parts:

- Sensor nodes: [DHT11 Temperature Humidity Sensor](https://hshop.vn/products/cam-bien-do-am-nhiet-do-dht11)
- Connectivity: ESP-NOW protocol, UART protocol, MQTT protocol
- Data processing:
  - The sensor send data to gateway node (consist of an ESP board and an Android smartphone) through wireless ESP-NOW protocol.
  - Then gateway's ESP board using UART protocol to send data to the mentioned smartphone.
  - The smartphone then send the received data to a server with MQTT protocol.
  - The UI then can fetch the data from the server through provided API.
- User interface:
  - The UI mobile application: `DataFetcher` dir, built with Android Studio
  - The UI provided by [adafruit](https://io.adafruit.com/)

---

### Components:

**Hardware**

- Sensor node: ESP8266 with DHT11 sensor.
- Gateway node: ESP32 and a USB otg connect to the smartphone installed the `UART_MQTT` app.

The ESP boards can be altered since the coding was based on Arduino library.

**Application**

The `mobile_application` dir contains:

- `DataFetcher`: The UI application fetching the data from the server.
- `UART_MQTT`: The Application parts of the Gateway node.

Those 2 applications are built using Android Studio. Proceed to install them to your devices with Android Studio.

The `MCU_module` dir contains:

- `GatewayMCU` source code
- `SensorNode` source code

Both can be deployed to the boards using Arduino IDE, I was using PlatformIO for the development so there are some other directories there.

In-depth information (hopefully detailed enough) can be found at the `Report` dir.

### Demo:

The demonstration of the system can be found [here](https://www.youtube.com/watch?v=MITGLIiJNmw).
