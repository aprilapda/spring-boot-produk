package com.apda.produk.entity;


public class Produk {
    private int id;
    private String nama;
    private double harga;
    private String deskripsi;

    public Produk() {
    }
    
    public Produk(int id, String nama, double harga, String deskripsi) {
        this.id = id;
        this.nama = nama;
        this.harga = harga;
        this.deskripsi = deskripsi;
    }

    public int getId() {
        return id;
    }

    public String getNama() {
        return nama;
    }

    public double getHarga() {
        return harga;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

}
