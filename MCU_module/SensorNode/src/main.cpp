#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <espnow.h>
#include <DHT.h>

#define SLEEP_SECS 30 
#define DHTPIN 4 // what digital pin we're connected to
#define DHTTYPE DHT11 // DHT 11

DHT dht(DHTPIN, DHTTYPE);
// REPLACE WITH RECEIVER MAC Address
uint8_t broadcastAddress[] = {0x24, 0x0A, 0xC4, 0xEE, 0xAB, 0xD4};

// Structure example to send data
// Must match the receiver structure
typedef struct struct_message {
  int id;
  float temp;
  float humidity;
} struct_message;

// Create a struct_message called myData
struct_message myData;

unsigned long lastTime = 0;  
unsigned long timerDelay = 10000;  // send readings timer

void goToSleep() {
  int sleepSecs = SLEEP_SECS;
  Serial.printf("Up for %li ms, going to sleep for %i secs...\n", millis(), sleepSecs);
  ESP.deepSleep(sleepSecs * 1000000, RF_NO_CAL); // sleep for 30 seconds
}

// Callback when data is sent
void OnDataSent(uint8_t *mac_addr, uint8_t sendStatus) {
  Serial.print("Last Packet Send Status: ");
  if (sendStatus == 0){
    Serial.println("Delivery success");
  }
  else{
    Serial.println("Delivery fail");
  }
}
 
void setup() {
  // Init Serial Monitor
  Serial.begin(115200);
 
  // Set device as a Wi-Fi Station
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();

  // init Sensor
  dht.begin();

  // Init ESP-NOW
  if (esp_now_init() != 0) {
    Serial.println("Error initializing ESP-NOW");
    goToSleep();
  }

  // Once ESPNow is successfully Init, we will register for Send CB to
  // get the status of Trasnmitted packet
  esp_now_set_self_role(ESP_NOW_ROLE_CONTROLLER);
  esp_now_register_send_cb(OnDataSent);
  
  // Register peer
  esp_now_add_peer(broadcastAddress, ESP_NOW_ROLE_SLAVE, 1, NULL, 0);

}
 
void loop() {
  if ((millis() - lastTime) > timerDelay) {

    // Read value from sensor:
    float humidity = dht.readHumidity();
    float temp = dht.readTemperature();
    
    if (isnan(humidity) || isnan(temp)) {
      Serial.println("Failed to read from DHT sensor!");
      lastTime = millis();
      return;
    }

    int id = ESP.getChipId();

    // Set values to send
    myData.temp = temp; 
    myData.humidity = humidity; 
    myData.id = id;

    Serial.printf("Getting temp = %f\n", myData.temp);
    Serial.printf("Getting humidity = %f\n", myData.humidity);
    Serial.printf("Getting id = %lu\n", myData.id);

    // Send message via ESP-NOW
    esp_now_send(broadcastAddress, (uint8_t *) &myData, sizeof(myData));

    lastTime = millis();
  }
}