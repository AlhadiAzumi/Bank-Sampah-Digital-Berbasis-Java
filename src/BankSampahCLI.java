import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import model.Nasabah;
import model.Penarikan;
import model.Sampah;
import model.Setoran;
import model.Transaksi;
import service.BankSampah;
import util.Format;

public class BankSampahCLI {

    private static final Scanner scanner = new Scanner(System.in);
    private static final BankSampah bank = new BankSampah();

    public static void main(String[] args) {
        boolean berjalan = true;

        System.out.println("Selamat datang di Bank Sampah Digital");

        while (berjalan) {
            tampilkanMenu();
            int pilihan = bacaInt("Pilih menu: ");
            System.out.println();

            switch (pilihan) {
                case 1:
                    tambahNasabah();
                    break;
                case 2:
                    lihatSemuaNasabah();
                    break;
                case 3:
                    setorSampah();
                    break;
                case 4:
                    tarikSaldo();
                    break;
                case 5:
                    lihatRiwayatNasabah();
                    break;
                case 6:
                    bank.cetakLaporan();
                    break;
                case 7:
                    berjalan = false;
                    System.out.println("Terima kasih telah menggunakan Bank Sampah Digital.");
                    break;
                default:
                    System.out.println("Pilihan tidak valid. Silakan pilih 1-7.");
            }
            System.out.println();
        }

        scanner.close();
    }

    private static void tampilkanMenu() {
        System.out.println("=== BANK SAMPAH DIGITAL ===");
        System.out.println("1. Tambah Nasabah");
        System.out.println("2. Lihat Semua Nasabah");
        System.out.println("3. Setor Sampah");
        System.out.println("4. Pencairan Saldo (Transfer)");
        System.out.println("5. Riwayat Transaksi Nasabah");
        System.out.println("6. Laporan");
        System.out.println("7. Keluar");
    }

    private static void tambahNasabah() {
        System.out.println("--- Tambah Nasabah ---");
        String nama = bacaString("Nama nasabah      : ");
        String alamat = bacaString("Alamat            : ");
        String noHp = bacaString("No HP             : ");
        String bankEwallet = bacaString("Bank/E-Wallet     : ");
        String noRekening = bacaString("No. Rekening/Tujuan: ");

        try {
            Nasabah nasabah = bank.tambahNasabah(nama, alamat, noHp, bankEwallet, noRekening);
            System.out.println("Nasabah berhasil ditambahkan dengan ID " + nasabah.getId() + ".");
            System.out.println("Tujuan transfer: " + nasabah.getTujuanTransfer());
        } catch (IllegalArgumentException e) {
            System.out.println("Gagal menambah nasabah: " + e.getMessage());
        }
    }

    private static void lihatSemuaNasabah() {
        System.out.println("--- Daftar Nasabah ---");
        List<Nasabah> daftar = bank.getDaftarNasabah();
        if (daftar.isEmpty()) {
            System.out.println("Belum ada nasabah terdaftar.");
            return;
        }
        for (Nasabah nasabah : daftar) {
            System.out.println(nasabah.getId() + " | " + nasabah.getNama()
                    + " | " + nasabah.getAlamat()
                    + " | " + nasabah.getNoHp()
                    + " | " + nasabah.getBankEwallet() + " " + nasabah.getNoRekening()
                    + " | Saldo: " + Format.rupiah(nasabah.getSaldo()));
        }
    }

    private static void setorSampah() {
        System.out.println("--- Setor Sampah ---");
        if (bank.getDaftarNasabah().isEmpty()) {
            System.out.println("Belum ada nasabah. Tambahkan nasabah terlebih dahulu.");
            return;
        }

        lihatSemuaNasabah();
        tampilkanDaftarSampah();
        String idNasabah = bacaString("ID nasabah     : ");
        String namaSampah = bacaString("Jenis sampah   : ");
        double berat = bacaDouble("Berat (kg)     : ");

        try {
            Setoran setoran = bank.setorSampah(idNasabah, namaSampah, berat);
            System.out.println("Setoran berhasil!");
            System.out.println("Nilai setoran : " + Format.rupiah(setoran.getJumlah()));
            System.out.println("Saldo sekarang: " + Format.rupiah(bank.cariNasabah(idNasabah).getSaldo()));
        } catch (IllegalArgumentException e) {
            System.out.println("Gagal menyetor sampah: " + e.getMessage());
        }
    }

