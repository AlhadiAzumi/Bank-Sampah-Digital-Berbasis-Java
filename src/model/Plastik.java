package model;

public class Plastik extends Sampah {

    public Plastik(double hargaPerKg) {
        super("Plastik", hargaPerKg);
    }

    @Override
    public double hitungNilai(double beratKg) {
        validasiBerat(beratKg);
        double nilai = beratKg * getHargaPerKg();
        if (beratKg >= 10) {
            nilai *= 1.05;
        }
        return nilai;
    }

    @Override
    public String getKategori() {
        return "Anorganik - Plastik";
    }
}
