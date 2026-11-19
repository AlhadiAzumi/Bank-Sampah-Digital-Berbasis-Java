package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import model.Elektronik;
import model.Karet;
import model.Logam;
import model.Nasabah;
import model.Penarikan;
import model.Plastik;
import model.Sampah;
import model.SampahKustom;
import model.Setoran;
import model.Transaksi;
import service.BankSampah;

public class WebServer {

    private static final Path WEB_DIR = Paths.get("web");

    private final BankSampah bank;
    private final HttpServer server;

    public WebServer(int port) throws IOException {
        bank = new BankSampah();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                handleApi(exchange);
            }
        });
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                handleStatic(exchange);
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
    }

    public static void launch(String[] args) {
        int port = 8080;
        for (String arg : args) {
            if (arg != null && arg.startsWith("--port=")) {
                try {
                    port = Integer.parseInt(arg.substring("--port=".length()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        try {
            WebServer web = new WebServer(port);
            web.start();
        } catch (IOException e) {
            System.err.println("Gagal menjalankan web server: " + e.getMessage());
        }
    }

    public void start() {
        server.start();
        System.out.println("TrashChain (web) siap di http://localhost:" + server.getAddress().getPort());
        System.out.println("Tekan Ctrl+C untuk menghentikan server.");
    }

    public void stop() {
        server.stop(0);
    }

    private void handleApi(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            Map<String, String> form = "POST".equals(method)
                    ? parseQuery(readBody(exchange)) : Collections.<String, String>emptyMap();

            if ("GET".equals(method)) {
                if (path.equals("/api/dashboard")) {
                    sendJson(exchange, 200, dashboardJson());
                } else if (path.equals("/api/nasabah")) {
                    sendJson(exchange, 200, nasabahListJson());
                } else if (path.equals("/api/sampah")) {
                    sendJson(exchange, 200, sampahListJson());
                } else if (path.equals("/api/riwayat")) {
                    sendJson(exchange, 200, riwayatJson(query));
                } else if (path.equals("/api/laporan")) {
                    sendJson(exchange, 200, laporanJson());
                } else if (path.equals("/api/export/riwayat")) {
                    exportCsv(exchange, "riwayat");
                } else if (path.equals("/api/export/laporan")) {
                    exportCsv(exchange, "laporan");
                } else if (path.startsWith("/api/struk/")) {
                    sendStruk(exchange, path.substring("/api/struk/".length()));
                } else {
                    sendError(exchange, 404, "Endpoint tidak ditemukan.");
                }
                return;
            }

            if ("POST".equals(method)) {
                if (path.equals("/api/nasabah")) {
                    Nasabah n = bank.tambahNasabah(form.get("nama"), form.get("alamat"),
                            form.get("noHp"), form.get("bankEwallet"), form.get("noRekening"));
                    sendJson(exchange, 200, "{\"ok\":true,\"nasabah\":" + nasabahJson(n) + "}");
                } else if (path.equals("/api/sampah")) {
                    Sampah s = bank.tambahJenisSampah(form.get("nama"), angka(form, "hargaPerKg", "Harga/kg"),
                            form.get("kategori"), form.get("aturan"));
                    sendJson(exchange, 200, "{\"ok\":true,\"sampah\":" + sampahJson(s) + "}");
                } else if (path.equals("/api/sampah/harga")) {
                    Sampah diperbarui = bank.updateHargaSampah(form.get("nama"), angka(form, "hargaPerKg", "Harga/kg"));
                    sendJson(exchange, 200, "{\"ok\":true,\"sampah\":" + sampahJson(diperbarui) + "}");
                } else if (path.equals("/api/setoran")) {
                    String id = form.get("idNasabah");
                    Setoran setoran = bank.setorSampah(id, form.get("namaSampah"),
                            angka(form, "beratKg", "Berat"));
                    sendJson(exchange, 200, "{\"ok\":true,\"id\":" + q(setoran.getId())
                            + ",\"nama\":" + q(setoran.getSampah().getNama())
                            + ",\"berat\":" + kg(setoran.getBeratKg())
                            + ",\"jumlah\":" + nilai(setoran.getJumlah())
                            + ",\"saldo\":" + nilai(bank.cariNasabah(id).getSaldo()) + "}");
                } else if (path.equals("/api/pencairan")) {
                    String id = form.get("idNasabah");
                    Nasabah nasabah = bank.cariNasabah(id);
                    Penarikan penarikan = bank.tarikSaldo(id, angka(form, "jumlah", "Jumlah pencairan"));
                    double sisa = nasabah.getSaldo();
                    String struk = "";
                    try {
                        bank.buatStrukTransfer(nasabah, penarikan, sisa);
                        struk = "/api/struk/" + penarikan.getNoReferensi();
                    } catch (RuntimeException ignored) {
                    }
                    sendJson(exchange, 200, "{\"ok\":true,\"ref\":" + q(penarikan.getNoReferensi())
                            + ",\"waktu\":" + q(penarikan.getWaktuFormatted())
                            + ",\"nama\":" + q(nasabah.getNama())
                            + ",\"jumlah\":" + nilai(penarikan.getJumlah())
                            + ",\"tujuan\":" + q(nasabah.getTujuanTransfer())
                            + ",\"saldoSisa\":" + nilai(sisa)
                            + ",\"struk\":" + q(struk) + "}");
                } else {
                    sendError(exchange, 404, "Endpoint tidak ditemukan.");
                }
                return;
            }

            sendError(exchange, 405, "Metode tidak didukung.");
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            sendError(exchange, 500, "Terjadi kesalahan: " + e.getMessage());
        }
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String file;
        String type;
        if (path.equals("/") || path.equals("/index.html")) {
            file = "index.html";
            type = "text/html; charset=utf-8";
        } else if (path.equals("/styles.css")) {
            file = "styles.css";
            type = "text/css; charset=utf-8";
        } else if (path.equals("/app.js")) {
            file = "app.js";
            type = "application/javascript; charset=utf-8";
        } else if (path.endsWith(".png")) {
            file = path.startsWith("/") ? path.substring(1) : path;
            type = "image/png";
        } else if (path.endsWith(".ico")) {
            file = path.startsWith("/") ? path.substring(1) : path;
            type = "image/x-icon";
        } else if (path.endsWith(".svg")) {
            file = path.startsWith("/") ? path.substring(1) : path;
            type = "image/svg+xml";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            file = path.startsWith("/") ? path.substring(1) : path;
            type = "image/jpeg";
        } else {
            sendError(exchange, 404, "Halaman tidak ditemukan.");
            return;
        }
        Path target = WEB_DIR.resolve(file).normalize();
        if (!target.startsWith(WEB_DIR) || !Files.exists(target)) {
            sendError(exchange, 404, "File web tidak ditemukan: " + file + ". Jalankan dari folder project.");
            return;
        }
        send(exchange, 200, type, Files.readAllBytes(target), null);
    }

    private String dashboardJson() {
        int jumlahNasabah = bank.getDaftarNasabah().size();
        int jumlahSetoran = 0;
        int jumlahPencairan = 0;
        double totalBerat = 0;
        double totalNilaiSetoran = 0;
        double totalDanaDicairkan = 0;
        Map<String, double[]> perKategori = new LinkedHashMap<String, double[]>();
        List<Baris> semua = new ArrayList<Baris>();

        for (Nasabah n : bank.getDaftarNasabah()) {
            for (Transaksi t : n.getRiwayat()) {
                boolean setoran = t instanceof Setoran;
                if (setoran) {
                    jumlahSetoran++;
                    totalBerat += t.getBeratKg();
                    totalNilaiSetoran += t.getJumlah();
                    double[] rekap = perKategori.get(t.getKategoriSampah());
                    if (rekap == null) {
                        rekap = new double[2];
                        perKategori.put(t.getKategoriSampah(), rekap);
                    }
                    rekap[0] += t.getBeratKg();
                    rekap[1] += t.getJumlah();
                } else {
                    jumlahPencairan++;
                    totalDanaDicairkan += t.getJumlah();
                }
                semua.add(new Baris(t.getWaktu(), transaksiJson(n, t)));
            }
        }
        Collections.sort(semua, new Comparator<Baris>() {
            @Override
            public int compare(Baris a, Baris b) {
                return b.waktu.compareTo(a.waktu);
            }
        });

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"jumlahNasabah\":").append(jumlahNasabah).append(',');
        sb.append("\"jumlahSetoran\":").append(jumlahSetoran).append(',');
        sb.append("\"jumlahPencairan\":").append(jumlahPencairan).append(',');
        sb.append("\"totalBerat\":").append(kg(totalBerat)).append(',');
        sb.append("\"totalNilaiSetoran\":").append(nilai(totalNilaiSetoran)).append(',');
        sb.append("\"totalDanaDicairkan\":").append(nilai(totalDanaDicairkan)).append(',');
        sb.append("\"saldoBeredar\":").append(nilai(bank.getTotalSaldoBeredar())).append(',');
        sb.append("\"kategori\":[");
        boolean first = true;
        for (Map.Entry<String, double[]> e : perKategori.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append("{\"nama\":").append(q(e.getKey()))
                    .append(",\"berat\":").append(kg(e.getValue()[0]))
                    .append(",\"nilai\":").append(nilai(e.getValue()[1])).append('}');
        }
        sb.append("],\"terbaru\":[");
        first = true;
        for (int i = 0; i < Math.min(6, semua.size()); i++) {
            if (!first) sb.append(',');
            first = false;
            sb.append(semua.get(i).json);
        }
        sb.append("]}");
        return sb.toString();
    }

    private String nasabahListJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bank.getDaftarNasabah().size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(nasabahJson(bank.getDaftarNasabah().get(i)));
        }
        return sb.append(']').toString();
    }

    private String nasabahJson(Nasabah n) {
        return "{\"id\":" + q(n.getId())
                + ",\"nama\":" + q(n.getNama())
                + ",\"alamat\":" + q(n.getAlamat())
                + ",\"noHp\":" + q(n.getNoHp())
                + ",\"bankEwallet\":" + q(n.getBankEwallet())
                + ",\"noRekening\":" + q(n.getNoRekening())
                + ",\"tujuanTransfer\":" + q(n.getTujuanTransfer())
                + ",\"saldo\":" + nilai(n.getSaldo())
                + ",\"jumlahTransaksi\":" + n.getRiwayat().size() + "}";
    }

    private String sampahListJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bank.getDaftarSampah().size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(sampahJson(bank.getDaftarSampah().get(i)));
        }
        return sb.append(']').toString();
    }

    private String sampahJson(Sampah s) {
        String aturan = "NORMAL";
        if (s instanceof SampahKustom) aturan = ((SampahKustom) s).getAturan();
        else if (s instanceof Plastik) aturan = "BONUS5_MIN10";
        else if (s instanceof Logam) aturan = "BONUS5";
        else if (s instanceof Elektronik) aturan = "BONUS5_MIN5";
        else if (s instanceof Karet) aturan = "BONUS5";
        return "{\"nama\":" + q(s.getNama())
                + ",\"hargaPerKg\":" + nilai(s.getHargaPerKg())
                + ",\"kategori\":" + q(s.getKategori())
                + ",\"aturan\":" + q(aturan)
                + ",\"aturanLabel\":" + q(s.getAturanDeskripsi()) + "}";
    }

    private String riwayatJson(Map<String, String> filter) {
        String idNasabah = filter.get("idNasabah");
        String jenis = filter.get("jenis");
        String cari = filter.get("q") == null ? "" : filter.get("q").trim().toLowerCase(Locale.ROOT);
        LocalDateTime dari = awalTanggal(filter.get("dari"));
        LocalDateTime sampai = akhirTanggal(filter.get("sampai"));

        List<Baris> rows = new ArrayList<Baris>();
        for (Nasabah n : bank.getDaftarNasabah()) {
            if (idNasabah != null && !idNasabah.trim().isEmpty()
                    && !n.getId().equalsIgnoreCase(idNasabah.trim())) continue;
            for (Transaksi t : n.getRiwayat()) {
                boolean setoran = t instanceof Setoran;
                String tipe = setoran ? "Setoran" : "Pencairan/Transfer";
                if (jenis != null && !jenis.trim().isEmpty() && !jenis.equals("Semua")
                        && !tipe.equalsIgnoreCase(jenis.trim())) continue;
                if (dari != null && t.getWaktu().isBefore(dari)) continue;
                if (sampai != null && t.getWaktu().isAfter(sampai)) continue;
                if (!cari.isEmpty()) {
                    String teks = (t.getId() + " " + t.getDeskripsi() + " " + n.getNama() + " " + n.getId()).toLowerCase(Locale.ROOT);
                    if (!teks.contains(cari)) continue;
                }
                rows.add(new Baris(t.getWaktu(), transaksiJson(n, t)));
            }
        }
        Collections.sort(rows, new Comparator<Baris>() {
            @Override
            public int compare(Baris a, Baris b) {
                return b.waktu.compareTo(a.waktu);
            }
        });
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(rows.get(i).json);
        }
        return sb.append(']').toString();
    }

    private String transaksiJson(Nasabah n, Transaksi t) {
        boolean setoran = t instanceof Setoran;
        String struk = (!setoran && Files.exists(Paths.get("struk", "Struk_" + t.getId() + ".html")))
                ? "/api/struk/" + t.getId() : "";
        return "{\"id\":" + q(t.getId())
                + ",\"waktu\":" + q(t.getWaktuFormatted())
                + ",\"idNasabah\":" + q(n.getId())
                + ",\"namaNasabah\":" + q(n.getNama())
                + ",\"tipe\":" + q(setoran ? "Setoran" : "Pencairan/Transfer")
                + ",\"deskripsi\":" + q(t.getDeskripsi())
                + ",\"jumlah\":" + nilai(t.getJumlah())
                + ",\"berat\":" + (setoran ? kg(t.getBeratKg()) : "0")
                + ",\"kategori\":" + q(setoran ? t.getKategoriSampah() : "")
                + ",\"struk\":" + q(struk) + "}";
    }

    private String laporanJson() {
        Map<String, double[]> perKategori = new LinkedHashMap<String, double[]>();
        double totalBerat = 0;
        double totalNilaiSetoran = 0;
        double totalNilaiPencairan = 0;
        int jumlahSetoran = 0;
        int jumlahPencairan = 0;
        for (Nasabah n : bank.getDaftarNasabah()) {
            for (Transaksi t : n.getRiwayat()) {
                if (t instanceof Setoran) {
                    jumlahSetoran++;
                    totalBerat += t.getBeratKg();
                    totalNilaiSetoran += t.getJumlah();
                    double[] rekap = perKategori.get(t.getKategoriSampah());
                    if (rekap == null) {
                        rekap = new double[2];
                        perKategori.put(t.getKategoriSampah(), rekap);
                    }
                    rekap[0] += t.getBeratKg();
                    rekap[1] += t.getJumlah();
                } else {
                    jumlahPencairan++;
                    totalNilaiPencairan += t.getJumlah();
                }
            }
        }
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"jumlahNasabah\":").append(bank.getDaftarNasabah().size()).append(',');
        sb.append("\"jumlahSetoran\":").append(jumlahSetoran).append(',');
        sb.append("\"jumlahPencairan\":").append(jumlahPencairan).append(',');
        sb.append("\"totalBerat\":").append(kg(totalBerat)).append(',');
        sb.append("\"totalNilaiSetoran\":").append(nilai(totalNilaiSetoran)).append(',');
        sb.append("\"totalDanaDicairkan\":").append(nilai(totalNilaiPencairan)).append(',');
        sb.append("\"saldoBeredar\":").append(nilai(bank.getTotalSaldoBeredar())).append(',');
        sb.append("\"kategori\":[");
        boolean first = true;
        for (Map.Entry<String, double[]> e : perKategori.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append("{\"nama\":").append(q(e.getKey()))
                    .append(",\"berat\":").append(kg(e.getValue()[0]))
                    .append(",\"nilai\":").append(nilai(e.getValue()[1])).append('}');
        }
        return sb.append("]}").toString();
    }

    private void exportCsv(HttpExchange exchange, String jenis) throws IOException {
        Path file = Files.createTempFile("bank_sampah_" + jenis + "_", ".csv");
        try {
            if ("riwayat".equals(jenis)) bank.exportRiwayatCsv(file);
            else bank.exportLaporanCsv(file);
            String name = "riwayat".equals(jenis) ? "riwayat_bank_sampah.csv" : "laporan_bank_sampah.csv";
            send(exchange, 200, "text/csv; charset=utf-8", Files.readAllBytes(file),
                    "attachment; filename=\"" + name + "\"");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private void sendStruk(HttpExchange exchange, String ref) throws IOException {
        if (!ref.matches("[A-Za-z0-9]+")) {
            sendError(exchange, 400, "Nomor referensi tidak valid.");
            return;
        }
        Path file = Paths.get("struk", "Struk_" + ref + ".html");
        if (!Files.exists(file)) {
            sendError(exchange, 404, "Struk " + ref + " tidak ditemukan.");
            return;
        }
        send(exchange, 200, "text/html; charset=utf-8", Files.readAllBytes(file), null);
    }

    private static LocalDateTime awalTanggal(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(s.trim()).atStartOfDay();
        } catch (Exception e) {
            throw new IllegalArgumentException("Format tanggal dari harus yyyy-MM-dd.");
        }
    }

    private static LocalDateTime akhirTanggal(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(s.trim()).atTime(LocalTime.MAX);
        } catch (Exception e) {
            throw new IllegalArgumentException("Format tanggal sampai harus yyyy-MM-dd.");
        }
    }

    private static double angka(Map<String, String> form, String key, String label) {
        String value = form.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " harus berupa angka.");
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> result = new HashMap<String, String>();
        if (raw == null || raw.isEmpty()) return result;
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            String key = i >= 0 ? pair.substring(0, i) : pair;
            String value = i >= 0 ? pair.substring(i + 1) : "";
            try {
                result.put(URLDecoder.decode(key, "UTF-8"), URLDecoder.decode(value, "UTF-8"));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream in = exchange.getRequestBody();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String q(String s) {
        if (s == null) return "\"\"";
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else if (c < 0x20) out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
            else out.append(c);
        }
        return out.append('"').toString();
    }

    private static String nilai(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String kg(double value) {
        return String.format(Locale.US, "%.4f", value);
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8), null);
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, "{\"error\":" + q(message == null ? "Terjadi kesalahan." : message) + "}");
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body,
                             String disposition) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        if (disposition != null) {
            exchange.getResponseHeaders().set("Content-Disposition", disposition);
        }
        exchange.sendResponseHeaders(status, body.length);
        OutputStream out = exchange.getResponseBody();
        out.write(body);
        out.close();
    }

    private static class Baris {
        private final LocalDateTime waktu;
        private final String json;

        private Baris(LocalDateTime waktu, String json) {
            this.waktu = waktu;
            this.json = json;
        }
    }
}
