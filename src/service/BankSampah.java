package service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import model.Elektronik;
import model.Kaca;
import model.Karet;
import model.Kertas;
import model.Logam;
import model.Nasabah;
import model.Organik;
import model.Penarikan;
import model.Plastik;
import model.Tekstil;
import model.Sampah;
import model.SampahKustom;
import model.Setoran;
import model.Transaksi;
import util.Format;
import util.StrukHtml;

public class BankSampah {

    private static final Path DATA_DIR = Paths.get("data");
    private static final Path NASABAH_FILE = DATA_DIR.resolve("nasabah.csv");
    private static final Path SETORAN_FILE = DATA_DIR.resolve("setoran.csv");
    private static final Path PENARIKAN_FILE = DATA_DIR.resolve("penarikan.csv");
    private static final Path COUNTER_FILE = DATA_DIR.resolve("counters.properties");
    private static final Path SAMPAH_FILE = DATA_DIR.resolve("sampah.csv");
    private static final Path STRUK_DIR = Paths.get("struk");

    private final List<Nasabah> daftarNasabah;
    private final List<Sampah> daftarSampah;
    private int counterNasabah;
    private int counterTransaksi;
    private int counterPenarikan;

    public BankSampah() {
        daftarNasabah = new ArrayList<Nasabah>();
        daftarSampah = new ArrayList<Sampah>();
        counterNasabah = 1;
        counterTransaksi = 1;
        counterPenarikan = 1;
        muatAtauSeedSampah();
        muatData();
    }

    private void seedJenisSampah() {
        daftarSampah.clear();
        daftarSampah.add(new Plastik(4000));
        daftarSampah.add(new Kertas(2500));
        daftarSampah.add(new Logam(12000));
        daftarSampah.add(new Organik(1000));
        daftarSampah.add(new Kaca(800));
        daftarSampah.add(new Elektronik(6000));
        daftarSampah.add(new Tekstil(1500));
        daftarSampah.add(new Karet(2000));
    }

    private void muatAtauSeedSampah() {
        if (Files.exists(SAMPAH_FILE)) {
            try {
                muatSampah();
                if (daftarSampah.isEmpty()) {
                    seedJenisSampah();
                    simpanSampah();
                }
                return;
            } catch (Exception e) {
                System.err.println("Gagal memuat data sampah: " + e.getMessage());
            }
        }
        seedJenisSampah();
        simpanSampahQuiet();
    }

    public Nasabah tambahNasabah(String nama, String alamat, String noHp, String bankEwallet, String noRekening) {
        String id = String.format("N%03d", counterNasabah++);
        Nasabah nasabah = new Nasabah(id, nama, alamat, noHp, bankEwallet, noRekening);
        daftarNasabah.add(nasabah);
        simpanData();
        return nasabah;
    }

