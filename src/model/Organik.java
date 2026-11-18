package model;

public class Organik extends Sampah {

    public Organik(double hargaPerKg) {
        super("Organik", hargaPerKg);
    }

    @Override
    public double hitungNilai(double beratKg) {
        validasiBerat(beratKg);
        return beratKg * getHargaPerKg();
    }

    @Override
    public String getKategori() {
        return "Organik";
    }
}
