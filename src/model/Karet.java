package model;

public class Karet extends Sampah {

    public Karet(double hargaPerKg) {
        super("Karet", hargaPerKg);
    }

    @Override
    public double hitungNilai(double beratKg) {
        validasiBerat(beratKg);
        return beratKg * getHargaPerKg() * 1.05;
    }

    @Override
    public String getKategori() {
        return "Anorganik - Karet";
    }

    @Override
    public String getAturanDeskripsi() {
        return "Bonus 5%";
    }
}
