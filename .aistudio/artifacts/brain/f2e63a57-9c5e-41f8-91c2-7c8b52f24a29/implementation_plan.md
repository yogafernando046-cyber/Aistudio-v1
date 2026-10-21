# Rencana Implementasi: Loading Screen 5 Detik & Hapus Permanen Supabase

Rencana ini difokuskan secara khusus sesuai permintaan: penataan alur awal aplikasi (Layar Login langsung tampil di awal mendahului loading screen, diikuti Loading Screen 5 detik dengan 3 foto toko dan animasi progress bar setelah masuk), serta implementasi Fitur Hapus Permanen transaksi dan pengeluaran yang terhubung langsung menghapus baris data di Supabase dan menyinkronkan laporan.

### User Review & Critical Decisions

> [!IMPORTANT]
> - **Alur Layar Awal**: Aplikasi akan langsung memunculkan **Layar Login** saat dibuka. Untuk kemudahan pengguna yang sudah pernah masuk, disediakan tombol **Login Cepat Biometrik (Face ID / Sidik Jari)** dan tombol **Masuk Mode Demo**.
> - **Transisi Loading Screen 5 Detik**: Setelah otentikasi (Login biasa, Login Cepat, atau Mode Demo), layar loading interaktif berdurasi **tepat 5 detik** akan aktif memutar 3 foto toko (`IMG_0283_4afacd59.jpeg`, `ce284eea...jpeg`, `2d764737...jpeg`) disertai progress bar 0%–100%, lalu masuk ke Dashboard Kasir.
> - **Hapus Permanen Supabase**: Tindakan hapus permanen pada transaksi atau pengeluaran akan langsung memanggil perintah delete ke Supabase (`sales`, `deleted_sales`, `expenses`), membersihkan cache lokal, dan menghitung ulang omzet serta laba secara otomatis.

---

### 1. Overview & Core Concept

- **Apa yang Dibangun**:
  1. **Alur Masuk & Loading Screen Terpadu**:
     - Saat dibuka, aplikasi menampilkan Layar Login bersih dengan opsi *Login Cepat Biometrik / PIN*, form email/password, dan tombol *Eksplor Mode Demo*.
     - Begitu pengguna masuk, layar transisi loading screen 5 detik (3 foto bergantian setiap ~1.6 detik, bilah kemajuan 100%, dan status inisialisasi) menyambut pengguna sebelum membuka kasir.
  2. **Fitur Hapus Permanen Terhubung Supabase**:
     - Tombol **Hapus Permanen** pada daftar transaksi laporan dan riwayat arsip transaksi yang terhapus (`deleted_sales`).
     - Mengirimkan query penghapusan langsung ke database Supabase secara real-time sehingga data benar-benar terhapus di cloud dan tidak muncul di akun atau perangkat lain.
     - Sinkronisasi otomatis laporan keuangan (total penjualan, pengeluaran, dan laba bersih).

---

### 2. User Experience & Visual Design

#### A. Alur Layar Buka Aplikasi (Login -> Loading 5s -> Kasir)
1. **Layar Login Pertama**:
   - Begitu APK dibuka, background gelap elegan dengan kartu login toko muncul seketika.
   - Jika pengguna sudah pernah masuk sebelumnya: kartu menampilkan sapaan hangat dan tombol besar berkilau **"Masuk Cepat (Biometrik / PIN)"**.
   - Pengguna juga dapat memilih form login akun Supabase atau tombol **"Masuk Mode Demo"**.
2. **Layar Loading 5 Detik**:
   - Setelah klik login / masuk cepat / demo, overlay loading screen 5 detik muncul dengan animasi lembut.
   - 3 foto toko berputar secara halus dengan indikator 3 titik foto di bagian bawah kartu foto.
   - Bilah kemajuan (progress bar) bergerak mulus dari 0% hingga 100% selama tepat 5000ms.
   - Teks status berganti bertahap:
     - 0.0s – 1.6s: *"Menghubungkan ke Toko Idris..."* (Foto 1)
     - 1.6s – 3.3s: *"Menyinkronkan data katalog & laporan..."* (Foto 2)
     - 3.3s – 5.0s: *"Sistem kasir siap digunakan!"* (Foto 3)
   - Pada detik ke-5, overlay memudar (*fade-out*) dan membuka dashboard kasir.

#### B. Alur Hapus Permanen Transaksi & Supabase
1. **Pada Tabel Laporan Penjualan**:
   - Setiap baris transaksi memiliki opsi hapus: "Batalkan (ke Arsip)" dan tombol merah **"Hapus Permanen"**.
2. **Pada Riwayat Hapus Transaksi (Arsip)**:
   - Disediakan tombol **"Hapus Permanen"** di samping tombol "Pulihkan" dan "Cetak Ulang".
