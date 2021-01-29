package com.bachtiarpanjaitan.miot;

/**
 * Created by BP on 9/15/2017.
 */

public class UserHandler {
    private String nama;
    private String email;
    private String alamat;
    private String password;
    private String bluetooth;
    private String plat;
    private String telepon;
    private String id;
    public UserHandler() {

    }

    public String getNama() {
        return nama;
    }

    public String getEmail() {
        return email;
    }

    public String getAlamat() {
        return alamat;
    }

    public String getPassword() {
        return password;
    }

    public String getPlat() {
        return plat;
    }

    public String getBluetooth()
    {
        return bluetooth;
    }

    public String getTelepon(){
        return telepon;
    }

    public String getId(){return id;}

    //set public

    public void setNama(String nama) {
        this.nama = nama;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat;
    }

    public void setTelepon(String telepon) {
        this.telepon = telepon;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPlat(String plat) {
        this.plat = plat;
    }

    public void setBluetooth(String bluetooth){
        this.bluetooth = bluetooth;
    }

    public void setId(String id){this.id = id;}
}
