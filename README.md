# TrashChain

Proyek akhir mata kuliah **Pemrograman Berorientasi Objek (PBO)** menggunakan **Java**.
Aplikasi ini memiliki **antarmuka web modern (landing page + light dashboard ala fintech)** sebagai mode utama, serta **GUI (Swing)** dan **CLI** sebagai opsi. Web tetap memakai seluruh logika PBO dan persistensi CSV yang sama: mencatat setoran sampah warga, mengonversinya menjadi saldo rupiah, dan melakukan **pencairan saldo secara transfer digital** ke rekening/e-wallet nasabah.

Data transaksi, nasabah, counter, dan **jenis sampah** disimpan **otomatis (persisten)** ke file CSV di folder `data/` dan dimuat kembali saat aplikasi dijalankan.

---

## 1. Fitur Utama

| Tab/Menu | Fitur | Keterangan |
|----------|-------|------------|
| Dashboard | Ringkasan | Total nasabah, setoran, pencairan, sampah terkelola (kg), nilai setoran, dana dicairkan, saldo beredar |
| Nasabah | Kelola nasabah | ID otomatis (`N001`), wajib isi tujuan transfer (Bank/E-Wallet & No. Rekening angka), cari/filter nasabah |
| Setor Sampah | Setoran | Pilih nasabah & jenis sampah, input berat, nilai dihitung otomatis (polimorfisme + aturan bonus) |
| Pencairan Saldo | Transfer digital | Konfirmasi transfer ke rekening/e-wallet terdaftar, No. Referensi (`TRF001`), **struk digital `.html`** modern otomatis di folder `struk/` (bisa dicetak/Simpan PDF) |
| Riwayat | Riwayat transaksi | Tampil per nasabah / **Semua Nasabah**, filter jenis (setoran/pencairan), rentang tanggal, pencarian, **ekspor CSV** |
| Jenis Sampah | Kelola sampah | Tambah jenis baru & ubah harga/kg dari UI, tersimpan persisten (`data/sampah.csv`) |
| Laporan | Laporan | Rekap per kategori, total nilai setoran/dicairkan, saldo beredar, **ekspor laporan CSV** |

Jenis sampah awal: Plastik (Rp4.000/kg, bonus 5% min 10kg), Kertas (Rp2.500/kg), Logam (Rp12.000/kg, bonus 5%), Organik (Rp1.000/kg), Kaca (Rp800/kg), Elektronik (Rp6.000/kg, bonus 5% min 5kg), Tekstil (Rp1.500/kg), Karet (Rp2.000/kg, bonus 5%).

---

## 2. Penerapan 4 Pilar PBO

- **Encapsulation**: field `private`, saldo hanya berubah via `tambahSaldo()`/`tarikSaldo()` (validasi), `getRiwayat()` unmodifiable.
- **Inheritance**: `Plastik/Kertas/Logam/Organik/Kaca/Elektronik/Tekstil/Karet/SampahKustom` extends `Sampah`; `Setoran` & `Penarikan` extends `Transaksi`.
- **Polymorphism**: `hitungNilai()` di-override berbeda per jenis; dipanggil via referensi `Sampah` saat setor. `getDeskripsi()` di-override pada transaksi.
- **Abstract class/method**: `Sampah` (abstrak: `hitungNilai`, `getKategori`) dan `Transaksi` (abstrak: `getJumlah`, `getDeskripsi`).

---

## 3. Struktur Folder

```
project/
  README.md
  .gitignore
  web/                 (frontend website: index.html, styles.css, app.js)
  data/                (dibuat otomatis, persisten, di-ignore git)
    nasabah.csv, setoran.csv, penarikan.csv, counters.properties, sampah.csv
  struk/               (dibuat otomatis, bukti transfer struk digital .html, di-ignore git)
  bin/                 (hasil kompilasi)
  src/
    Main.java
    BankSampahGUI.java
    BankSampahCLI.java
    model/ (Sampah, Plastik, Kertas, Logam, Organik, Kaca, Elektronik, Tekstil, Karet, SampahKustom, Nasabah, Transaksi, Setoran, Penarikan)
    service/BankSampah.java
    web/WebServer.java   (API HTTP + static file server)
    util/Format.java
```

---

## 4. Cara Kompilasi & Menjalankan

Prasyarat: **JDK 8+**.

```powershell
javac --release 8 -d bin -sourcepath src src/Main.java src/BankSampahGUI.java src/BankSampahCLI.java src/web/WebServer.java
java -cp bin Main --web                # Website di http://localhost:8080
java -cp bin Main --web --port=8081    # Port custom
java -cp bin Main                      # GUI (default)
java -cp bin Main --cli                # CLI
```

`--release 8` dipakai agar kompatibel bila runtime Java 8 (menghindari `UnsupportedClassVersionError`).

Web server memakai `com.sun.net.httpserver` (bawaan JDK, tanpa library eksternal). Jalankan perintah dari folder `project/` agar folder `web/`, `data/`, dan `struk/` terbaca. UI ada di `web/index.html`, `web/styles.css`, dan `web/app.js`; API di `src/web/WebServer.java`. Semua fitur GUI tersedia di web, termasuk melihat struk digital HTML dan ekspor CSV.

---

## 5. Skenario Demo Singkat

1. **Tambah Nasabah**: Budi, alamat, HP, BCA, 1234567890 → `N001`, tujuan transfer tercatat.
2. **Setor Sampah**: `N001`, Plastik, 12 kg → nilai Rp50.400 (bonus 5%), saldo Rp50.400.
3. **Pencairan/Transfer**: `N001`, Rp20.000, konfirmasi → sukses, Ref `TRF001`, saldo Rp30.400, **struk digital tersimpan** di `struk/Struk_TRF001.html` (dapat dibuka/dicetak lewat browser).
4. **Pencairan > saldo**: ditolak (saldo tidak cukup).
5. **Riwayat**: filter/cari, tampil setoran & transfer (tujuan + Ref), ekspor CSV.
6. **Dashboard/Laporan**: ringkasan & rekap kategori, ekspor laporan CSV.
7. **Jenis Sampah**: tambah jenis baru / ubah harga (tersimpan dan langsung dipakai).

---

## 6. Aturan Validasi & Catatan

- Nama, Bank/E-Wallet, No. Rekening wajib; No. HP (jika diisi) & No. Rekening harus **angka**.
- Berat & jumlah pencairan > 0; pencairan tidak boleh melebihi saldo (saldo tidak pernah negatif).
- Pencairan = **transfer digital (simulasi)** ke rekening/e-wallet terdaftar dengan **No. Referensi unik** + struk digital HTML (bisa dicetak/disimpan sebagai PDF dari browser).
- **Persistensi otomatis**: data & jenis sampah di `data/`, struk digital HTML di `struk/` (keduanya di-ignore git). Tanpa library eksternal (JDK standar).