    public Sampah tambahJenisSampah(String nama, double hargaPerKg, String kategori, String aturan) {
        if (nama == null || nama.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama jenis sampah tidak boleh kosong.");
        }
        if (hargaPerKg <= 0) {
            throw new IllegalArgumentException("Harga per kg harus lebih dari 0.");
        }
        String namaTrim = nama.trim();
        try {
            cariSampah(namaTrim);
            throw new IllegalArgumentException("Jenis sampah '" + namaTrim + "' sudah ada.");
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("sudah ada")) throw e;
        }
        String kat = (kategori == null || kategori.trim().isEmpty()) ? "Lainnya" : kategori.trim();
        String at = aturan == null ? "NORMAL" : aturan.trim().toUpperCase();
        if (!"BONUS5".equals(at) && !"BONUS5_MIN10".equals(at)) at = "NORMAL";
        SampahKustom sampah = new SampahKustom(namaTrim, hargaPerKg, kat, at);
        daftarSampah.add(sampah);
        simpanSampahQuiet();
        return sampah;
    }

    public Sampah updateHargaSampah(String nama, double hargaPerKg) {
        if (hargaPerKg <= 0) {
            throw new IllegalArgumentException("Harga per kg harus lebih dari 0.");
        }
        Sampah sampah = cariSampah(nama);
        sampah.setHargaPerKg(hargaPerKg);
        simpanSampahQuiet();
        return sampah;
    }

    public List<Nasabah> getDaftarNasabah() {
        return Collections.unmodifiableList(daftarNasabah);
    }

    public List<Sampah> getDaftarSampah() {
        return Collections.unmodifiableList(daftarSampah);
    }

    public Nasabah cariNasabah(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID nasabah tidak boleh kosong.");
        }
        for (Nasabah nasabah : daftarNasabah) {
            if (nasabah.getId().equalsIgnoreCase(id.trim())) {
                return nasabah;
            }
        }
        throw new IllegalArgumentException("Nasabah dengan ID " + id + " tidak ditemukan.");
    }

    public Sampah cariSampah(String nama) {
        if (nama == null || nama.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama jenis sampah tidak boleh kosong.");
        }
        for (Sampah sampah : daftarSampah) {
            if (sampah.getNama().equalsIgnoreCase(nama.trim())) {
                return sampah;
            }
        }
        throw new IllegalArgumentException("Jenis sampah " + nama + " tidak tersedia.");
    }

    public Setoran setorSampah(String idNasabah, String namaSampah, double beratKg) {
        Nasabah nasabah = cariNasabah(idNasabah);
        Sampah sampah = cariSampah(namaSampah);
        Setoran setoran = new Setoran(generateIdTransaksi(), sampah, beratKg);
        nasabah.tambahSaldo(setoran.getJumlah());
        nasabah.tambahTransaksi(setoran);
        simpanData();
        return setoran;
    }

    public Penarikan tarikSaldo(String idNasabah, double jumlah) {
        Nasabah nasabah = cariNasabah(idNasabah);
        nasabah.tarikSaldo(jumlah);
        Penarikan penarikan = new Penarikan(generateNoReferensi(), jumlah,
                nasabah.getBankEwallet(), nasabah.getNoRekening(), nasabah.getNama());
        nasabah.tambahTransaksi(penarikan);
        simpanData();
        return penarikan;
    }

    public Path buatStrukTransfer(Nasabah nasabah, Penarikan penarikan, double saldoSisa) {
        try {
            if (!Files.exists(STRUK_DIR)) {
                Files.createDirectories(STRUK_DIR);
            }
            Path file = STRUK_DIR.resolve("Struk_" + penarikan.getNoReferensi() + ".html");
            String html = StrukHtml.render(penarikan.getNoReferensi(), penarikan.getWaktuFormatted(),
                    nasabah.getNama() + " (" + nasabah.getId() + ")",
                    nasabah.getTujuanTransfer(), Format.rupiah(penarikan.getJumlah()),
                    Format.rupiah(saldoSisa), "BERHASIL · TRANSFER DIGITAL");
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write(html);
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Gagal menyimpan struk transfer: " + e.getMessage(), e);
        }
    }

    public void exportRiwayatCsv(Path file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("ID/Ref,Waktu,ID Nasabah,Nama Nasabah,Tipe,Deskripsi,Jumlah,Berat(kg),Kategori,Saldo Dampak");
            w.newLine();
            for (Nasabah n : daftarNasabah) {
                for (Transaksi t : n.getRiwayat()) {
                    boolean setoran = t.getKategoriSampah() != null;
                    String tipe = setoran ? "Setoran" : "Pencairan/Transfer";
                    String jumlah = String.format(Locale.US, "%.2f", t.getJumlah());
                    String berat = setoran ? String.format(Locale.US, "%.4f", t.getBeratKg()) : "";
                    String kategori = setoran ? safeCsv(t.getKategoriSampah()) : "";
                    w.write(String.join(",",
                            csv(t.getId()),
                            csv(t.getWaktuFormatted()),
                            csv(n.getId()),
                            csv(n.getNama()),
                            csv(tipe),
                            csv(t.getDeskripsi()),
                            csv(jumlah),
                            csv(berat),
                            csv(kategori),
                            ""));
                    w.newLine();
                }
            }
        }
    }

    public void exportLaporanCsv(Path file) throws IOException {
        Map<String, Double> totalBeratPerKategori = new LinkedHashMap<String, Double>();
        double totalNilaiSetoran = 0;
        double totalNilaiPencairan = 0;
        int jumlahSetoran = 0;
        int jumlahPencairan = 0;
        double totalBerat = 0;
        for (Nasabah nasabah : daftarNasabah) {
            for (Transaksi transaksi : nasabah.getRiwayat()) {
                if (transaksi.getKategoriSampah() != null) {
                    totalBeratPerKategori.merge(transaksi.getKategoriSampah(), transaksi.getBeratKg(), Double::sum);
                    totalNilaiSetoran += transaksi.getJumlah();
                    totalBerat += transaksi.getBeratKg();
                    jumlahSetoran++;
                } else {
                    jumlahPencairan++;
                    totalNilaiPencairan += transaksi.getJumlah();
                }
            }
        }
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("Metrik,Nilai"); w.newLine();
            w.write("Jumlah nasabah," + daftarNasabah.size()); w.newLine();
            w.write("Jumlah setoran," + jumlahSetoran); w.newLine();
            w.write("Jumlah pencairan (transfer)," + jumlahPencairan); w.newLine();
            w.write("Total berat sampah (kg)," + String.format(Locale.US, "%.4f", totalBerat)); w.newLine();
            w.write("Total nilai setoran," + String.format(Locale.US, "%.2f", totalNilaiSetoran)); w.newLine();
            w.write("Total dana dicairkan," + String.format(Locale.US, "%.2f", totalNilaiPencairan)); w.newLine();
            w.write("Total saldo beredar," + String.format(Locale.US, "%.2f", totalSaldoBeredar())); w.newLine();
            w.newLine();
            w.write("Kategori,Total Berat (kg)"); w.newLine();
            for (Map.Entry<String, Double> e : totalBeratPerKategori.entrySet()) {
                w.write(csv(e.getKey()) + "," + String.format(Locale.US, "%.4f", e.getValue())); w.newLine();
            }
        }
    }

    private String csv(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    private String safeCsv(String s) { return s == null ? "" : s; }

    private String generateIdTransaksi() {
        return String.format("T%03d", counterTransaksi++);
    }

    private String generateNoReferensi() {
        return String.format("TRF%03d", counterPenarikan++);
    }

    public void cetakLaporan() {
        System.out.println();
        System.out.println("=== LAPORAN BANK SAMPAH ===");

        Map<String, Double> totalBeratPerKategori = new LinkedHashMap<String, Double>();
        double totalNilaiSetoran = 0;
        double totalNilaiPencairan = 0;
        int jumlahSetoran = 0;
        int jumlahPencairan = 0;

        for (Nasabah nasabah : daftarNasabah) {
            for (Transaksi transaksi : nasabah.getRiwayat()) {
                if (transaksi.getKategoriSampah() != null) {
                    totalBeratPerKategori.merge(
                            transaksi.getKategoriSampah(),
                            transaksi.getBeratKg(),
                            Double::sum);
                    totalNilaiSetoran += transaksi.getJumlah();
                    jumlahSetoran++;
                } else {
                    jumlahPencairan++;
                    totalNilaiPencairan += transaksi.getJumlah();
                }
            }
        }

        System.out.println("Jumlah nasabah           : " + daftarNasabah.size());
        System.out.println("Jumlah setoran           : " + jumlahSetoran);
        System.out.println("Jumlah pencairan (transfer): " + jumlahPencairan);
        System.out.println("Total nilai setoran      : " + Format.rupiah(totalNilaiSetoran));
        System.out.println("Total dana dicairkan     : " + Format.rupiah(totalNilaiPencairan));
        System.out.println("Total saldo beredar      : " + Format.rupiah(totalSaldoBeredar()));
        System.out.println();
        System.out.println("Total sampah terkelola per kategori:");

        if (totalBeratPerKategori.isEmpty()) {
            System.out.println("- Belum ada sampah yang disetor.");
        } else {
            for (Map.Entry<String, Double> entry : totalBeratPerKategori.entrySet()) {
                System.out.println("- " + entry.getKey() + " : " + Format.angka(entry.getValue()) + " kg");
            }
        }
        System.out.println("===========================");
    }

    public double getTotalSaldoBeredar() {
        return totalSaldoBeredar();
    }

    private double totalSaldoBeredar() {
        double total = 0;
        for (Nasabah nasabah : daftarNasabah) {
            total += nasabah.getSaldo();
        }
        return total;
    }

    public synchronized void simpanData() {
        try {
            if (!Files.exists(DATA_DIR)) {
                Files.createDirectories(DATA_DIR);
            }
            simpanNasabah();
            simpanSetoran();
            simpanPenarikan();
            simpanCounters();
        } catch (IOException e) {
            System.err.println("Gagal menyimpan data: " + e.getMessage());
        }
    }

    private synchronized void muatData() {
        if (!Files.exists(DATA_DIR)) {
            return;
        }
        try {
            muatNasabah();
            muatSetoran();
            muatPenarikan();
            muatCounters();
        } catch (Exception e) {
            System.err.println("Gagal memuat data: " + e.getMessage());
        }
    }

    private void simpanNasabah() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(NASABAH_FILE, StandardCharsets.UTF_8)) {
            for (Nasabah n : daftarNasabah) {
                writer.write(String.join("|",
                        esc(n.getId()),
                        esc(n.getNama()),
                        esc(n.getAlamat()),
                        esc(n.getNoHp()),
                        esc(n.getBankEwallet()),
                        esc(n.getNoRekening()),
                        String.format(Locale.US, "%.2f", n.getSaldo())));
                writer.newLine();
            }
        }
    }

    private void simpanSetoran() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(SETORAN_FILE, StandardCharsets.UTF_8)) {
            for (Nasabah n : daftarNasabah) {
                for (Transaksi t : n.getRiwayat()) {
                    if (t instanceof Setoran) {
                        Setoran s = (Setoran) t;
                        writer.write(String.join("|",
                                esc(n.getId()),
                                esc(s.getId()),
                                esc(s.getWaktu().toString()),
                                esc(s.getSampah().getNama()),
                                String.format(Locale.US, "%.4f", s.getBeratKg()),
                                String.format(Locale.US, "%.2f", s.getJumlah())));
                        writer.newLine();
                    }
                }
            }
        }
    }

    private void simpanPenarikan() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(PENARIKAN_FILE, StandardCharsets.UTF_8)) {
            for (Nasabah n : daftarNasabah) {
                for (Transaksi t : n.getRiwayat()) {
                    if (t instanceof Penarikan) {
                        Penarikan p = (Penarikan) t;
                        writer.write(String.join("|",
                                esc(n.getId()),
                                esc(p.getNoReferensi()),
                                esc(p.getWaktu().toString()),
                                String.format(Locale.US, "%.2f", p.getJumlah()),
                                esc(p.getBankEwallet()),
                                esc(p.getNoRekening()),
                                esc(p.getNamaNasabah())));
                        writer.newLine();
                    }
                }
            }
        }
    }

    private void simpanCounters() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(COUNTER_FILE, StandardCharsets.UTF_8)) {
            writer.write("nasabah=" + counterNasabah);
            writer.newLine();
            writer.write("transaksi=" + counterTransaksi);
            writer.newLine();
            writer.write("penarikan=" + counterPenarikan);
            writer.newLine();
        }
    }

    private void simpanSampahQuiet() {
        try {
            if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);
            simpanSampah();
        } catch (IOException e) {
            System.err.println("Gagal menyimpan data sampah: " + e.getMessage());
        }
    }

    private void simpanSampah() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(SAMPAH_FILE, StandardCharsets.UTF_8)) {
            for (Sampah s : daftarSampah) {
                String tipe = "KUSTOM";
                String aturan = "NORMAL";
                String kategori = s.getKategori();
                if (s instanceof Plastik) { tipe = "PLASTIK"; aturan = "BONUS5_MIN10"; kategori = s.getKategori(); }
                else if (s instanceof Kertas) { tipe = "KERTAS"; aturan = "NORMAL"; kategori = s.getKategori(); }
                else if (s instanceof Logam) { tipe = "LOGAM"; aturan = "BONUS5"; kategori = s.getKategori(); }
                else if (s instanceof Organik) { tipe = "ORGANIK"; aturan = "NORMAL"; kategori = s.getKategori(); }
                else if (s instanceof Kaca) { tipe = "KACA"; aturan = "NORMAL"; kategori = s.getKategori(); }
                else if (s instanceof Elektronik) { tipe = "ELEKTRONIK"; aturan = "BONUS5_MIN5"; kategori = s.getKategori(); }
                else if (s instanceof Tekstil) { tipe = "TEKSTIL"; aturan = "NORMAL"; kategori = s.getKategori(); }
                else if (s instanceof Karet) { tipe = "KARET"; aturan = "BONUS5"; kategori = s.getKategori(); }
                else if (s instanceof SampahKustom) { tipe = "KUSTOM"; aturan = ((SampahKustom) s).getAturan(); kategori = s.getKategori(); }
                writer.write(String.join("|",
                        esc(s.getNama()),
                        String.format(Locale.US, "%.2f", s.getHargaPerKg()),
                        tipe,
                        esc(kategori),
                        esc(aturan)));
                writer.newLine();
            }
        }
    }

    private void muatSampah() throws IOException {
        daftarSampah.clear();
        if (!Files.exists(SAMPAH_FILE)) return;
        try (BufferedReader reader = Files.newBufferedReader(SAMPAH_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = split(line, 5);
                if (p.length < 3) continue;
                try {
                    String nama = unesc(p[0]);
                    double harga = Double.parseDouble(unesc(p[1]));
                    String tipe = unesc(p[2]).toUpperCase();
                    String kategori = p.length > 3 ? unesc(p[3]) : "";
                    String aturan = p.length > 4 ? unesc(p[4]) : "NORMAL";
                    Sampah s = null;
                    if ("PLASTIK".equals(tipe)) s = new Plastik(harga);
                    else if ("KERTAS".equals(tipe)) s = new Kertas(harga);
                    else if ("LOGAM".equals(tipe)) s = new Logam(harga);
                    else if ("ORGANIK".equals(tipe)) s = new Organik(harga);
                    else if ("KACA".equals(tipe)) s = new Kaca(harga);
                    else if ("ELEKTRONIK".equals(tipe)) s = new Elektronik(harga);
                    else if ("TEKSTIL".equals(tipe)) s = new Tekstil(harga);
                    else if ("KARET".equals(tipe)) s = new Karet(harga);
                    else s = new SampahKustom(nama, harga, kategori, aturan);
                    if (s != null) daftarSampah.add(s);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void muatNasabah() throws IOException {
        if (!Files.exists(NASABAH_FILE)) return;
        try (BufferedReader reader = Files.newBufferedReader(NASABAH_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = split(line, 7);
                if (p.length < 6) continue;
                try {
                    Nasabah n = new Nasabah(unesc(p[0]), unesc(p[1]), unesc(p[2]), unesc(p[3]), unesc(p[4]), unesc(p[5]));
                    daftarNasabah.add(n);
                    int idNum = parseIdNum(n.getId(), 'N');
                    if (idNum >= counterNasabah) counterNasabah = idNum + 1;
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void muatSetoran() throws IOException {
        if (!Files.exists(SETORAN_FILE)) return;
        try (BufferedReader reader = Files.newBufferedReader(SETORAN_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = split(line, 6);
                if (p.length < 6) continue;
                try {
                    Nasabah n = cariNasabah(unesc(p[0]));
                    String idTrx = unesc(p[1]);
                    LocalDateTime waktu = LocalDateTime.parse(unesc(p[2]));
                    Sampah sampah = cariSampah(unesc(p[3]));
                    double berat = Double.parseDouble(unesc(p[4]));
                    double nilai = Double.parseDouble(unesc(p[5]));
                    Setoran s = new Setoran(idTrx, waktu, sampah, berat, nilai);
                    n.tambahSaldo(nilai);
                    n.tambahTransaksi(s);
                    int idNum = parseIdNum(idTrx, 'T');
                    if (idNum >= counterTransaksi) counterTransaksi = idNum + 1;
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void muatPenarikan() throws IOException {
        if (!Files.exists(PENARIKAN_FILE)) return;
        try (BufferedReader reader = Files.newBufferedReader(PENARIKAN_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = split(line, 7);
                if (p.length < 7) continue;
                try {
                    Nasabah n = cariNasabah(unesc(p[0]));
                    String ref = unesc(p[1]);
                    LocalDateTime waktu = LocalDateTime.parse(unesc(p[2]));
                    double jumlah = Double.parseDouble(unesc(p[3]));
                    String bank = unesc(p[4]);
                    String norek = unesc(p[5]);
                    String nama = unesc(p[6]);
                    Penarikan pen = new Penarikan(ref, waktu, jumlah, bank, norek, nama);
                    n.tarikSaldo(jumlah);
                    n.tambahTransaksi(pen);
                    int idNum = parseRefNum(ref);
                    if (idNum >= counterPenarikan) counterPenarikan = idNum + 1;
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void muatCounters() throws IOException {
        if (!Files.exists(COUNTER_FILE)) return;
        int cn = counterNasabah, ct = counterTransaksi, cp = counterPenarikan;
        try (BufferedReader reader = Files.newBufferedReader(COUNTER_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] kv = line.split("=", 2);
                if (kv.length != 2) continue;
                String k = kv[0].trim();
                int v = Integer.parseInt(kv[1].trim());
                if ("nasabah".equals(k)) cn = v;
                else if ("transaksi".equals(k)) ct = v;
                else if ("penarikan".equals(k)) cp = v;
            }
        } catch (Exception ignored) {
        }
        if (cn > counterNasabah) counterNasabah = cn;
        if (ct > counterTransaksi) counterTransaksi = ct;
        if (cp > counterPenarikan) counterPenarikan = cp;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace("\r", "");
    }

    private String unesc(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == '\\') { out.append('\\'); i++; }
                else if (n == 'p') { out.append('|'); i++; }
                else if (n == 'n') { out.append('\n'); i++; }
                else { out.append(c); }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private String[] split(String line, int expected) {
        List<String> parts = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) {
                cur.append(c).append(line.charAt(i + 1));
                i++;
            } else if (c == '|') {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    private int parseIdNum(String id, char prefix) {
        if (id == null || id.length() < 2 || id.charAt(0) != prefix) return 0;
        try { return Integer.parseInt(id.substring(1)); } catch (Exception e) { return 0; }
    }

    private int parseRefNum(String ref) {
        if (ref == null || !ref.startsWith("TRF")) return 0;
        try { return Integer.parseInt(ref.substring(3)); } catch (Exception e) { return 0; }
    }
}