package model;

public class Kaca extends Sampah {

    public Kaca(double hargaPerKg) {
        super("Kaca", hargaPerKg);
    }

    @Override
    public double hitungNilai(double beratKg) {
        validasiBerat(beratKg);
        return beratKg * getHargaPerKg();
    }

    @Override
    public String getKategori() {
        return "Anorganik - Kaca";
    }
}
