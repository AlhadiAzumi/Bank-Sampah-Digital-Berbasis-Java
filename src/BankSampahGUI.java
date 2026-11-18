import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import model.Nasabah;
import model.Penarikan;
import model.Sampah;
import model.Setoran;
import model.Transaksi;
import service.BankSampah;
import util.Format;

public class BankSampahGUI extends JFrame {

    private final BankSampah bank = new BankSampah();

    private DefaultTableModel nasabahTableModel;
    private DefaultTableModel riwayatTableModel;
    private JComboBox<String> setorNasabahCombo;
    private JComboBox<String> setorSampahCombo;
    private JComboBox<String> tarikNasabahCombo;
    private JComboBox<String> riwayatNasabahCombo;
    private JTextField namaField;
    private JTextField alamatField;
    private JTextField noHpField;
    private JTextField bankEwalletField;
    private JTextField noRekeningField;
    private JTextField beratField;
    private JTextField jumlahTarikField;
    private JLabel laporanLabel;

    public BankSampahGUI() {
        setTitle("Bank Sampah Digital");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 550));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 247, 250));
        root.add(createHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tabs.addTab("Nasabah", createNasabahPanel());
        tabs.addTab("Setor Sampah", createSetorPanel());
        tabs.addTab("Pencairan Saldo", createTarikPanel());
        tabs.addTab("Riwayat", createRiwayatPanel());
        tabs.addTab("Laporan", createLaporanPanel());

        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        refreshAll();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(34, 139, 87));
        header.setOpaque(true);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("Bank Sampah Digital");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Kelola nasabah, setoran, pencairan saldo digital, dan laporan");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(230, 245, 235));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(subtitle);

        header.add(text, BorderLayout.WEST);
        return header;
    }

    private JPanel createNasabahPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Tambah Nasabah"));
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        namaField = new JTextField(20);
        alamatField = new JTextField(20);
        noHpField = new JTextField(20);
        bankEwalletField = new JTextField(20);
        noRekeningField = new JTextField(20);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(new JLabel("Nama"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(namaField, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        form.add(new JLabel("Alamat"), gbc);
        gbc.gridx = 3; gbc.weightx = 1;
        form.add(alamatField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(new JLabel("No HP"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(noHpField, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0;
        form.add(new JLabel("Bank/E-Wallet"), gbc);
        gbc.gridx = 3; gbc.weightx = 1;
        form.add(bankEwalletField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(new JLabel("No. Rekening/Tujuan"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(noRekeningField, gbc);

        JButton tambahBtn = primaryButton("Tambah Nasabah");
        tambahBtn.addActionListener(e -> tambahNasabah());
        gbc.gridx = 3; gbc.gridy = 2; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        form.add(tambahBtn, gbc);

        nasabahTableModel = new DefaultTableModel(
                new Object[]{"ID", "Nama", "Alamat", "No HP", "Bank/E-Wallet", "No. Rekening", "Saldo"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(nasabahTableModel);
        styleTable(table);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSetorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setOpaque(false);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));

        GridBagConstraints gbc = baseGbc();
        JLabel heading = new JLabel("Setor Sampah");
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        card.add(heading, gbc);

        setorNasabahCombo = new JComboBox<>();
        setorSampahCombo = new JComboBox<>();
        for (Sampah s : bank.getDaftarSampah()) {
            setorSampahCombo.addItem(s.getNama() + " (" + Format.rupiah(s.getHargaPerKg()) + "/kg)");
        }
        beratField = new JTextField();

        gbc.gridwidth = 1;
        addRow(card, gbc, 1, "Nasabah", setorNasabahCombo);
        addRow(card, gbc, 2, "Jenis Sampah", setorSampahCombo);
        addRow(card, gbc, 3, "Berat (kg)", beratField);

        JButton setorBtn = primaryButton("Proses Setoran");
        setorBtn.addActionListener(e -> setorSampah());
        gbc.gridx = 1; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST;
        card.add(setorBtn, gbc);

        JLabel info = new JLabel("Nilai dihitung otomatis sesuai jenis sampah dan aturan bonus.");
        info.setForeground(new Color(90, 100, 110));
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        card.add(info, gbc);

        panel.add(card, new GridBagConstraints());
        return panel;
    }

    private JPanel createTarikPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setOpaque(false);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));

        GridBagConstraints gbc = baseGbc();
        JLabel heading = new JLabel("Pencairan Saldo (Transfer Digital)");
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        card.add(heading, gbc);

        tarikNasabahCombo = new JComboBox<>();
        jumlahTarikField = new JTextField();

        gbc.gridwidth = 1;
        addRow(card, gbc, 1, "Nasabah", tarikNasabahCombo);
        addRow(card, gbc, 2, "Jumlah (Rp)", jumlahTarikField);

        JButton tarikBtn = primaryButton("Proses Transfer");
        tarikBtn.addActionListener(e -> tarikSaldo());
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        card.add(tarikBtn, gbc);

        JLabel info = new JLabel("Saldo ditransfer ke rekening/e-wallet terdaftar nasabah dan tercatat dengan no. referensi.");
        info.setForeground(new Color(90, 100, 110));
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        card.add(info, gbc);

        panel.add(card, new GridBagConstraints());
        return panel;
    }

    private JPanel createRiwayatPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        top.add(new JLabel("Nasabah:"));
        riwayatNasabahCombo = new JComboBox<>();
        riwayatNasabahCombo.addActionListener(e -> refreshRiwayat());
        top.add(riwayatNasabahCombo);
        JButton refresh = new JButton("Muat Riwayat");
        refresh.addActionListener(e -> refreshRiwayat());
        top.add(refresh);

        riwayatTableModel = new DefaultTableModel(new Object[]{"ID", "Waktu", "Deskripsi"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(riwayatTableModel);
        styleTable(table);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLaporanPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        JButton refresh = primaryButton("Perbarui Laporan");
        refresh.addActionListener(e -> refreshLaporan());
        top.add(refresh);

        laporanLabel = new JLabel();
        laporanLabel.setVerticalAlignment(SwingConstants.TOP);
        laporanLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        content.add(laporanLabel, BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(content), BorderLayout.CENTER);
        return panel;
    }

    private void tambahNasabah() {
        try {
            Nasabah n = bank.tambahNasabah(namaField.getText(), alamatField.getText(), noHpField.getText(),
                    bankEwalletField.getText(), noRekeningField.getText());
            JOptionPane.showMessageDialog(this,
                    "Nasabah berhasil ditambahkan dengan ID " + n.getId() + ".\nTujuan transfer: " + n.getTujuanTransfer(),
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            namaField.setText("");
            alamatField.setText("");
            noHpField.setText("");
            bankEwalletField.setText("");
            noRekeningField.setText("");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void setorSampah() {
        String nasabahId = selectedId(setorNasabahCombo);
        int sampahIndex = setorSampahCombo.getSelectedIndex();
        if (nasabahId == null) { showError("Pilih nasabah terlebih dahulu."); return; }
        if (sampahIndex < 0) { showError("Pilih jenis sampah."); return; }
        try {
            double berat = parseDouble(beratField.getText(), "Berat");
            String namaSampah = bank.getDaftarSampah().get(sampahIndex).getNama();
            Setoran setoran = bank.setorSampah(nasabahId, namaSampah, berat);
            double saldo = bank.cariNasabah(nasabahId).getSaldo();
            JOptionPane.showMessageDialog(this,
                    "Setoran berhasil!\nNilai setoran: " + Format.rupiah(setoran.getJumlah())
                            + "\nSaldo sekarang: " + Format.rupiah(saldo),
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            beratField.setText("");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void tarikSaldo() {
        String nasabahId = selectedId(tarikNasabahCombo);
        if (nasabahId == null) { showError("Pilih nasabah terlebih dahulu."); return; }
        try {
            double jumlah = parseDouble(jumlahTarikField.getText(), "Jumlah pencairan");
            Nasabah nasabah = bank.cariNasabah(nasabahId);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Transfer " + Format.rupiah(jumlah) + " ke\n" + nasabah.getTujuanTransfer()
                            + "?\nSaldo saat ini: " + Format.rupiah(nasabah.getSaldo()),
                    "Konfirmasi Pencairan", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            Penarikan penarikan = bank.tarikSaldo(nasabahId, jumlah);
            double saldo = bank.cariNasabah(nasabahId).getSaldo();
            JOptionPane.showMessageDialog(this,
                    "Pencairan berhasil diproses!\nNo. Referensi: " + penarikan.getNoReferensi()
                            + "\nTujuan: " + nasabah.getTujuanTransfer()
                            + "\nJumlah: " + Format.rupiah(penarikan.getJumlah())
                            + "\nSaldo sekarang: " + Format.rupiah(saldo),
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            jumlahTarikField.setText("");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshAll() {
        refreshNasabahTable();
        refreshNasabahCombos();
        refreshRiwayat();
        refreshLaporan();
    }

    private void refreshNasabahTable() {
        nasabahTableModel.setRowCount(0);
        for (Nasabah n : bank.getDaftarNasabah()) {
            nasabahTableModel.addRow(new Object[]{n.getId(), n.getNama(), n.getAlamat(), n.getNoHp(),
                    n.getBankEwallet(), n.getNoRekening(), Format.rupiah(n.getSaldo())});
        }
    }

    private void refreshNasabahCombos() {
        updateCombo(setorNasabahCombo);
        updateCombo(tarikNasabahCombo);
        String selectedRiwayat = selectedId(riwayatNasabahCombo);
        updateCombo(riwayatNasabahCombo);
        if (selectedRiwayat != null) {
            selectId(riwayatNasabahCombo, selectedRiwayat);
        }
    }

    private void updateCombo(JComboBox<String> combo) {
        if (combo == null) return;
        combo.removeAllItems();
        for (Nasabah n : bank.getDaftarNasabah()) {
            combo.addItem(n.getId() + " - " + n.getNama() + " (" + Format.rupiah(n.getSaldo()) + ")");
        }
    }

    private void refreshRiwayat() {
        if (riwayatTableModel == null) return;
        riwayatTableModel.setRowCount(0);
        String id = selectedId(riwayatNasabahCombo);
        if (id == null) return;
        try {
            Nasabah n = bank.cariNasabah(id);
            for (Transaksi t : n.getRiwayat()) {
                riwayatTableModel.addRow(new Object[]{t.getId(), t.getWaktuFormatted(), t.getDeskripsi()});
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void refreshLaporan() {
        Map<String, Double> totalBeratPerKategori = new LinkedHashMap<String, Double>();
        double totalNilaiSetoran = 0;
        double totalNilaiPencairan = 0;
        int jumlahSetoran = 0;
        int jumlahPencairan = 0;
        for (Nasabah nasabah : bank.getDaftarNasabah()) {
            for (Transaksi transaksi : nasabah.getRiwayat()) {
                if (transaksi.getKategoriSampah() != null) {
                    String key = transaksi.getKategoriSampah();
                    Double prev = totalBeratPerKategori.get(key);
                    totalBeratPerKategori.put(key, (prev == null ? 0 : prev) + transaksi.getBeratKg());
                    totalNilaiSetoran += transaksi.getJumlah();
                    jumlahSetoran++;
                } else {
                    jumlahPencairan++;
                    totalNilaiPencairan += transaksi.getJumlah();
                }
            }
        }
        double totalSaldo = 0;
        for (Nasabah n : bank.getDaftarNasabah()) totalSaldo += n.getSaldo();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:sans-serif;'>");
        sb.append("<h2>Laporan Bank Sampah</h2>");
        sb.append("<table cellspacing='6'>");
        row(sb, "Jumlah nasabah", String.valueOf(bank.getDaftarNasabah().size()));
        row(sb, "Jumlah setoran", String.valueOf(jumlahSetoran));
        row(sb, "Jumlah pencairan (transfer)", String.valueOf(jumlahPencairan));
        row(sb, "Total nilai setoran", Format.rupiah(totalNilaiSetoran));
        row(sb, "Total dana dicairkan", Format.rupiah(totalNilaiPencairan));
        row(sb, "Total saldo beredar", Format.rupiah(totalSaldo));
        sb.append("</table>");
        sb.append("<h3>Total sampah terkelola per kategori</h3>");
        if (totalBeratPerKategori.isEmpty()) {
            sb.append("<p>Belum ada sampah yang disetor.</p>");
        } else {
            sb.append("<ul>");
            for (Map.Entry<String, Double> e : totalBeratPerKategori.entrySet()) {
                sb.append("<li>").append(escape(e.getKey())).append(": ")
                        .append(Format.angka(e.getValue())).append(" kg</li>");
            }
            sb.append("</ul>");
        }
        sb.append("</body></html>");
        laporanLabel.setText(sb.toString());
    }

    private void row(StringBuilder sb, String label, String value) {
        sb.append("<tr><td><b>").append(escape(label)).append("</b></td><td>: ")
                .append(escape(value)).append("</td></tr>");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String selectedId(JComboBox<String> combo) {
        if (combo == null) return null;
        Object sel = combo.getSelectedItem();
        if (sel == null) return null;
        String s = sel.toString();
        int idx = s.indexOf(" - ");
        return idx > 0 ? s.substring(0, idx).trim() : s.trim();
    }

    private void selectId(JComboBox<String> combo, String id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i);
            if (item.startsWith(id + " - ")) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private double parseDouble(String text, String label) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " tidak boleh kosong.");
        }
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " harus berupa angka.");
        }
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        if (field instanceof JTextField) ((JTextField) field).setColumns(18);
        panel.add(field, gbc);
    }

    private GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(232, 245, 233));
        btn.setForeground(new Color(27, 94, 32));
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(165, 214, 167)),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        return btn;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(26);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Gagal", JOptionPane.ERROR_MESSAGE);
    }

    public static void launch() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new BankSampahGUI().setVisible(true);
            }
        });
    }
}