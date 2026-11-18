package util;

import java.text.NumberFormat;
import java.util.Locale;

public final class Format {

    private Format() {
    }

    public static String rupiah(double nilai) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(nilai);
    }

    public static String angka(double nilai) {
        return String.format(Locale.US, "%.2f", nilai);
    }
}
