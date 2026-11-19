package model;

public class Tekstil extends Sampah {

    public Tekstil(double hargaPerKg) {
        super("Tekstil", hargaPerKg);
    }

    @Override
    public double hitungNilai(double beratKg) {
        validasiBerat(beratKg);
        return beratKg * getHargaPerKg();
    }

    @Override
    public String getKategori() {
        return "Anorganik - Tekstil";
    }
}
