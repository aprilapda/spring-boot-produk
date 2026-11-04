package com.apda.order.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Pelanggan {
    private Long id;
    private String nama;
    private String alamat;
    @JsonProperty("jenis_kelamin")
    private String jenisKelamin;

    public Pelanggan() {
    }

    public Pelanggan(Long id, String nama, String alamat, String jenisKelamin) {
        this.id = id;
        this.nama = nama;
        this.alamat = alamat;
        this.jenisKelamin = jenisKelamin;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getAlamat() {
        return alamat;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat;
    }

    public String getJenisKelamin() {
        return jenisKelamin;
    }

    public void setJenisKelamin(String jenisKelamin) {
        this.jenisKelamin = jenisKelamin;
    }
}
