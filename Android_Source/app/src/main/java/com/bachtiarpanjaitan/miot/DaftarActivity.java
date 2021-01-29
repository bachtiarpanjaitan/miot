package com.bachtiarpanjaitan.miot;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

import org.w3c.dom.Text;

import static java.security.AccessController.getContext;

public class DaftarActivity extends AppCompatActivity {

    private TextView textNama;
    private TextView textEmail;
    private TextView textAlamat;
    private TextView textTelepon;
    private TextView textPassword;
    private TextView textUpassword;
    private TextView textBluetooth;
    private TextView textPlat;
    private Button btnDaftar;
    private ProgressDialog mProgress;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daftar);

        mAuth = FirebaseAuth.getInstance();

        textNama = (TextView)findViewById(R.id.txt_daftar_nama);
        textEmail = (TextView) findViewById(R.id.txt_daftar_email);
        textAlamat = (TextView)findViewById(R.id.txt_daftar_alamat);
        textTelepon = (TextView)findViewById(R.id.txt_daftar_telepon);
        textPassword = (TextView)findViewById(R.id.txt_daftar_password);
        textUpassword = (TextView)findViewById(R.id.txt_daftar_upassword);
        textBluetooth = (TextView)findViewById(R.id.txt_daftar_bluetooth);
        textPlat = (TextView)findViewById(R.id.txt_daftar_plat);

        btnDaftar = (Button) findViewById(R.id.btn_daftar);

        mProgress = new ProgressDialog(this);

        btnDaftar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mulaiDaftar();
            }
        });
    }

    private void mulaiDaftar() {
        final String nama = textNama.getText().toString().trim();
        final String email = textEmail.getText().toString().trim();
        final String alamat = textAlamat.getText().toString().trim();
        final String telepon = textTelepon.getText().toString().trim();
        final String password = textPassword.getText().toString().trim();
        final String bluetooth = textBluetooth.getText().toString().trim();
        final String plat = textPlat.getText().toString().trim();
        final String upassword = textUpassword.getText().toString().trim();

        if(!TextUtils.isEmpty(nama) && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(alamat)
                && !TextUtils.isEmpty(telepon) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(upassword)){
            if(TextUtils.equals(password,upassword)) {
                mProgress.setMessage("Sedang Mendaftarkan....");
                mProgress.show();

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Firebase ref = new Firebase(Config.FIREBASE_URL_DATABASE_USER);
                                    String ambilid = mAuth.getCurrentUser().getUid();

                                    UserHandler user = new UserHandler();
                                    user.setId(ambilid);
                                    user.setNama(nama);
                                    user.setEmail(email);
                                    user.setAlamat(alamat);
                                    user.setBluetooth(bluetooth);
                                    user.setPlat(plat);
                                    user.setTelepon(telepon);

                                    ref.child(ambilid).setValue(user);

                                    mProgress.dismiss();

                                    Intent kembali = new Intent(DaftarActivity.this, LoginActivity.class);
                                    startActivity(kembali);
                                    finish();

                                }
                            }
                        });
            }
            else {
                Toast.makeText(this, "Password Tidak Sama", Toast.LENGTH_SHORT).show();

            }

        }else{
            Toast.makeText(this, "Data Yang Dibutuhkan Kosong", Toast.LENGTH_SHORT).show();
        }
    }
}
