# ABL Microservices

Proyek microservices untuk demo Auth Service, CQRS Order, dan Email Service.

## Menjalankan

Prasyarat: JDK 23 dan RabbitMQ aktif. Dari PowerShell pada root repository:

```powershell
.\start-all.ps1
```

Gateway tersedia pada `http://localhost:9001`, Eureka pada `http://localhost:8761`, dan RabbitMQ management (jika aktif) pada `http://localhost:15672`.

Consumer email tidak dijalankan secara default. Pengiriman email nyata memerlukan `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_TO`, serta `ABL_ENABLE_EMAIL_SENDING=true` sebelum menjalankan script. Jangan commit kredensial.

## Alur fitur

- **Auth**: register/login, BCrypt, dan JWT untuk identitas serta role.
- **CQRS Order**: POST/PUT/DELETE masuk ke ORDER; GET masuk ke ORDER-QUERY. Outbox dan RabbitMQ memperbarui read model secara asynchronous.
- **Email**: event `ORDER_CREATED` diproses oleh consumer terpisah; kegagalan SMTP dapat di-retry dan diarahkan ke DLQ.

Panduan detail dan langkah demo:

- [Panduan teknis UTS](ALUR_UTS_AUTH_CQRS_EMAIL.md)
- [Skenario Postman](SCENARIO_POSTMAN.md)
- [Penjelasan alur dalam Word](PENJELASAN_ALUR_UTS_ABL.docx)

Import koleksi [ABL-Postman-Collection.json](ABL-Postman-Collection.json) untuk menjalankan contoh request.

## Modul

| Modul | Folder |
|---|---|
| Auth | `auth/auth` |
| Consumer | `consumer/consumer` |
| Eureka | `eureka/eureka` |
| Gateway | `gateway/gateway` |
| Order write | `order/order` |
| Order Query | `order-query/query` |
| Pelanggan | `pelanggan/pelanggan` |
| Producer | `producer/producer` |
| Produk | `produk/produk` |