    private static void tarikSaldo() {
        System.out.println("--- Pencairan Saldo (Transfer Digital) ---");
        if (bank.getDaftarNasabah().isEmpty()) {
            System.out.println("Belum ada nasabah. Tambahkan nasabah terlebih dahulu.");
            return;
        }

        lihatSemuaNasabah();
        String idNasabah = bacaString("ID nasabah     : ");
        try {
            Nasabah nasabah = bank.cariNasabah(idNasabah);
            System.out.println("Tujuan transfer: " + nasabah.getTujuanTransfer());
            System.out.println("Saldo saat ini : " + Format.rupiah(nasabah.getSaldo()));
            double jumlah = bacaDouble("Jumlah cair    : ");
            String konfirmasi = bacaString("Konfirmasi transfer? (y/n): ");
            if (!"y".equalsIgnoreCase(konfirmasi)) {
                System.out.println("Pencairan dibatalkan.");
                return;
            }
            Penarikan penarikan = bank.tarikSaldo(idNasabah, jumlah);
            double saldoSisa = bank.cariNasabah(idNasabah).getSaldo();
            Path struk = null;
            try {
                struk = bank.buatStrukTransfer(nasabah, penarikan, saldoSisa);
            } catch (Exception ex) {
                System.out.println("Catatan: gagal menyimpan struk (" + ex.getMessage() + ")");
            }
            System.out.println("Pencairan berhasil diproses!");
            System.out.println("No. Referensi : " + penarikan.getNoReferensi());
            System.out.println("Tujuan        : " + nasabah.getTujuanTransfer());
            System.out.println("Jumlah        : " + Format.rupiah(penarikan.getJumlah()));
            System.out.println("Saldo sekarang: " + Format.rupiah(saldoSisa));
            if (struk != null) {
                System.out.println("Struk tersimpan: " + struk.toAbsolutePath());
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Gagal mencairkan saldo: " + e.getMessage());
        }
    }

    private static void lihatRiwayatNasabah() {
        System.out.println("--- Riwayat Transaksi ---");
        if (bank.getDaftarNasabah().isEmpty()) {
            System.out.println("Belum ada nasabah terdaftar.");
            return;
        }

        lihatSemuaNasabah();
        String idNasabah = bacaString("ID nasabah     : ");

        try {
            Nasabah nasabah = bank.cariNasabah(idNasabah);
            List<Transaksi> riwayat = nasabah.getRiwayat();
            System.out.println();
            System.out.println("Riwayat transaksi " + nasabah.getNama() + " (" + nasabah.getId() + "):");
            if (riwayat.isEmpty()) {
                System.out.println("Belum ada transaksi.");
                return;
            }
            for (Transaksi transaksi : riwayat) {
                System.out.println(transaksi.getId() + " | " + transaksi.getWaktuFormatted()
                        + " | " + transaksi.getDeskripsi());
            }
            System.out.println("Saldo saat ini: " + Format.rupiah(nasabah.getSaldo()));
        } catch (IllegalArgumentException e) {
            System.out.println("Gagal membuka riwayat: " + e.getMessage());
        }
    }

    private static void tampilkanDaftarSampah() {
        System.out.println("Jenis sampah tersedia:");
        for (Sampah sampah : bank.getDaftarSampah()) {
            System.out.println("- " + sampah.getNama() + " (" + Format.rupiah(sampah.getHargaPerKg()) + "/kg, " + sampah.getKategori() + ")");
        }
    }

    private static String bacaString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static int bacaInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Input harus berupa angka bulat.");
            }
        }
    }

    private static double bacaDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().replace(',', '.');
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Input harus berupa angka.");
            }
        }
    }
}