# Fridge Systeme — Smart Refrigeration Monitoring

A complete smart refrigeration monitoring system that enables **real-time temperature and humidity monitoring** through a mobile application. The system consists of an ESP32-based sensor node that publishes readings over MQTT, and an Android mobile app that subscribes and displays the data live.

---

## System Architecture

```
┌─────────────┐      WiFi / MQTT      ┌──────────────────┐
│   ESP32     │ ─────────────────────►│  HiveMQ Broker   │
│  (DHT22)    │  publish              │  broker.hivemq   │
│             │  temperature          │  .com:1883       │
│             │  humidity             │                  │
│             │  alarm status         │                  │
└─────────────┘                       └────────┬─────────┘
                                              │ subscribe
                                              ▼
                                      ┌─────────────────┐
                                      │  Android App    │
                                      │  (Real-time UI) │
                                      │  Temperature    │
                                      │  Humidity       │
                                      │  Alarm status   │
                                      │  Connection     │
                                      └─────────────────┘
```

---

## Components

### 1. ESP32 Firmware (Sensor Node)
- **Microcontroller:** ESP32
- **Sensor:** DHT22 (temperature + humidity)
- **Outputs:** Built-in LED (GPIO 2), Buzzer (GPIO 4)
- **Connectivity:** WiFi + MQTT (PubSubClient library)
- **Publishes to:**
  - `mystore123/fridge1/temperature` — temperature in °C
  - `mystore123/fridge1/humidity` — humidity in %
  - `mystore123/fridge1/alarm` — alarm state (`OK`, `TEMP_HIGH`, `HUMIDITY_HIGH`, `TEMP_AND_HUMIDITY_HIGH`)
  - `mystore123/fridge1/status` — device online/offline status
  - `mystore123/fridge1/data` — combined JSON payload
- **Limits:** Temp > 4.0°C, Humidity > 80% triggers alarm

### 2. Android Mobile App (Real-Time Dashboard)
- **Platform:** Android (minSdk 28, targetSdk 35)
- **Language:** Java
- **MQTT Client:** Eclipse Paho Android Service
- **Broker:** `tcp://broker.hivemq.com:1883`
- **Subscribes to:** all 5 ESP32 topics above
- **UI:** Real-time dashboard with color-coded cards, connection indicator, alarm status, and last-updated timestamp

---

## Color Palette (App Theme)

| Color | HEX | Usage |
|-------|-----|-------|
| Background | `#0B1120` | Main app background |
| Card | `#151F32` | Cards and dashboard widgets |
| Secondary card | `#1E293B` | Inputs, panels |
| Border | `#273449` | Borders and separators |
| Primary blue | `#38BDF8` | Main buttons, charts, active states |
| Cyan | `#06B6D4` | Sensors, temperature data |
| Green | `#22C55E` | Online / normal / ON |
| Orange | `#F59E0B` | Warnings |
| Red | `#EF4444` | Alerts / critical status |
| Main text | `#F8FAFC` | Titles and text |
| Secondary text | `#94A3B8` | Descriptions and labels |

### Chart Colors
- Temperature: `#F97316` (Orange)
- Humidity: `#06B6D4` (Cyan)
- Energy: `#A78BFA` (Purple)
- Device status: `#22C55E` (Green)

---

## Mobile App Screens

| Screen | Description |
|--------|-------------|
| **Home** | Real-time fridge dashboard — live temperature, humidity, connection status, alarm state, and last-updated time |
| **Navigation Drawer** | App navigation (Home, Gallery, Slideshow) |

### Dashboard UI Elements
- **Connection chip** — green dot = online, red = offline
- **Temperature card** — large cyan reading (°C)
- **Humidity card** — large cyan reading (%)
- **Status card** — color-coded alarm state (green = OK, orange = warning, red = alert)
- **Last updated** — timestamp at bottom

---

## MQTT Topic Reference

| Topic | Direction | Payload | Description |
|-------|-----------|---------|-------------|
| `mystore123/fridge1/temperature` | ESP32 → App | `22.5` | Temperature in °C |
| `mystore123/fridge1/humidity` | ESP32 → App | `55.3` | Humidity in % |
| `mystore123/fridge1/alarm` | ESP32 → App | `OK` / `TEMP_HIGH` / `HUMIDITY_HIGH` / `TEMP_AND_HUMIDITY_HIGH` | Alarm state |
| `mystore123/fridge1/status` | ESP32 → App | `online` / `offline` | Device connection status |
| `mystore123/fridge1/data` | ESP32 → App | `{"temperature":22.5,"humidity":55.3}` | Combined JSON reading |

> ⚠️ `broker.hivemq.com` is a public broker. For production, change the topic prefix (`mystore123`) to something unique and consider enabling authentication/TLS.

---

## Project Structure

```
fridge-systeme/
├── README.md
├── Mobile-App/                          # Android application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/example/myapplication/
│   │   │   │   ├── MainActivity.java
│   │   │   │   └── ui/home/
│   │   │   │       ├── HomeFragment.java      # MQTT client + dashboard UI
│   │   │   │       └── HomeViewModel.java     # LiveData state holder
│   │   │   ├── res/
│   │   │   │   ├── layout/fragment_home.xml   # Dashboard layout
│   │   │   │   ├── values/colors.xml          # App color palette
│   │   │   │   ├── values/themes.xml          # App themes (light)
│   │   │   │   ├── values-night/themes.xml    # App themes (dark)
│   │   │   │   ├── values/strings.xml         # UI strings
│   │   │   │   ├── drawable/status_circle.xml # Connection indicator
│   │   │   │   └── ...
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts                   # Dependencies (Paho MQTT)
│   └── build.gradle.kts
├── simulation/
│   └── temp-controle/                     # Simulation files
└── .git/
```

---

## Setup & Build

### Prerequisites
- Android Studio (latest)
- ESP32 board with DHT22 sensor connected to GPIO 15
- WiFi network (SSID: `Wokwi-GUEST` by default — change for your network)
- MQTT broker access (default: `broker.hivemq.com:1883`)

### Android App
1. Open `Mobile-App` in Android Studio
2. Sync Gradle (installs Eclipse Paho MQTT dependency)
3. Connect an Android device or start an emulator
4. Click **Run** (Shift + F10)
5. The app will connect to the broker and display live data once the ESP32 is publishing

### ESP32 Firmware
1. Open the ESP32 code in Arduino IDE or PlatformIO
2. Install libraries: `WiFi`, `PubSubClient`, `DHT`
3. Update `WIFI_SSID`, `WIFI_PASS` if needed
4. Optionally change the topic prefix `mystore123` to something unique
5. Upload to ESP32

---

## How It Works

1. ESP32 reads temperature and humidity from the DHT22 every 5 seconds
2. It checks against limits (temp > 4.0°C, humidity > 80%)
3. Local alarm triggers (LED + buzzer with different tones)
4. Readings and alarm state are published to MQTT topics
5. The Android app receives messages in real time and updates the dashboard UI
6. Connection status is reflected via the online/offline chip at the top

---

## License

MIT License