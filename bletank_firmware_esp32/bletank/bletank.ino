#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <CircularBuffer.h>

#define CMD_ENGINE_START 256
#define CMD_ENGINE_STOP  257
#define CMD_BRAKING      258
#define CMD_FORWARD      259
#define CMD_BACKWARD     260
#define CMD_TURN_LEFT    261
#define CMD_TURN_RIGHT   262

// BLE variables
BLEServer *pServer = NULL;
BLECharacteristic * pTxCharacteristic;
bool deviceConnected = false;
bool oldDeviceConnected = false;
bool isEngineStarted = false;
uint8_t txValue = 0;
CircularBuffer<String, 512> buffer;

// UART service UUID
#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

// Server callback
class ServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      Serial.println("connected");
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      Serial.println("disconnected");
      deviceConnected = false;
      pServer->startAdvertising(); // restart advertising
    }
};

// Data recieved callback
class DataRecievedCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      buffer.push(pCharacteristic->getValue().c_str());
    }
};

// pin
const int vref1Pin = 25;
const int in1_1Pin = 26;
const int in1_2Pin = 27;
const int vref2Pin = 4;
const int in2_1Pin = 16;
const int in2_2Pin = 17;

// setting PWM properties
const int freq = 30000;
const int vrefChannel = 0;
const int resolution = 8;
int dutyCycle = 255;
 
void setup(){
  Serial.begin(115200);

  // Create the BLE Device
  BLEDevice::init("BLE TANK");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());

    // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create a BLE Characteristic
  pTxCharacteristic = pService->createCharacteristic(
                        CHARACTERISTIC_UUID_TX,
                       BLECharacteristic::PROPERTY_NOTIFY
                      );
                      
  pTxCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic * pRxCharacteristic = pService->createCharacteristic(
                        CHARACTERISTIC_UUID_RX,
                        BLECharacteristic::PROPERTY_WRITE
                      );

  pRxCharacteristic->setCallbacks(new DataRecievedCallbacks());

  // Start the service
  pService->start();

  // Start advertising
  pServer->getAdvertising()->start();
  Serial.println("Waiting a client connection to notify...");
  
  // configure PWM functionalitites
  ledcSetup(vrefChannel, freq, resolution);
  
  // attach the channel to the GPIO to be controlled
  ledcAttachPin(vref1Pin, vrefChannel);
  ledcAttachPin(vref2Pin, vrefChannel);
  pinMode(in1_1Pin, OUTPUT);
  pinMode(in1_2Pin, OUTPUT);
  pinMode(in2_1Pin, OUTPUT);
  pinMode(in2_2Pin, OUTPUT);
}

void parse_command() {
  String command = buffer.pop();
  Serial.println(command);
  switch (command.toInt()) {
    case CMD_ENGINE_START:
      isEngineStarted = true;
      Serial.println("CMD_ENGINE_START");
      break;
    case CMD_ENGINE_STOP:
      isEngineStarted = false;
      Serial.println("CMD_ENGINE_STOP");
      break;
    case CMD_BRAKING:
      Serial.println("CMD_BRAKING");
      if (isEngineStarted) {
        digitalWrite(in1_1Pin, HIGH);
        digitalWrite(in1_2Pin, HIGH);
        digitalWrite(in2_1Pin, HIGH);
        digitalWrite(in2_2Pin, HIGH);
      }
      break;
    case CMD_FORWARD:
      Serial.println("CMD_FORWARD");
      if (isEngineStarted) {
        digitalWrite(in1_1Pin, HIGH);
        digitalWrite(in1_2Pin, LOW);
        digitalWrite(in2_1Pin, HIGH);
        digitalWrite(in2_2Pin, LOW);
      }
      break;
    case CMD_BACKWARD:
      Serial.println("CMD_BACKWARD");
      if (isEngineStarted) {
        digitalWrite(in1_1Pin, LOW);
        digitalWrite(in1_2Pin, HIGH);
        digitalWrite(in2_1Pin, LOW);
        digitalWrite(in2_2Pin, HIGH);
      }
      break;
    case CMD_TURN_LEFT:
      Serial.println("CMD_TURN_LEFT");
      if (isEngineStarted) {
        digitalWrite(in1_1Pin, HIGH);
        digitalWrite(in1_2Pin, LOW);
        digitalWrite(in2_1Pin, LOW);
        digitalWrite(in2_2Pin, HIGH);
      }
      break;
    case CMD_TURN_RIGHT:
      Serial.println("CMD_TURN_RIGHT");
      if (isEngineStarted) {
        digitalWrite(in1_1Pin, LOW);
        digitalWrite(in1_2Pin, HIGH);
        digitalWrite(in2_1Pin, HIGH);
        digitalWrite(in2_2Pin, LOW);
      }
      break;
    default:
      Serial.println("CMD_SPEED");
      dutyCycle = command.toInt(); 
  }
  delay(100);
  // stop after delay
  digitalWrite(in1_1Pin, LOW);
  digitalWrite(in1_2Pin, LOW);
  digitalWrite(in2_1Pin, LOW);
  digitalWrite(in2_2Pin, LOW);
}
 
void loop(){
  if (!buffer.isEmpty()) {
    parse_command();
  }
  ledcWrite(vrefChannel, dutyCycle);
  delay(15);
}
