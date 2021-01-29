#include <EEPROM.h> 
#include <LiquidCrystal.h>    
#include <SPI.h>        
#include <MFRC522.h>  

LiquidCrystal lcd(7,6,5,4,3,2);
//LiquidCrystal lcd(2,3,4,5,6,7);
boolean match = false;         
boolean programMode = false;  
boolean replaceMaster = false;

uint8_t successRead; 
uint8_t ambilId1;
uint8_t ambilStart;     

byte storedCard[4];  
byte readCard[4];  
byte masterCard[4]; 

extern volatile unsigned long mulaiHitungWaktu = 0;
unsigned long waktuSekarang = 0;
unsigned long jarakWaktu;

String data = "0";
int bluetooth = A0;
int alarm = 8;
int relay = A1;
int statusRelay;
int sensorGetar = A2;
int statusGetar = 0;


constexpr uint8_t RST_PIN = 9;    
constexpr uint8_t SS_PIN = 10;     

MFRC522 mfrc522(SS_PIN, RST_PIN);

///////////////////////////////////////// Setup ///////////////////////////////////
void setup() {
  Serial.begin(38400); 
  SPI.begin();   
  lcd.begin(16, 2);       
  mfrc522.PCD_Init(); 
  pinMode(alarm, OUTPUT);
  pinMode(bluetooth, OUTPUT);
  pinMode(relay, OUTPUT);
  pinMode(sensorGetar, INPUT);

  pinMode(15, OUTPUT);
  pinMode(14, OUTPUT);
  
  mfrc522.PCD_SetAntennaGain(mfrc522.RxGain_max);

  if (EEPROM.read(1) != 143) {
    Serial.println(F("KUNCI MASTER TIDAK DIDEFENISIKAN"));
    Serial.println(F("MENCARI PISC UNTUK DIDEFENISIKAN SEBAGAI MASTER"));
    do {
      successRead = getID();       
      delay(200);
    }
    while (!successRead);                  
    for ( uint8_t j = 0; j < 4; j++ ) {       
      EEPROM.write( 2 + j, readCard[j] ); 
    }
    EEPROM.write(1, 143);                 
    Serial.println(F("MASTER SUDAH DIDEFENISIKAN"));
  }
  Serial.println(F("-------------------"));
  Serial.println(F("UID MASTER"));
  for ( uint8_t i = 0; i < 4; i++ ) {       
    masterCard[i] = EEPROM.read(2 + i); 
    Serial.print(masterCard[i], HEX);
  }
  Serial.println("");
  Serial.println(F("-------------------"));
  Serial.println(F("PROSES DATA SELESAI"));

  statusGetar = analogRead(A2);
}


///////////////////////////////////////// LOOPING ///////////////////////////////////
void loop () {    
    if(Serial.available() > 0)
    {
    data = Serial.read();
    Serial.print(data);
    
    if(data == "1"){
        data_1:           
        getAlarm();
    }
    else if(data == "11"){
      mati();
    }
    else if(data == "10"){
      data_10:
      pesan("KUNCI TERBUKA",0,0);
      hidup();
      startEngine();
    }
    else if(data == "10" && statusRelay == 1)
    {
      data_dan_relay:
      pesan("KUNCI TERBUKA",0,0);
      hidup();
      startEngine();
    }
    else 
    {
      pesan("TIDAK DI IJINKAN",0,0);
    }
    }

       if(relay == LOW && statusGetar == LOW){
        do {
          digitalWrite(8, HIGH);
          delay(500);
          digitalWrite(8, LOW);
          delay(500);
          if(data == "1"){
            goto data_1;
            break;
          }
          else if (data == "10"){
            goto data_10;
            break;
          }
          else if(data == "10" && statusRelay ==1){
            goto data_dan_relay;
          }
        }while(data != "11");
        Serial.println(sensorGetar);
     }
  }

/////////////////////////////////////////  AKSES DI IJINKAN   ///////////////////////////////////
void granted ( uint16_t setDelay) {
digitalWrite(14, HIGH);
delay(2000);
digitalWrite(14,LOW);
lcd.clear();
}

