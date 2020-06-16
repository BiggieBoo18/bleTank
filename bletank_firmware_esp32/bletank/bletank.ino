#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <CircularBuffer.h>

#define CMD_ENGINE_START 0
#define CMD_ENGINE_STOP  1
#define CMD_BRAKING      2
#define CMD_FORWARD      3
#define CMD_BACKWARD     4
#define CMD_TURN_LEFT    5
#define CMD_TURN_RIGHT   6

// BLE variables
BLEServer *pServer = NULL;
BLECharacteristic * pTxCharacteristic;
bool deviceConnected = false;
bool oldDeviceConnected = false;
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
const int vrefPin = 25;  // 25 corresponds to GPIO25
const int in1Pin = 26;
const int in2Pin = 27;

// setting PWM properties
const int freq = 30000;
const int vrefChannel = 0;
const int resolution = 8;
 
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
  ledcAttachPin(vrefPin, vrefChannel);
  pinMode(in1Pin, OUTPUT);
  pinMode(in2Pin, OUTPUT);
}

void parse_command() {
  String command = buffer.pop();
  Serial.println(command);
  switch (command.toInt()) {
    case CMD_ENGINE_START:
      Serial.println("CMD_ENGINE_START");
      break;
    case CMD_ENGINE_STOP:
      Serial.println("CMD_ENGINE_STOP");
      break;
    case CMD_BRAKING:
      Serial.println("CMD_BRAKING");
      break;
    case CMD_FORWARD:
      Serial.println("CMD_FORWARD");
      break;
    case CMD_BACKWARD:
      Serial.println("CMD_BACKWARD");
      break;
    case CMD_TURN_LEFT:
      Serial.println("CMD_TURN_LEFT");
      break;
    case CMD_TURN_RIGHT:
      Serial.println("CMD_TURN_RIGHT");
      break;
  }
}
 
void loop(){
  if (!buffer.isEmpty()) {
    parse_command();
  }
  delay(10);
//  digitalWrite(in1Pin, LOW);
//  digitalWrite(in2Pin, HIGH);
//  // increase the LED brightness
//  for(int dutyCycle = 0; dutyCycle <= 255; dutyCycle++){   
//    // changing the LED brightness with PWM
//    ledcWrite(vrefChannel, dutyCycle);
//    delay(15);
//  }
//
//  // decrease the LED brightness
//  for(int dutyCycle = 255; dutyCycle >= 0; dutyCycle--){
//    // changing the LED brightness with PWM
//    ledcWrite(vrefChannel, dutyCycle);   
//    delay(15);
//  }
}
