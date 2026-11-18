package model;

public class Kertas extends Sampah {

    public Kertas(double hargaPerKg) {
        super("Kertas", hargaPerKg);
    }

    @Override
    public double hitungNilai(double beratKg) {
        validasiBerat(beratKg);
        return beratKg * getHargaPerKg();
    }

    @Override
    public String getKategori() {
        return "Anorganik - Kertas";
    }
}
