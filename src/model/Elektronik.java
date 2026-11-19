package model;

public class Elektronik extends Sampah {

    public Elektronik(double hargaPerKg) {
        super("Elektronik", hargaPerKg);
    }

    @Override
    public double hitungNilai(double beratKg) {
        validasiBerat(beratKg);
        double nilai = beratKg * getHargaPerKg();
        if (beratKg >= 5) {
            nilai *= 1.05;
        }
        return nilai;
    }

    @Override
    public String getKategori() {
        return "Anorganik - Elektronik";
    }

    @Override
    public String getAturanDeskripsi() {
        return "Bonus 5% (min 5kg)";
    }
}