///////////////////////////////////////// AKSES DITOLAK  ///////////////////////////////////
void denied() {
pesan("TIDAK BISA DIBUKA",0,0);
}


///////////////////////////////////////// MENGAMBIL PICC's UID ///////////////////////////////////
uint8_t getID() {
  // Getting ready for Reading PICCs
  if ( ! mfrc522.PICC_IsNewCardPresent()) { 
    return 0;
  }
  if ( ! mfrc522.PICC_ReadCardSerial()) { 
    return 0;
  }
  Serial.println(F("Mencari PICC's UID:"));
  for ( uint8_t i = 0; i < 4; i++) {  //
    readCard[i] = mfrc522.uid.uidByte[i];
    Serial.print(readCard[i], HEX);
  }
  Serial.println("");
  mfrc522.PICC_HaltA();
  return 1;
}

void ShowReaderDetails() {
  byte v = mfrc522.PCD_ReadRegister(mfrc522.VersionReg);
  Serial.print(F("MFRC522 Software Version: 0x"));
  Serial.print(v, HEX);
  if (v == 0x91)
    Serial.print(F(" = v1.0"));
  else if (v == 0x92)
    Serial.print(F(" = v2.0"));
  else
    Serial.print(F(" (unknown),probably a chinese clone?"));
  Serial.println("");
  
  if ((v == 0x00) || (v == 0xFF)) {
    Serial.println(F("WARNING: Communication failure, is the MFRC522 properly connected?"));
    Serial.println(F("SYSTEM HALTED: Check connections."));
    while (true); 
  }
}
//////////////////////////////////////// MEMBACA ID DARI EEPROM //////////////////////////////
void readID( uint8_t number ) {
  uint8_t start = (number * 4 ) + 2;  
  for ( uint8_t i = 0; i < 4; i++ ) {   
    storedCard[i] = EEPROM.read(start + i);  
  }
}

///////////////////////////////////////// MENAMBAH ID KE EEPROM   ///////////////////////////////////
void writeID( byte a[] ) {
  if ( !findID( a ) ) {  
    uint8_t num = EEPROM.read(0);   
    uint8_t start = ( num * 4 ) + 6;  
    num++;             
    EEPROM.write( 0, num ); 
    for ( uint8_t j = 0; j < 4; j++ ) {  
      EEPROM.write( start + j, a[j] ); 
    }
    Serial.println(F("BERHASIL MENAMBAH ID KE EEPROM"));
  }
  else {
    Serial.println(F("GAGAL MENAMBAH ID KE EEPROM"));
  }
}

///////////////////////////////////////// MENGHAPUS ID DARI EEPROM   ///////////////////////////////////
void deleteID( byte a[] ) {
  if ( !findID( a ) ) {    
    Serial.println(F("GAGAL, ADA MASALAH DENGAN EEPROM"));
  }
  else {
    uint8_t num = EEPROM.read(0);   
    uint8_t slot;   
    uint8_t start;     
    uint8_t looping;   
    uint8_t j;
    uint8_t count = EEPROM.read(0); 
    slot = findIDSLOT( a );  
    start = (slot * 4) + 2;
    looping = ((num - slot) * 4);
    num--;     
    EEPROM.write( 0, num );  
    for ( j = 0; j < looping; j++ ) {      
      EEPROM.write( start + j, EEPROM.read(start + 4 + j)); 
    }
    for ( uint8_t k = 0; k < 4; k++ ) {        
      EEPROM.write( start + j + k, 0);
    }
    Serial.println(F("SUKSES MENGHAPUS ID DARI EEPROM"));
  }
}

///////////////////////////////////////// MEMERIKSA BYTE   ///////////////////////////////////
boolean checkTwo ( byte a[], byte b[] ) {
  if ( a[0] != 0 )      
    match = true;      
  for ( uint8_t k = 0; k < 4; k++ ) {   
    if ( a[k] != b[k] )   
      match = false;
  }
  if ( match ) {     
    return true;    
  }
  else  {
    return false;      
  }
}

