package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.Kertas;
import model.Logam;
import model.Nasabah;
import model.Organik;
import model.Penarikan;
import model.Plastik;
import model.Sampah;
import model.Setoran;
import model.Transaksi;
import util.Format;

public class BankSampah {

    private final List<Nasabah> daftarNasabah;
    private final List<Sampah> daftarSampah;
    private int counterNasabah;
    private int counterTransaksi;
    private int counterPenarikan;

    public BankSampah() {
        daftarNasabah = new ArrayList<>();
        daftarSampah = new ArrayList<>();
        counterNasabah = 1;
        counterTransaksi = 1;
        counterPenarikan = 1;
        seedJenisSampah();
    }

    private void seedJenisSampah() {
        daftarSampah.add(new Plastik(4000));
        daftarSampah.add(new Kertas(2500));
        daftarSampah.add(new Logam(12000));
        daftarSampah.add(new Organik(1000));
    }

    public Nasabah tambahNasabah(String nama, String alamat, String noHp, String bankEwallet, String noRekening) {
        String id = String.format("N%03d", counterNasabah++);
        Nasabah nasabah = new Nasabah(id, nama, alamat, noHp, bankEwallet, noRekening);
        daftarNasabah.add(nasabah);
        return nasabah;
    }

    public List<Nasabah> getDaftarNasabah() {
        return Collections.unmodifiableList(daftarNasabah);
    }

    public List<Sampah> getDaftarSampah() {
        return Collections.unmodifiableList(daftarSampah);
    }

    public Nasabah cariNasabah(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID nasabah tidak boleh kosong.");
        }
        for (Nasabah nasabah : daftarNasabah) {
            if (nasabah.getId().equalsIgnoreCase(id.trim())) {
                return nasabah;
            }
        }
        throw new IllegalArgumentException("Nasabah dengan ID " + id + " tidak ditemukan.");
    }

    public Sampah cariSampah(String nama) {
        if (nama == null || nama.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama jenis sampah tidak boleh kosong.");
        }
        for (Sampah sampah : daftarSampah) {
            if (sampah.getNama().equalsIgnoreCase(nama.trim())) {
                return sampah;
            }
        }
        throw new IllegalArgumentException("Jenis sampah " + nama + " tidak tersedia.");
    }

    public Setoran setorSampah(String idNasabah, String namaSampah, double beratKg) {
        Nasabah nasabah = cariNasabah(idNasabah);
        Sampah sampah = cariSampah(namaSampah);
        Setoran setoran = new Setoran(generateIdTransaksi(), sampah, beratKg);
        nasabah.tambahSaldo(setoran.getJumlah());
        nasabah.tambahTransaksi(setoran);
        return setoran;
    }

    public Penarikan tarikSaldo(String idNasabah, double jumlah) {
        Nasabah nasabah = cariNasabah(idNasabah);
        nasabah.tarikSaldo(jumlah);
        Penarikan penarikan = new Penarikan(generateNoReferensi(), jumlah,
                nasabah.getBankEwallet(), nasabah.getNoRekening(), nasabah.getNama());
        nasabah.tambahTransaksi(penarikan);
        return penarikan;
    }

    private String generateIdTransaksi() {
        return String.format("T%03d", counterTransaksi++);
    }

    private String generateNoReferensi() {
        return String.format("TRF%03d", counterPenarikan++);
    }

    public void cetakLaporan() {
        System.out.println();
        System.out.println("=== LAPORAN BANK SAMPAH ===");

        Map<String, Double> totalBeratPerKategori = new LinkedHashMap<>();
        double totalNilaiSetoran = 0;
        double totalNilaiPencairan = 0;
        int jumlahSetoran = 0;
        int jumlahPencairan = 0;

        for (Nasabah nasabah : daftarNasabah) {
            for (Transaksi transaksi : nasabah.getRiwayat()) {
                if (transaksi.getKategoriSampah() != null) {
                    totalBeratPerKategori.merge(
                            transaksi.getKategoriSampah(),
                            transaksi.getBeratKg(),
                            Double::sum);
                    totalNilaiSetoran += transaksi.getJumlah();
                    jumlahSetoran++;
                } else {
                    jumlahPencairan++;
                    totalNilaiPencairan += transaksi.getJumlah();
                }
            }
        }

        System.out.println("Jumlah nasabah           : " + daftarNasabah.size());
        System.out.println("Jumlah setoran           : " + jumlahSetoran);
        System.out.println("Jumlah pencairan (transfer): " + jumlahPencairan);
        System.out.println("Total nilai setoran      : " + Format.rupiah(totalNilaiSetoran));
        System.out.println("Total dana dicairkan     : " + Format.rupiah(totalNilaiPencairan));
        System.out.println("Total saldo beredar      : " + Format.rupiah(totalSaldoBeredar()));
        System.out.println();
        System.out.println("Total sampah terkelola per kategori:");

        if (totalBeratPerKategori.isEmpty()) {
            System.out.println("- Belum ada sampah yang disetor.");
        } else {
            for (Map.Entry<String, Double> entry : totalBeratPerKategori.entrySet()) {
                System.out.println("- " + entry.getKey() + " : " + Format.angka(entry.getValue()) + " kg");
            }
        }
        System.out.println("===========================");
    }

    private double totalSaldoBeredar() {
        double total = 0;
        for (Nasabah nasabah : daftarNasabah) {
            total += nasabah.getSaldo();
        }
        return total;
    }
}
