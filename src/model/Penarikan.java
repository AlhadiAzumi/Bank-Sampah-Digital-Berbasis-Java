package model;

import util.Format;

public class Penarikan extends Transaksi {

    private final double jumlah;
    private final String bankEwallet;
    private final String noRekening;
    private final String namaNasabah;

    public Penarikan(String noReferensi, double jumlah, String bankEwallet, String noRekening, String namaNasabah) {
        super(noReferensi);
        this.jumlah = jumlah;
        this.bankEwallet = bankEwallet;
        this.noRekening = noRekening;
        this.namaNasabah = namaNasabah;
    }

    public String getNoReferensi() {
        return getId();
    }

    public String getBankEwallet() {
        return bankEwallet;
    }

    public String getNoRekening() {
        return noRekening;
    }

    public String getNamaNasabah() {
        return namaNasabah;
    }

    @Override
    public double getJumlah() {
        return jumlah;
    }

    @Override
    public String getDeskripsi() {
        return "Transfer " + Format.rupiah(jumlah) + " ke " + bankEwallet + " " + noRekening
                + " a.n. " + namaNasabah + " (Ref: " + getNoReferensi() + ")";
    }
}
