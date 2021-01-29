package com.bachtiarpanjaitan.miot;

import android.app.Application;

import com.firebase.client.Firebase;
import com.google.firebase.database.FirebaseDatabase;
/**
 * Created by BP on 9/15/2017.
 */

public class Config extends Application {
    public static final String DEVICE_ADDRESS = "98:d3:31:fb:5d:5d";
    public static final String FIREBASE_URL_DATABASE = "https://motorku-e1411.firebaseio.com/";
    public static final String FIREBASE_URL_DATABASE_USER = "https://motorku-e1411.firebaseio.com/users/";

    @Override
    public void onCreate(){
        super.onCreate();
        Firebase.setAndroidContext(this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
