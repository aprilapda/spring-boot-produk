package com.apda.produk.entity;

public class JenisProduk {
    private int id;
    private String namaJenis;

    public JenisProduk() {
    }

    public JenisProduk(int id, String namaJenis) {
        this.id = id;
        this.namaJenis = namaJenis;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNamaJenis() {
        return namaJenis;
    }

    public void setNamaJenis(String namaJenis) {
        this.namaJenis = namaJenis;
    }
}