///////////////////////////////////////// MENCARI SLOT   ///////////////////////////////////
uint8_t findIDSLOT( byte find[] ) {
  uint8_t count = EEPROM.read(0);      
  for ( uint8_t i = 1; i <= count; i++ ) {   
    readID(i);             
    if ( checkTwo( find, storedCard ) ) {  
      return i;        
      break;        
    }
  }
}

///////////////////////////////////////// MENCARI ID DARI EEPROM   ///////////////////////////////////
boolean findID( byte find[] ) {
  uint8_t count = EEPROM.read(0);     
  for ( uint8_t i = 1; i <= count; i++ ) {    
    readID(i);       
    if ( checkTwo( find, storedCard ) ) {   
      return true;
      break; 
    }
    else {  
    }
  }
  return false;
}

////////////////////// MENGECEK USER SEBAGAI MASTER  ///////////////////////////////////

boolean isMaster( byte test[] ) {
  if ( checkTwo( test, masterCard ) )
    return true;
  else
    return false;
}

/////////////////// ENGINE dan AUTENTIKASI /////////////////////////////

void startEngine()
{
  pesan2("1 TAP = MEMULAI","2 TAP = BERHENTI",0,0,0,1);
  ulangi_starter:
do{
    pesan2(" SELAMAT  JALAN ","    BK1234CM    ",0,0,0,1);
    successRead = getID();
      data = Serial.read();
      if(data == "11"){
        Serial.print(data);
        mati();
        goto endLoop;
        break;
      }
      else if (data == "1"){
        getAlarm();
        goto endLoop;
        break;
      }      
}while(!successRead and data != "10");
    lcd.clear();
    delay(50);
    digitalWrite(alarm, HIGH);
    delay(20);
    digitalWrite(alarm, LOW);

    mulaiHitungWaktu = millis();

  /////////////////////// ENGINE STOP ///////////////////////////////////////
do{
  int last = 0;
  int m = 0;
    ambilId1 = getID();
     if(ambilId1 == true){
        mati();
        goto endLoop;
        break;
     }
     else {
      waktuSekarang = millis() - mulaiHitungWaktu;
     pesan2("SISA WAKTU START",String(2000 - waktuSekarang),0,0,0,1);
     lcd.setCursor(8,1);
     lcd.print("MS");
     }
     Serial.println(mulaiHitungWaktu);
     Serial.println(waktuSekarang);
     delay(500);
}while(waktuSekarang < 2000 );
    lcd.clear();
    mulaiHitungWaktu = 0;
    
  
  if ( isMaster(readCard)) {
    programMode = true;  
//    pesan("SEDANG DIPROSES", 0,0);
//    delay(500);
//    pesan("*** MEMULAI ***",0,0);
//    delay(500);
    lcd.clear();
    granted(10); 
    lcd.setCursor(0,0);
    pesan("SELAMAT DATANG",0,0);
    goto ulangi_starter;
    endLoop:
    delay(10);
  }
  
}

void mati()
{
  lcd.clear();
  digitalWrite(15, LOW);
  lcd.setCursor(0,0);
  lcd.print("TERKUNCI"); 
}

void getAlarm(){
  lcd.clear();
  for(int i = 0;i <=3;i++)
  {
    digitalWrite(alarm, HIGH); 
    delay(20);
    digitalWrite(alarm, LOW); 
    delay(100);
    digitalWrite(alarm, HIGH); 
    delay(20);
    digitalWrite(alarm, LOW); 
  }
  
  lcd.setCursor(0,0);
  lcd.print("ANDROID ALARM");
}

void hidup()
{
  digitalWrite(15, HIGH);
  lcd.clear();
}

void pesan(String psn, int x, int y){
  lcd.clear();
  lcd.setCursor(x,y);
  lcd.print(psn);
  return true;
}
void pesan2(String psn1,String psn2, int x1, int y1, int x2, int y2){
  lcd.clear();
  lcd.setCursor(x1,y1);
  lcd.print(psn1);
  lcd.setCursor(x2, y2);
  lcd.print(psn2);
  return true;
}