3. **Konfirmasi & Eksekusi**:
   - Menampilkan dialog konfirmasi: *"Hapus transaksi ini secara permanen dari server Supabase dan memori lokal? Data tidak dapat dipulihkan kembali."*
   - Menjalankan penghapusan atomic ke tabel `sales` dan `deleted_sales` di Supabase.
   - Menampilkan feedback toast/alert: *"Transaksi berhasil dihapus permanen dari Supabase dan laporan diperbarui."*

---

### 3. Key Product Decisions & Trade-Offs

- **Layar Login di Awal vs Auto-Skip**:
  - *Keputusan*: Menampilkan layar login di awal sesuai instruksi Anda, namun memberikan tombol "Masuk Cepat Biometrik" satu sentuhan bagi pengguna setia agar tidak perlu mengetik ulang email/password setiap kali membuka aplikasi.
  - *Alasan*: Menjawab keluhan *"Kok gak ada login screen nya"* sekaligus menjaga kemudahan dan kecepatan kasir melayani pembeli.
- **Hapus Permanen Langsung ke Supabase**:
  - *Keputusan*: Mengirim delete request langsung via Supabase REST Client (`.delete().eq('store_id', STORE_ID).eq('id', serverId)`) dengan fallback RPC bila tersedia, serta membersihkan cache lokal `idris_sales` dan `idris_deleted_sales`.
  - *Alasan*: Memastikan data di server cloud benar-benar bersih sehingga tidak ada perbedaan data antar akun atau perangkat lain.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   Aplikasi Dibuka                      │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                   Layar Login Awal                     │
│  - Tombol Masuk Cepat (Biometrik / PIN)                │
│  - Form Login Akun Supabase                            │
│  - Mode Demo                                           │
└───────────────────────────┬────────────────────────────┘
                            │ (Otentikasi Berhasil)
                            ▼
┌────────────────────────────────────────────────────────┐
│             Loading Screen Tepat 5 Detik               │
│  - Rotasi 3 Foto Toko Asli (IMG_0283, ce284, 2d764)    │
│  - Progress Bar Animasi (0% -> 100% / 5000ms)          │
│  - Status Sinkronisasi Data                            │
└───────────────────────────┬────────────────────────────┘
                            │ (Setelah 5000ms)
                            ▼
┌────────────────────────────────────────────────────────┐
│                 Dashboard Toko Idris                   │
│  ┌───────────────────────┐  ┌───────────────────────┐  │
│  │     Kasir & POS       │  │   Laporan & Rekap     │  │
│  └───────────────────────┘  └───────────┬───────────┘  │
└─────────────────────────────────────────┼──────────────┘
                                          │
                                          ▼
                      ┌───────────────────────────────────────┐
                      │          Aksi Hapus Permanen          │
                      │  - sales.delete() [Supabase]          │
                      │  - deleted_sales.delete() [Supabase]  │
                      │  - expenses.delete() [Supabase]       │
                      │  - Recalculate Metrics & Offline Store│
                      └───────────────────────────────────────┘
```

- **Path Aset Foto Loading**:
  - Foto 1: `file:///android_asset/www/assets/IMG_0283_4afacd59.jpeg` atau `public/assets/aistudio/...`
  - Foto 2: `ce284eea-6763-4d4d-bcb1-50f478ec2662_4975b49f.jpeg`
  - Foto 3: `2d764737-504d-46c5-88f4-febb3314cbf9_75d65274.jpeg`
- **Fungsi JavaScript yang Disesuaikan**:
  - `initAppStartup()`: Menjamin login overlay tampil pertama kali, menyiapkan tombol Biometrik cepat.
  - `startPostLoginSplash(callback)`: Menjalankan loading screen 5 detik dengan 3 foto sebelum memanggil `showApp()`.
  - `permanentDeleteSale(id)`: Menghapus data permanen dari tabel `sales`, `deleted_sales`, dan state lokal.
  - `deleteExpense(id)`: Menjamin penghapusan permanen dari tabel `expenses` di Supabase.

---

### Verification Plan

1. **Pengujian Alur Masuk & Loading Screen**:
   - Membuka aplikasi: verifikasi layar login langsung muncul di awal.
   - Melakukan login / login cepat / mode demo: verifikasi loading screen 5 detik muncul dengan 3 foto toko bergantian dan progress bar mencapai 100% sebelum masuk ke kasir.
2. **Pengujian Hapus Permanen**:
   - Hapus transaksi melalui tombol "Hapus Permanen" di Laporan dan di Riwayat Hapus: verifikasi dialog konfirmasi muncul, data terhapus dari Supabase, dan data tidak kembali saat laporan dimuat ulang.
   - Hapus pengeluaran: verifikasi data pengeluaran terhapus dari Supabase dan total pengeluaran/laba diperbarui.
3. **Verifikasi Build**:
   - Menjalankan `compile_applet` untuk memastikan seluruh kode terkompilasi dengan sukses tanpa error.
