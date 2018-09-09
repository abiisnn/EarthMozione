#include <SoftwareSerial.h>
SoftwareSerial BTserial(3, 2); // (TX,RX)
int finalVoltage = 0;
void setup() {

  pinMode(31, INPUT);
  pinMode(33, INPUT);
  pinMode(35, INPUT);
  pinMode(37, INPUT);
  pinMode(39, INPUT);
  pinMode(41, INPUT);
  pinMode(43, INPUT);
  pinMode(45, INPUT);
  BTserial.begin(9600);
  Serial.begin(9600);
   
}
void serialFlush(){
  while(BTserial.available() > 0) {
    char t = BTserial.read();
  }
} 
void loop() {
  BTserial.flush();
  // read the input on analog pin 0:
  int sensorValue = analogRead(A0);
  // Convert the analog reading (which goes from 0 - 1023) to a voltage (0 - 5V):
  float voltage = sensorValue * (5.00f / 1023.00f);
  // print out the value you read:
  Serial.println(voltage);
  finalVoltage = 5-voltage;
  if(finalVoltage <= 0)
    finalVoltage = 0;
  BTserial.println(finalVoltage);
  delay(250);
}
