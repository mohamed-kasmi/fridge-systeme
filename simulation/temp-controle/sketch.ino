#include <WiFi.h>
#include <PubSubClient.h>
#include "DHT.h"

// ---------- Pins ----------
#define DHTPIN     15
#define DHTTYPE    DHT22
#define LED_PIN    2
#define BUZZER_PIN 4

// ---------- Limits ----------
const float TEMP_LIMIT = 4.0;              // °C
const float HUM_LIMIT  = 80.0;             // %
const unsigned long READ_INTERVAL = 5000;  // 5 seconds

// ---------- WiFi (Wokwi simulated network) ----------
const char* WIFI_SSID = "Wokwi-GUEST";
const char* WIFI_PASS = "";

// ---------- MQTT ----------
const char* MQTT_SERVER = "broker.hivemq.com";
const int   MQTT_PORT   = 1883;

// CHANGE "mystore123" to something unique (the broker is public!)
const char* TOPIC_TEMP   = "mystore123/fridge1/temperature";
const char* TOPIC_HUM    = "mystore123/fridge1/humidity";
const char* TOPIC_ALARM  = "mystore123/fridge1/alarm";
const char* TOPIC_STATUS = "mystore123/fridge1/status";
const char* TOPIC_DATA = "mystore123/fridge1/data";

DHT dht(DHTPIN, DHTTYPE);
WiFiClient espClient;
PubSubClient mqtt(espClient);
unsigned long lastRead = 0;

void connectWiFi() {
  Serial.print("Connecting to WiFi");
  WiFi.begin(WIFI_SSID, WIFI_PASS, 6);  // channel 6 for Wokwi
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println(" connected!");
}

void connectMQTT() {
  while (!mqtt.connected()) {
    Serial.print("Connecting to MQTT...");
    String clientId = "esp32-fridge-" + String(random(0xffff), HEX);

    // Last Will: broker publishes "offline" if the ESP32 disconnects
    if (mqtt.connect(clientId.c_str(), NULL, NULL, TOPIC_STATUS, 1, true, "offline")) {
      Serial.println(" connected!");
      mqtt.publish(TOPIC_STATUS, "online", true);
    } else {
      Serial.print(" failed, rc=");
      Serial.print(mqtt.state());
      Serial.println(" retrying in 3s");
      delay(3000);
    }
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  dht.begin();

  connectWiFi();
  mqtt.setServer(MQTT_SERVER, MQTT_PORT);
  Serial.println("Fridge monitor started...");
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) connectWiFi();
  if (!mqtt.connected()) connectMQTT();
  mqtt.loop();

  unsigned long now = millis();
  if (now - lastRead >= READ_INTERVAL) {
    lastRead = now;

    float temp = dht.readTemperature();
    float hum  = dht.readHumidity();

    if (isnan(temp) || isnan(hum)) {
      Serial.println("Failed to read from DHT22!");
      return;
    }

    Serial.printf("Temperature: %.1f °C | Humidity: %.1f %%\n", temp, hum);

    bool tempAlarm = temp > TEMP_LIMIT;
    bool humAlarm  = hum  > HUM_LIMIT;

    // ----- Local alarm (LED + buzzer) -----
    if (tempAlarm || humAlarm) {
      digitalWrite(LED_PIN, HIGH);
      if (tempAlarm && humAlarm) tone(BUZZER_PIN, 1500);
      else if (tempAlarm)        tone(BUZZER_PIN, 1000);
      else                       tone(BUZZER_PIN, 2000);
    } else {
      digitalWrite(LED_PIN, LOW);
      noTone(BUZZER_PIN);
    }

    // ----- Alarm text for the app -----
    const char* alarmMsg;
    if (tempAlarm && humAlarm) alarmMsg = "TEMP_AND_HUMIDITY_HIGH";
    else if (tempAlarm)        alarmMsg = "TEMP_HIGH";
    else if (humAlarm)         alarmMsg = "HUMIDITY_HIGH";
    else                       alarmMsg = "OK";

    // ----- Publish to MQTT -----
    char buf[16];
    dtostrf(temp, 1, 1, buf);
    mqtt.publish(TOPIC_TEMP, buf);

    dtostrf(hum, 1, 1, buf);
    mqtt.publish(TOPIC_HUM, buf);

    mqtt.publish(TOPIC_ALARM, alarmMsg);

    // ----- NEW: temperature + humidity in one message -----
    char json[64];
    snprintf(json, sizeof(json),
             "{\"temperature\":%.1f,\"humidity\":%.1f}", temp, hum);
    mqtt.publish(TOPIC_DATA, json);

    Serial.printf("Published. Alarm status: %s\n", alarmMsg);
    Serial.println(json);
  }
}