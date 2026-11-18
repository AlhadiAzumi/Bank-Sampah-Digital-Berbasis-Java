package model;

public class Logam extends Sampah {

    public Logam(double hargaPerKg) {
        super("Logam", hargaPerKg);
    }

    @Override
    public double hitungNilai(double beratKg) {
        validasiBerat(beratKg);
        return beratKg * getHargaPerKg() * 1.05;
    }

    @Override
    public String getKategori() {
        return "Anorganik - Logam";
    }
}
