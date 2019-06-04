#include <SoftwareSerial.h>
#include <SPI.h>
#include <Wire.h>
#include "ssd1306.h"

int bluetoothTx = 7;
int bluetoothRx = 6;
#define WIDTH     128
#define HEIGHT     64

#define OLED_RST    9 
#define OLED_DC     8
#define OLED_CS    10
#define SPI_MOSI   11    /* connect to the DIN pin of OLED */
#define SPI_SCK    13     /* connect to the CLK pin of OLED */

SoftwareSerial mySerial(bluetoothTx, bluetoothRx); // RX, TX  
// Connect HM10      Arduino Uno
//     Pin 1/TXD          Pin 7
//     Pin 2/RXD          Pin 8

uint8_t oled_buf[WIDTH * HEIGHT / 8];

void setup() {  
  Serial.begin(9600);
  mySerial.begin(9600);
  SSD1306_begin();
  SSD1306_clear(oled_buf);

}


void loop() {  
  String op;
  
  if (mySerial.available()) {
    op = mySerial.read();  
    
    if (op != 0)
    {
      // about signs
      if(op.startsWith("0"){
        String t = op.substring(2);
        if(t[0].c_str() == '0'){
          // straight sign
          SSD1306_bitmap(30, 40, up_map, 16, 8, oled_buf); 
        } else if(t[0].c_str() == '1'){
          // right straight sign
          SSD1306_bitmap(30, 40, right_map, 16, 8, oled_buf); 
        } else if(t[0].c_str() == '2'){
          // left straight sign
          SSD1306_bitmap(35, 30, left_map, 16, 8, oled_buf); 
        } else if(t[0].c_str() == '3'){
          // turn left sign
          SSD1306_bitmap(25, 40, go_left_map, 16, 8, oled_buf); 
        } else if(t[0].c_str() == '4'){
          // turn right sign
          SSD1306_bitmap(25,30, go_right_map, 16, 8, oled_buf); 
        } else if(t[0].c_str() == '5'){
          // return sign
          SSD1306_bitmap(20, 40, go_back_map, 16, 8, oled_buf); 
        } else if(t[0].c_str() = ='6'){
          // go backward sign
          SSD1306_bitmap(0, 2, down_map, 16, 8, oled_buf); 
        }
      }

      // about other informations.
      else if(op.startsWith("1")){
        String dist = op.substring(2);
        dist.concat(" m");
        SSD1306_string(98, 52, dist, 12, 0, oled_buf);
      }
    }
    
  }
}
