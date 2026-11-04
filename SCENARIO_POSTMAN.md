# Skenario Postman UTS ABL

Panduan ini mengikuti alur Auth, CQRS Order, dan Email yang terbaru. Panduan penjelasan dan batas tes ada di [ALUR_UTS_AUTH_CQRS_EMAIL.md](./ALUR_UTS_AUTH_CQRS_EMAIL.md).

## Persiapan

1. Pastikan JDK 23 dan RabbitMQ aktif; jalankan `.\start-all.ps1` dari PowerShell pada folder proyek.
2. Tunggu service terdaftar di Eureka `http://localhost:8761`, lalu import `ABL-Postman-Collection.json`.
3. Gateway Postman: `http://localhost:9001`. Service langsung: Auth `8086`, Produk `8083`, Pelanggan `8082`, Order write `8081`, Order Query `8087`; Producer `8085`.
4. RabbitMQ harus tersedia untuk alur event dan proyeksi CQRS. Consumer email tidak dijalankan secara default.

## A. Auth

Tanpa token, lakukan `POST http://localhost:9001/api/auth/register`:

```json
{
  "username": "siswa1",
  "password": "rahasia123",
  "email": "siswa1@example.com"
}
```

Harapkan **201**, role `PELANGGAN`, dan token. Register kedua kali dengan username sama memberi **409**. Email kosong atau kredensial kosong memberi **400**. Mencoba mendaftar sebagai `ADMIN_PELANGGAN` juga ditolak.

Untuk login, `POST /api/auth/login` dengan username/password yang sama. Token dikembalikan pada respons; Postman Collection menyimpannya ke `{{token}}`. Password salah menghasilkan **401**.

## B. Profil Pelanggan

Tambahkan header `Authorization: Bearer {{token}}`.

1. `POST /api/pelanggan` dengan `{"nama":"April","alamat":"Padang","jenis_kelamin":"P"}` untuk membuat profil.
2. GET/PUT/DELETE `/api/pelanggan/{id}` hanya dapat mengakses profil milik subject token tersebut. Nilai pemilik tidak dapat dikirim melalui JSON.
3. Tanpa token, endpoint terlindungi harus memberi **401**; mencoba membaca profil pengguna lain memberi **404**.

Order memerlukan id profil yang valid dan id produk yang masih ada. Buat produk lewat `POST /api/produk` jika belum ada; katalog Produk tidak memerlukan token.

## C. CQRS Order

1. Buat order lewat `POST http://localhost:9001/api/orders` dengan token:

   ```json
   {
     "produkId": 1,
     "pelangganId": 1,
     "tglTrans": "2026-10-23",
     "jumlah": 2,
     "total": 24000000
   }
   ```

   Ganti ID berdasarkan respons Produk dan Pelanggan yang dibuat sebelumnya. Order tanpa referensi valid ditolak, tidak disimpan.
2. ORDER menangani POST/PUT/DELETE. Order dan event outbox disimpan dalam satu transaksi. Publisher menunggu confirm RabbitMQ sebelum menandai event terkirim.
3. Baca dengan `GET /api/orders` atau `GET /api/orders/{id}`. Gateway meneruskan GET ke ORDER-QUERY yang memiliki database proyeksi terpisah.
4. Read model diperbarui asynchronous. Jika GET langsung setelah write mendapat `503 READ_MODEL_LAGGING`, tunggu sebentar lalu ulangi. Header `X-Order-Projection-Min-Version` dari write dapat dikirim pada GET untuk memastikan query menunggu versi minimum.
5. Untuk update/delete, gunakan `PUT/DELETE /api/orders/{id}`. Sesudah proyeksi memproses event, GET mencerminkan perubahan/penghapusan.

## D. Email

Consumer tidak aktif secara default. Pengiriman email nyata perlu `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_TO`, dan `ABL_ENABLE_EMAIL_SENDING=true` sebelum menjalankan `start-all.ps1`. Event `ORDER_CREATED` dikirim ke antrean email khusus; Consumer mengirim isi email ke `MAIL_TO`. Tanpa opt-in, RabbitMQ dapat menyimpan event tetapi email tidak dikirim.

Tes otomatis memakai mock mail sender. Untuk demo tanpa risiko mengirim email, demonstrasikan order/event melalui RabbitMQ dan jangan aktifkan Consumer dengan konfigurasi SMTP nyata.

## Catatan hasil

H2 Auth, Order, dan Order Query menggunakan penyimpanan file di `%LOCALAPPDATA%\ABL`; data tetap ada setelah service restart. Produk/Pelanggan memiliki konfigurasi DB sendiri. Jangan mengandalkan ID contoh `1`—gunakan ID dari respons API. Signing key dan akun admin bawaan hanya untuk demo lokal.
