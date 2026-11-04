package com.apda.order.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JsonProperty("produkId")
	@JsonAlias({"produk_id"})
	@Column(name = "produk_id", nullable = false)
	private Long produkId;

	@JsonProperty("pelangganId")
	@JsonAlias({"pelanggan_id"})
	@Column(name = "pelanggan_id", nullable = false)
	private Long pelangganId;

	@JsonProperty("tglTrans")
	@JsonAlias({"tgl_trans"})
	@Column(name = "tgl_trans", nullable = false)
	private LocalDate tglTrans;

	@Column(nullable = false)
	private Integer jumlah;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal total;

	// Versi agregat untuk event CQRS: 1 saat create, naik satu tiap update/delete.
	// Disembunyikan dari JSON publik (bukan bagian dari kontrak respons lama).
	@JsonIgnore
	@Column(name = "aggregate_version", nullable = false)
	private long aggregateVersion;

	// Username pemilik order dari sub JWT (metadata internal; tidak dipublikasikan).
	@JsonIgnore
	@Column(name = "owner_subject")
	private String ownerSubject;


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getProdukId() {
		return produkId;
	}

	public void setProdukId(Long produkId) {
		this.produkId = produkId;
	}

	public Long getPelangganId() {
		return pelangganId;
	}

	public void setPelangganId(Long pelangganId) {
		this.pelangganId = pelangganId;
	}

	public LocalDate getTglTrans() {
		return tglTrans;
	}

	public void setTglTrans(LocalDate tglTrans) {
		this.tglTrans = tglTrans;
	}

	public Integer getJumlah() {
		return jumlah;
	}

	public void setJumlah(Integer jumlah) {
		this.jumlah = jumlah;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public long getAggregateVersion() {
		return aggregateVersion;
	}

	public void setAggregateVersion(long aggregateVersion) {
		this.aggregateVersion = aggregateVersion;
	}

	public String getOwnerSubject() {
		return ownerSubject;
	}

	public void setOwnerSubject(String ownerSubject) {
		this.ownerSubject = ownerSubject;
	}
}