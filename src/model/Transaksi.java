package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class Transaksi {

    private static final DateTimeFormatter FORMAT_WAKTU =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final String id;
    private final LocalDateTime waktu;

    public Transaksi(String id) {
        this.id = id;
        this.waktu = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getWaktu() {
        return waktu;
    }

    public String getWaktuFormatted() {
        return waktu.format(FORMAT_WAKTU);
    }

    public abstract double getJumlah();

    public abstract String getDeskripsi();

    public double getBeratKg() {
        return 0;
    }

    public String getKategoriSampah() {
        return null;
    }
}
