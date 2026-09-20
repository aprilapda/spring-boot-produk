package com.apda.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

	@Column(name = "produk_id", nullable = false)
	private Long produkId;

	@Column(name = "pelanggan_id", nullable = false)
	private Long pelangganId;

	@Column(name = "tgl_Trans", nullable = false)
	private LocalDateTime tglTrans;

	@Column(nullable = false)
	private Integer jumlah;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal total;

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

	public LocalDateTime getTglTrans() {
		return tglTrans;
	}

	public void setTglTrans(LocalDateTime tglTrans) {
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
}