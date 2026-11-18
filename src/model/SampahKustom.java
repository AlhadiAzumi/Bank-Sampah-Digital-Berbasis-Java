package model;

public class SampahKustom extends Sampah {

    private final String kategori;
    private final String aturan; // NORMAL, BONUS5, BONUS5_MIN10

    public SampahKustom(String nama, double hargaPerKg, String kategori, String aturan) {
        super(nama, hargaPerKg);
        this.kategori = (kategori == null || kategori.trim().isEmpty()) ? "Lainnya" : kategori.trim();
        this.aturan = normalizeAturan(aturan);
    }

    private String normalizeAturan(String aturan) {
        if (aturan == null) return "NORMAL";
        if ("BONUS5".equalsIgnoreCase(aturan.trim())) return "BONUS5";
        if ("BONUS5_MIN10".equalsIgnoreCase(aturan.trim())) return "BONUS5_MIN10";
        return "NORMAL";
    }

    public String getAturan() {
        return aturan;
    }

    @Override
    public double hitungNilai(double beratKg) {
        validasiBerat(beratKg);
        double nilai = beratKg * getHargaPerKg();
        if ("BONUS5".equals(aturan)) {
            nilai *= 1.05;
        } else if ("BONUS5_MIN10".equals(aturan) && beratKg >= 10) {
            nilai *= 1.05;
        }
        return nilai;
    }

    @Override
    public String getKategori() {
        return kategori;
    }
}