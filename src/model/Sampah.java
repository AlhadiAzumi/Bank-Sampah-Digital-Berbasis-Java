package model;

public abstract class Sampah {

    private String nama;
    private double hargaPerKg;

    public Sampah(String nama, double hargaPerKg) {
        if (nama == null || nama.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama sampah tidak boleh kosong.");
        }
        this.nama = nama.trim();
        setHargaPerKg(hargaPerKg);
    }

    public String getNama() {
        return nama;
    }

    public double getHargaPerKg() {
        return hargaPerKg;
    }

    public void setHargaPerKg(double hargaPerKg) {
        if (hargaPerKg < 0) {
            throw new IllegalArgumentException("Harga per kg tidak boleh negatif.");
        }
        this.hargaPerKg = hargaPerKg;
    }

    public abstract double hitungNilai(double beratKg);

    public abstract String getKategori();

    protected void validasiBerat(double beratKg) {
        if (beratKg <= 0) {
            throw new IllegalArgumentException("Berat sampah harus lebih dari 0 kg.");
        }
    }
}
