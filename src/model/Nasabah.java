package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import util.Format;

public class Nasabah {

    private final String id;
    private String nama;
    private String alamat;
    private String noHp;
    private String bankEwallet;
    private String noRekening;
    private double saldo;
    private final List<Transaksi> riwayat;

    public Nasabah(String id, String nama, String alamat, String noHp, String bankEwallet, String noRekening) {
        if (nama == null || nama.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama nasabah tidak boleh kosong.");
        }
        if (bankEwallet == null || bankEwallet.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank/E-Wallet tidak boleh kosong.");
        }
        if (noRekening == null || noRekening.trim().isEmpty()) {
            throw new IllegalArgumentException("No. rekening/tujuan tidak boleh kosong.");
        }
        this.id = id;
        this.nama = nama.trim();
        this.alamat = alamat == null ? "" : alamat.trim();
        this.noHp = noHp == null ? "" : noHp.trim();
        this.bankEwallet = bankEwallet.trim();
        this.noRekening = noRekening.trim();
        this.saldo = 0;
        this.riwayat = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        if (nama == null || nama.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama nasabah tidak boleh kosong.");
        }
        this.nama = nama.trim();
    }

    public String getAlamat() {
        return alamat;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat == null ? "" : alamat.trim();
    }

    public String getNoHp() {
        return noHp;
    }

    public void setNoHp(String noHp) {
        this.noHp = noHp == null ? "" : noHp.trim();
    }

    public String getBankEwallet() {
        return bankEwallet;
    }

    public void setBankEwallet(String bankEwallet) {
        if (bankEwallet == null || bankEwallet.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank/E-Wallet tidak boleh kosong.");
        }
        this.bankEwallet = bankEwallet.trim();
    }

    public String getNoRekening() {
        return noRekening;
    }

    public void setNoRekening(String noRekening) {
        if (noRekening == null || noRekening.trim().isEmpty()) {
            throw new IllegalArgumentException("No. rekening/tujuan tidak boleh kosong.");
        }
        this.noRekening = noRekening.trim();
    }

    public String getTujuanTransfer() {
        return bankEwallet + " " + noRekening + " a.n. " + nama;
    }

    public double getSaldo() {
        return saldo;
    }

    public void tambahSaldo(double jumlah) {
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah saldo yang ditambahkan harus lebih dari 0.");
        }
        saldo += jumlah;
    }

    public void tarikSaldo(double jumlah) {
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah pencairan harus lebih dari 0.");
        }
        if (jumlah > saldo) {
            throw new IllegalArgumentException("Saldo tidak cukup. Saldo saat ini: " + Format.rupiah(saldo));
        }
        saldo -= jumlah;
    }

    public void tambahTransaksi(Transaksi transaksi) {
        if (transaksi != null) {
            riwayat.add(transaksi);
        }
    }

    public List<Transaksi> getRiwayat() {
        return Collections.unmodifiableList(riwayat);
    }
}
