package com.bachtiarpanjaitan.miot;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.Image;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.mainapi.smsnotification.ApiException;
import net.mainapi.smsnotification.api.DefaultApi;
import net.mainapi.smsnotification.ApiClient;
import net.mainapi.smsnotification.auth.HttpBasicAuth;

import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;

public class UtamaActivity extends AppCompatActivity implements View.OnClickListener {


    private String Uid;
    private Firebase dataUser;
    private FirebaseAuth mAuth;
    private String terjemahan;
    public String perintah;
    public URL myURL;
    //private StorageReference mStorage;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //widgets
    Button btnPaired;
    ListView devicelist;
    //Bluetoothx
    public BluetoothAdapter myBluetooth = null;
    public Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "";
    public  int engineStatus = 0;

    String address, info, uid;

    //data pengguna
    public String log_nama, log_email, log_alamat, log_telepon, aBluetooth, log_plat;
    public static final int VOICE_RECOGNITION_CODE = 1234;

    private ProgressDialog progress;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    private boolean ConnectSuccess = true;
    public Button vr;
    public TextToSpeech TTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utama);

        final TextView nama = (TextView) findViewById(R.id.nama_lengkap);
        final TextView email = (TextView) findViewById(R.id.email);
        final TextView nopol = (TextView) findViewById(R.id.nopol);

        Firebase.setAndroidContext(this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        mAuth.getCurrentUser();
        if (mAuth.getCurrentUser() != null) {
            uid = mAuth.getCurrentUser().getUid();
            dataUser = new Firebase(Config.FIREBASE_URL_DATABASE_USER + uid);
            Log.i("DATAUSER :", dataUser.toString());
            Log.i("UID :" ,mAuth.getCurrentUser().getUid().toString());
            dataUser.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Map<String, String> map = dataSnapshot.getValue(Map.class);
                    log_nama = map.get("nama");
                    log_alamat = map.get("alamat");
                    log_email = map.get("email");
                    log_telepon = map.get("telepon");
                    aBluetooth = map.get("bluetooth");
                    log_plat = map.get("plat");
                    nama.setText("Nama Lengkap : " + log_nama);
                    email.setText("Email : " + log_email);
                    nopol.setText("Nomor Polisi : " + log_plat);

                    Log.i("MAP :", map.toString());
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });
        } else {
            Intent kembaliLogin = new Intent(UtamaActivity.this, LoginActivity.class);
            startActivity(kembaliLogin);
            finish();
        }

        final ImageView bukaKunci = (ImageView) findViewById(R.id.bBukaKunci);
        final ImageView kunci = (ImageView) findViewById(R.id.bKunci);
        final Button alarm = (Button) findViewById(R.id.bAlarm);
        final Button cariPerangkat = (Button) findViewById(R.id.cariPerangkat);
        final Button putus = (Button) findViewById(R.id.bDisconnect);
        vr = (Button) findViewById(R.id.bVr);
        vr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVoiceRecognitionActivity();
                String toSpeak = "hai, " + log_nama.toString()+", What can I do?.";
                TTS.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        });
        voiceinputbuttons();

        progress = new ProgressDialog(this);

        //text to speech
        TTS=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    TTS.setLanguage(Locale.US);
                }
            }
        });


        btnPaired = (Button) findViewById(R.id.cariPerangkat);
        devicelist = (ListView) findViewById(R.id.listView);

        //mengatur default object
        putus.setClickable(false);
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (myBluetooth == null) {
            Toast.makeText(getApplicationContext(), "Perangkat tidak tersedia", Toast.LENGTH_LONG).show();

            //finish apk
            finish();
        } else if (!myBluetooth.isEnabled()) {
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 1);
        }

        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pairedDevicesList();
            }
        });

        dataUser = new Firebase(Config.FIREBASE_URL_DATABASE_USER + Uid);
        dataUser.keepSynced(true);

        //TODO: alarm = 1, bukakunci = 10, kunci = 11
        alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fungsiAlarm();
            }
        });

        bukaKunci.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fungsiBuka();
            }
        });

        kunci.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fungsiKunci();
            }
        });
        cariPerangkat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pairedDevicesList();
            }
        });
        putus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (btSocket.isConnected()) {
                        btSocket.close();
                    } else {

                        Toast.makeText(UtamaActivity.this, "Belum Disambungkan", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Disconnect();

            }
        });
    }
    public void voiceinputbuttons() {
        vr = (Button) findViewById(R.id.bVr);
        devicelist = (ListView) findViewById(R.id.listView);
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            Button putus = (Button) findViewById(R.id.bDisconnect);
            putus.setEnabled(true);
            info = ((TextView) v).getText().toString();
            address = info.substring(info.length() - 17);
//            new ConnectBT().execute();
            Log.e("ALAMAT PERANGKAT : ", address);
            Log.e("ALAMAT FIREBASE : ", aBluetooth);
            if (TextUtils.equals(address, aBluetooth)) {
                new ConnectBT().execute();
                Toast.makeText(UtamaActivity.this, "Autentikasi BLuetooth Berhasil.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(UtamaActivity.this, "Bluetooth tidak dikenal", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.keluar) {
            mAuth.signOut();
            Intent kembali = new Intent(UtamaActivity.this, LoginActivity.class);
            startActivity(kembali);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void pairedDevicesList() {
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                list.add(bt.getName() + "\n" + bt.getAddress()); 
                EXTRA_ADDRESS = bt.getAddress();
                Log.e("ALAMAT", EXTRA_ADDRESS);
            }
        } else {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener); 

    }

    private void fungsiAlarm() {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(1);
            } catch (IOException e) {
                Toast.makeText(this, "Error !, alarm", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fungsiBuka() {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(10);
            } catch (IOException e) {
                Toast.makeText(this, "ERROR !, membuka Kunci", Toast.LENGTH_SHORT).show();
            }
        }
        engineStatus = 1;
    }

    private void fungsiKunci() {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(11);
            } catch (IOException e) {
                Toast.makeText(this, "ERROR !, mengunci", Toast.LENGTH_SHORT).show();
            }
        }
        engineStatus = 0;
    }

    private void Disconnect() {
        if (btSocket != null) {
            try {
                btSocket.close(); //close connection
                Toast.makeText(this, "Bluetooth Terputus", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(UtamaActivity.this, UtamaActivity.class);
                startActivity(intent);
            } catch (IOException e) {
                msg("Error");
            }
        }

    }

    private void msg(String s) {
        Toast.makeText(UtamaActivity.this, s, Toast.LENGTH_SHORT).show();
    }

    /// voice recognition
    public void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Hi, MIOT ");
        startActivityForResult(intent, VOICE_RECOGNITION_CODE);
    }
    public void onClick(View v) {
        // TODO Auto-generated method stub
        startVoiceRecognitionActivity();
    }
    public void informationMenu() {
        startActivity(new Intent("android.intent.action.INFOSCREEN"));
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_RECOGNITION_CODE && resultCode == RESULT_OK) {
            ArrayList matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            devicelist.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, matches));
           perintah = matches.get(0).toString();
           if(perintah.equals("start") || perintah.equals("buka")|| perintah.equals("hidup")){
               fungsiBuka();
               String toSpeak = "motorcycle has been started.";
               TTS.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

           }
           else if(perintah.equals("close") || perintah.equals("mati")|| perintah.equals("stop")){
               fungsiKunci();
               String toSpeak = "motorcycle has been stoped.";
               TTS.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
           }
           else if(perintah.equals("alarm")){
               fungsiAlarm();
           }
           else if(perintah.equals("engine start") || perintah.equals("nyalakan")){
               if(engineStatus == 1){
                   fungsiBuka();
               }
               else if (engineStatus == 0){
                   fungsiBuka();
                   fungsiBuka();
               }
               String toSpeak = "motorcycle has been started.";
               TTS.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
           }


        }
    }


    //clas koneksi bluetooth
    private class ConnectBT extends AsyncTask<Void, Void, Void>  
    {

        private boolean ConnectSuccess = true; 

        @Override
        protected void onPreExecute() {
            progress.setMessage("Menyambungkan, Silahkan Tunggu...");
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... devices)
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                    progress.dismiss();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) 
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
            } else {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }


    }
}

