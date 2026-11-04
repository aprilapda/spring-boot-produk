# Panduan UTS ABL: Auth, CQRS Order, dan Email

Dokumen ini merangkum implementasi, demonstrasi, dan batas verifikasi tiga fitur UTS.

## Status fitur

| Fitur | Implementasi utama | Cara membuktikan |
|---|---|---|
| Auth Service | Register/login, BCrypt, JWT bertanda tangan berisi `sub`, `role`, dan email. Register publik selalu menjadi `PELANGGAN`; tidak dapat mendaftar sebagai admin. Database Auth H2 berbasis file. | Register lalu login melalui gateway; gunakan token untuk endpoint Pelanggan/Order. |
| CQRS Order | ORDER menangani command dan menyimpan order + event outbox secara transaksional. ORDER-QUERY memiliki read model sendiri. RabbitMQ mengirim event; consumer proyeksi menggunakan inbox/idempotensi, versi, dan tombstone delete. | Buat/update/hapus order melalui gateway, lalu GET dari query model dan amati kemungkinan jeda eventual consistency. |
| Email Service | Consumer bertipe mengolah `ORDER_CREATED` dari antrean email khusus dan menyusun email dari snapshot event. Retry terbatas, DLQ untuk pesan yang gagal/tidak valid. Listener email hanya diaktifkan dengan opt-in eksplisit. | Jalankan Consumer hanya setelah menyiapkan SMTP, penerima, dan `ABL_ENABLE_EMAIL_SENDING=true`; cek antrean/DLQ. Jangan kirim email sungguhan saat tes otomatis. |

Email menggunakan penerima konfigurasi `MAIL_TO`, bukan otomatis alamat email akun. Producer `/email/send` adalah jalur pesan teks lama yang terpisah dari event order.

## Menjalankan aplikasi

1. Pastikan JDK 23 dan RabbitMQ tersedia. `start-all.ps1` menyiapkan folder data di `%LOCALAPPDATA%\ABL`, lalu menjalankan Eureka, Auth, Produk, Pelanggan, Order, Order Query, Gateway, dan Producer.
2. Dari PowerShell di folder proyek jalankan:

   ```powershell
   .\start-all.ps1
   ```

3. Tunggu service siap. Gateway ada di `http://localhost:9001`; Eureka di `http://localhost:8761`; RabbitMQ management, jika diaktifkan, di `http://localhost:15672`.
4. Import `ABL-Postman-Collection.json`. Buat produk dan profil pelanggan lebih dulu sebelum membuat order.
5. Untuk email sungguhan, set `MAIL_USERNAME`, `MAIL_PASSWORD`, dan `MAIL_TO` di terminal yang sama, lalu set `$env:ABL_ENABLE_EMAIL_SENDING='true'` sebelum menjalankan script. Variabel ini adalah persetujuan eksplisit untuk mengirim email eksternal. Tanpa semua syarat, Consumer email tidak dijalankan.

Semua HTTP demo melewati gateway port 9001. `POST/PUT/DELETE /api/orders/**` diteruskan ke ORDER; `GET /api/orders/**` diteruskan ke ORDER-QUERY. RabbitMQ harus hidup untuk mengirim event dan memperbarui read model. Service tulis menyimpan data H2 ke folder data; read model juga tersimpan terpisah. Produk/Pelanggan tetap memakai konfigurasi database masing-masing.

## Demo Postman

### A. Auth dan otorisasi

1. `POST /api/auth/register` tanpa token:

   ```json
   {"username":"siswa1","password":"rahasia123","email":"siswa1@example.com"}
   ```

   Harapkan **201**, role `PELANGGAN`, dan token. Email wajib; role ditentukan server.
2. `POST /api/auth/login` dengan username/password tadi. Harapkan **200** dan token baru. Password salah menghasilkan **401**.
3. Panggil `POST /api/pelanggan` dengan token untuk membuat profil. `authSubject` diikat server ke subject JWT, bukan diambil dari JSON. Akun pelanggan hanya dapat melihat/mengubah/menghapus profil sendiri; akun admin demo dapat mengelola semua profil.
4. Endpoint terlindungi tanpa token menghasilkan **401**. Akun admin demo awal: `admin` / `admin123`; gunakan hanya untuk demo lokal.

### B. CQRS Order

1. Buat order melalui `POST /api/orders` dengan JWT, referensi produk dan profil pelanggan yang benar. ORDER memvalidasi referensi sebelum menyimpan.
2. Jika sukses, ORDER menyimpan command dan outbox event dalam transaksi yang sama. Dispatcher mengirim event ke exchange RabbitMQ dan baru menandai event terkirim setelah menerima confirm broker.
3. Baca dengan `GET /api/orders` atau `GET /api/orders/{id}`. Ini menuju ORDER-QUERY, bukan database tulis. Karena proyeksi asynchronous, tunggu event diproses atau ulangi GET. Respons write menyertakan `X-Order-Projection-Min-Version`; query menerima header itu dan merespons `503 READ_MODEL_LAGGING` bila belum mengejar versi.
4. Update/delete mengikuti jalur write yang sama. Delete diproyeksikan sebagai tombstone sehingga pesan lama tidak menghidupkan kembali record yang sudah dihapus.

### C. Email

Order baru menghasilkan `ORDER_CREATED` yang dikirim ke antrean proyeksi dan antrean email terpisah. Query consumer memperbarui read model; email consumer menyusun isi dari snapshot event dan mengirim ke `MAIL_TO`. RabbitMQ menyimpan pesan ketika consumer mati. Untuk demonstrasi tanpa SMTP sungguhan, periksa event dan antrean; pengujian otomatis mem-mock pengirim email.

## Penjelasan singkat untuk dosen

- **Auth**: password tidak disimpan sebagai teks biasa. Setelah login, server menerbitkan JWT bertanda tangan. Service lain memverifikasi token dan memakai subject/role untuk membatasi akses.
- **CQRS**: model tulis dan model baca dipisahkan. ORDER berfokus pada validasi/transaksi command; ORDER-QUERY dioptimalkan untuk pembacaan dan diperbarui lewat event, sehingga konsistensinya eventual, bukan langsung.
- **Email/event**: RabbitMQ memisahkan proses order dari notifikasi. Outbox menghindari order tersimpan tanpa event yang dapat dikirim ulang. Retry/DLQ menangani kegagalan, sementara opt-in mencegah tes mengirim email eksternal tanpa sengaja.

## Batas verifikasi

- Validasi setelah perubahan: **44 tes Maven lulus, 0 gagal/error/skip** di sembilan modul; `mvnw -DskipTests package` juga berhasil untuk semua sembilan service. Koleksi Postman tervalidasi sebagai JSON. Auth juga berhasil smoke test saat dijalankan pada port 8086 dan merespons **401** untuk login berkredensial salah.
- Docker tidak tersedia. RabbitMQ lokal merespons pada port **5672** (management **15672**), tetapi runtime semua service dan alur order→broker→proyeksi belum dijalankan end-to-end pada sesi ini.
- Tes SMTP memakai mock. Pengiriman Gmail/SMTP nyata dan konfigurasi kredensial eksternal tidak diuji. Jangan memasukkan kredensial ke source control.
- H2 dipakai untuk pengembangan/demo, bukan pengganti validasi skala produksi. Signing key dan kredensial admin default harus diganti sebelum deployment.
