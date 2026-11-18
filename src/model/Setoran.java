package model;

import util.Format;

public class Setoran extends Transaksi {

    private final Sampah sampah;
    private final double beratKg;
    private final double nilai;

    public Setoran(String id, Sampah sampah, double beratKg) {
        super(id);
        if (sampah == null) {
            throw new IllegalArgumentException("Jenis sampah tidak boleh kosong.");
        }
        this.sampah = sampah;
        this.beratKg = beratKg;
        this.nilai = sampah.hitungNilai(beratKg);
    }

    public Sampah getSampah() {
        return sampah;
    }

    @Override
    public double getJumlah() {
        return nilai;
    }

    @Override
    public double getBeratKg() {
        return beratKg;
    }

    @Override
    public String getKategoriSampah() {
        return sampah.getKategori();
    }

    @Override
    public String getDeskripsi() {
        return "Setor " + sampah.getNama() + " " + Format.angka(beratKg) + " kg"
                + " -> " + Format.rupiah(nilai);
    }
}
