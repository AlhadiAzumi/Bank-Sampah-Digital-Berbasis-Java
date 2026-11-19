import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

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
    private DefaultTableModel sampahTableModel;
    private TableRowSorter<DefaultTableModel> nasabahSorter;
    private JComboBox<String> setorNasabahCombo;
    private JComboBox<String> setorSampahCombo;
    private JComboBox<String> tarikNasabahCombo;
    private JComboBox<String> riwayatNasabahCombo;
    private JComboBox<String> riwayatJenisCombo;
    private JComboBox<String> aturanSampahCombo;
    private JTextField namaField;
    private JTextField alamatField;
    private JTextField noHpField;
    private JTextField bankEwalletField;
    private JTextField noRekeningField;
    private JTextField beratField;
    private JTextField jumlahTarikField;
    private JTextField searchField;
    private JTextField riwayatDariField;
    private JTextField riwayatSampaiField;
    private JTextField riwayatCariField;
    private JTextField namaSampahBaruField;
    private JTextField hargaSampahField;
    private JTextField kategoriSampahField;
    private JLabel tujuanTransferLabel;
    private JLabel statusLabel;
    private JLabel laporanLabel;
    private JLabel dashNasabahVal;
    private JLabel dashSetorVal;
    private JLabel dashCairVal;
    private JLabel dashBeratVal;
    private JLabel dashSaldoVal;
    private JLabel dashSetoranVal;
    private JLabel dashDicairkanVal;

    public BankSampahGUI() {
        setTitle("TrashChain");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 760);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(980, 640));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 247, 250));
        root.add(createHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tabs.addTab("Dashboard", createDashboardPanel());
        tabs.addTab("Nasabah", createNasabahPanel());
        tabs.addTab("Setor Sampah", createSetorPanel());
        tabs.addTab("Pencairan Saldo", createTarikPanel());
        tabs.addTab("Riwayat", createRiwayatPanel());
        tabs.addTab("Jenis Sampah", createSampahPanel());
        tabs.addTab("Laporan", createLaporanPanel());

        root.add(tabs, BorderLayout.CENTER);
        root.add(createFooter(), BorderLayout.SOUTH);
        setContentPane(root);
        refreshAll();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(34, 139, 87));
        header.setOpaque(true);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("TrashChain");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Setoran, pencairan transfer digital + struk, riwayat & laporan (persisten)");
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

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(235, 238, 242));
        footer.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        statusLabel = new JLabel("Data tersimpan otomatis ke folder data/ (persisten). Struk tersimpan di folder struk/.");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(70, 80, 90));
        footer.add(statusLabel, BorderLayout.WEST);
        JLabel hint = new JLabel("Pencairan = transfer digital ke rekening/e-wallet terdaftar + No. Referensi");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setForeground(new Color(70, 80, 90));
        footer.add(hint, BorderLayout.EAST);
        return footer;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        JPanel grid = new JPanel(new java.awt.GridLayout(2, 4, 12, 12));
        grid.setOpaque(false);
        dashNasabahVal = createStatCard(grid, "Nasabah Terdaftar");
        dashSetorVal = createStatCard(grid, "Total Setoran");
        dashCairVal = createStatCard(grid, "Total Pencairan");
        dashBeratVal = createStatCard(grid, "Sampah Terkelola");
        dashSaldoVal = createStatCard(grid, "Saldo Beredar");
        dashSetoranVal = createStatCard(grid, "Nilai Setoran");
        dashDicairkanVal = createStatCard(grid, "Dana Dicairkan");
        JPanel infoCard = new JPanel(new BorderLayout());
        infoCard.setBackground(Color.WHITE);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel info = new JLabel("<html><b>Info</b><br/>Data & jenis sampah tersimpan di <code>data/</code>.<br/>Struk transfer di <code>struk/</code>.</html>");
        info.setFont(new Font("SansSerif", Font.PLAIN, 13));
        infoCard.add(info, BorderLayout.CENTER);
        grid.add(infoCard);

        JButton refresh = primaryButton("Perbarui Dashboard");
        refresh.addActionListener(e -> refreshDashboard());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(refresh);

        panel.add(top, BorderLayout.NORTH);
        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createStatCard(JPanel parent, String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setForeground(new Color(90, 100, 110));
        JLabel v = new JLabel("-");
        v.setFont(new Font("SansSerif", Font.BOLD, 20));
        v.setForeground(new Color(30, 60, 40));
        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        parent.add(card);
        return v;
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

        namaField = new JTextField(18);
        alamatField = new JTextField(18);
        noHpField = new JTextField(18);
        bankEwalletField = new JTextField(18);
        noRekeningField = new JTextField(18);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; form.add(new JLabel("Nama"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(namaField, gbc);
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0; form.add(new JLabel("Alamat"), gbc);
        gbc.gridx = 3; gbc.weightx = 1; form.add(alamatField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; form.add(new JLabel("No HP (angka)"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(noHpField, gbc);
        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0; form.add(new JLabel("Bank/E-Wallet"), gbc);
        gbc.gridx = 3; gbc.weightx = 1; form.add(bankEwalletField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; form.add(new JLabel("No. Rekening/Tujuan (angka)"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(noRekeningField, gbc);

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
        nasabahSorter = new TableRowSorter<DefaultTableModel>(nasabahTableModel);
        table.setRowSorter(nasabahSorter);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setNasabahColumnWidths(table);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Cari nasabah:"));
        searchField = new JTextField(24);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterNasabah(); }
            public void removeUpdate(DocumentEvent e) { filterNasabah(); }
            public void changedUpdate(DocumentEvent e) { filterNasabah(); }
        });
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("(ID/nama/bank/no. rekening)"));

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(searchPanel, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        panel.add(form, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private void filterNasabah() {
        if (nasabahSorter == null || searchField == null) return;
        String text = searchField.getText();
        if (text == null || text.trim().isEmpty()) {
            nasabahSorter.setRowFilter(null);
        } else {
            try {
                nasabahSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text.trim())));
            } catch (Exception e) {
                nasabahSorter.setRowFilter(null);
            }
        }
    }

    private void setNasabahColumnWidths(JTable table) {
        TableColumnModel cm = table.getColumnModel();
        if (cm.getColumnCount() < 7) return;
        cm.getColumn(0).setPreferredWidth(60);
        cm.getColumn(1).setPreferredWidth(140);
        cm.getColumn(2).setPreferredWidth(170);
        cm.getColumn(3).setPreferredWidth(110);
        cm.getColumn(4).setPreferredWidth(120);
        cm.getColumn(5).setPreferredWidth(130);
        cm.getColumn(6).setPreferredWidth(120);
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

        setorNasabahCombo = new JComboBox<String>();
        setorSampahCombo = new JComboBox<String>();
        beratField = new JTextField();

        gbc.gridwidth = 1;
        addRow(card, gbc, 1, "Nasabah", setorNasabahCombo);
        addRow(card, gbc, 2, "Jenis Sampah", setorSampahCombo);
        addRow(card, gbc, 3, "Berat (kg)", beratField);

        JButton setorBtn = primaryButton("Proses Setoran");
        setorBtn.addActionListener(e -> setorSampah());
        gbc.gridx = 1; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST;
        card.add(setorBtn, gbc);

        JLabel info = new JLabel("Nilai dihitung otomatis. Jenis & harga dikelola di tab 'Jenis Sampah'.");
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

        tarikNasabahCombo = new JComboBox<String>();
        tarikNasabahCombo.addActionListener(e -> updateTujuanTransferInfo());
        jumlahTarikField = new JTextField();
        tujuanTransferLabel = new JLabel("Pilih nasabah untuk melihat tujuan transfer.");
        tujuanTransferLabel.setForeground(new Color(40, 90, 50));
        tujuanTransferLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        gbc.gridwidth = 1;
        addRow(card, gbc, 1, "Nasabah", tarikNasabahCombo);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.anchor = GridBagConstraints.WEST;
        card.add(new JLabel("Tujuan Transfer"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        card.add(tujuanTransferLabel, gbc);

        addRow(card, gbc, 3, "Jumlah (Rp)", jumlahTarikField);

        JButton tarikBtn = primaryButton("Proses Transfer");
        tarikBtn.addActionListener(e -> tarikSaldo());
        gbc.gridx = 1; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        card.add(tarikBtn, gbc);

        JLabel info = new JLabel("Setelah berhasil, struk bukti transfer (.txt) disimpan otomatis di folder struk/.");
        info.setForeground(new Color(90, 100, 110));
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        card.add(info, gbc);

        panel.add(card, new GridBagConstraints());
        return panel;
    }

    private void updateTujuanTransferInfo() {
        if (tujuanTransferLabel == null) return;
        String nasabahId = selectedId(tarikNasabahCombo);
        if (nasabahId == null) {
            tujuanTransferLabel.setText("Pilih nasabah untuk melihat tujuan transfer.");
            return;
        }
        try {
            Nasabah n = bank.cariNasabah(nasabahId);
            tujuanTransferLabel.setText(n.getTujuanTransfer() + "  |  Saldo: " + Format.rupiah(n.getSaldo()));
        } catch (Exception e) {
            tujuanTransferLabel.setText("-");
        }
    }

    private JPanel createRiwayatPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setOpaque(false);
        top.add(new JLabel("Nasabah:"));
        riwayatNasabahCombo = new JComboBox<String>();
        riwayatNasabahCombo.addActionListener(e -> refreshRiwayat());
        top.add(riwayatNasabahCombo);
        top.add(new JLabel("Jenis:"));
        riwayatJenisCombo = new JComboBox<String>(new String[]{"Semua", "Setoran", "Pencairan/Transfer"});
        riwayatJenisCombo.addActionListener(e -> refreshRiwayat());
        top.add(riwayatJenisCombo);
        top.add(new JLabel("Dari (yyyy-MM-dd):"));
        riwayatDariField = new JTextField(8);
        top.add(riwayatDariField);
        top.add(new JLabel("Sampai:"));
        riwayatSampaiField = new JTextField(8);
        top.add(riwayatSampaiField);
        top.add(new JLabel("Cari:"));
        riwayatCariField = new JTextField(10);
        top.add(riwayatCariField);
        JButton refresh = new JButton("Terapkan Filter");
        refresh.addActionListener(e -> refreshRiwayat());
        top.add(refresh);
        JButton export = new JButton("Ekspor CSV");
        export.addActionListener(e -> exportRiwayat());
        top.add(export);

        riwayatTableModel = new DefaultTableModel(new Object[]{"ID/Ref", "Waktu", "Nasabah", "Tipe", "Deskripsi", "Jumlah"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(riwayatTableModel);
        styleTable(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumnModel cm = table.getColumnModel();
        if (cm.getColumnCount() >= 6) {
            cm.getColumn(0).setPreferredWidth(90);
            cm.getColumn(1).setPreferredWidth(140);
            cm.getColumn(2).setPreferredWidth(170);
            cm.getColumn(3).setPreferredWidth(120);
            cm.getColumn(4).setPreferredWidth(380);
            cm.getColumn(5).setPreferredWidth(120);
        }

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSampahPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Tambah / Ubah Jenis Sampah"));
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        namaSampahBaruField = new JTextField(14);
        hargaSampahField = new JTextField(10);
        kategoriSampahField = new JTextField(12);
        aturanSampahCombo = new JComboBox<String>(new String[]{"Normal", "Bonus 5%", "Bonus 5% (min 10kg)"});

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; form.add(new JLabel("Nama"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(namaSampahBaruField, gbc);
        gbc.gridx = 2; gbc.weightx = 0; form.add(new JLabel("Harga/kg (Rp)"), gbc);
        gbc.gridx = 3; gbc.weightx = 1; form.add(hargaSampahField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; form.add(new JLabel("Kategori"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(kategoriSampahField, gbc);
        gbc.gridx = 2; gbc.weightx = 0; form.add(new JLabel("Aturan nilai"), gbc);
        gbc.gridx = 3; gbc.weightx = 1; form.add(aturanSampahCombo, gbc);

        JButton tambah = primaryButton("Tambah Jenis");
        tambah.addActionListener(e -> tambahJenisSampah());
        JButton ubah = primaryButton("Ubah Harga (nama sama)");
        ubah.addActionListener(e -> ubahHargaSampah());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(tambah);
        btns.add(ubah);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4; gbc.anchor = GridBagConstraints.EAST;
        form.add(btns, gbc);

        sampahTableModel = new DefaultTableModel(new Object[]{"Nama", "Harga/kg", "Kategori", "Aturan"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(sampahTableModel);
        styleTable(table);

        panel.add(form, BorderLayout.NORTH);
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
        JButton export = new JButton("Ekspor Laporan CSV");
        export.addActionListener(e -> exportLaporan());
        top.add(refresh);
        top.add(export);

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
                    "Nasabah berhasil ditambahkan (ID " + n.getId() + ").\nTujuan: " + n.getTujuanTransfer() + "\n\nData tersimpan otomatis.",
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            namaField.setText(""); alamatField.setText(""); noHpField.setText("");
            bankEwalletField.setText(""); noRekeningField.setText("");
            refreshAll();
            setStatus("Nasabah " + n.getId() + " ditambahkan dan data disimpan.");
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
                    "Setoran berhasil!\nJenis: " + setoran.getSampah().getNama() + " (" + Format.angka(berat) + " kg)"
                            + "\nNilai: " + Format.rupiah(setoran.getJumlah())
                            + "\nSaldo sekarang: " + Format.rupiah(saldo),
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            beratField.setText("");
            refreshAll();
            setStatus("Setoran berhasil disimpan. Saldo " + nasabahId + " diperbarui.");
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
                    "Konfirmasi transfer digital:\n\nJumlah : " + Format.rupiah(jumlah)
                            + "\nTujuan : " + nasabah.getTujuanTransfer()
                            + "\nSaldo  : " + Format.rupiah(nasabah.getSaldo()),
                    "Konfirmasi Pencairan", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            Penarikan penarikan = bank.tarikSaldo(nasabahId, jumlah);
            double saldoSisa = bank.cariNasabah(nasabahId).getSaldo();
            Path struk = null;
            String strukInfo = "";
            try {
                struk = bank.buatStrukTransfer(nasabah, penarikan, saldoSisa);
                strukInfo = "\nStruk: " + struk.toString();
            } catch (Exception ex) {
                strukInfo = "\n(Struk gagal disimpan: " + ex.getMessage() + ")";
            }
            JOptionPane.showMessageDialog(this,
                    "Pencairan berhasil (transfer digital)!\nNo. Referensi: " + penarikan.getNoReferensi()
                            + "\nTujuan: " + nasabah.getTujuanTransfer()
                            + "\nJumlah: " + Format.rupiah(penarikan.getJumlah())
                            + "\nSaldo sisa: " + Format.rupiah(saldoSisa)
                            + strukInfo,
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            jumlahTarikField.setText("");
            refreshAll();
            setStatus("Transfer " + penarikan.getNoReferensi() + " berhasil, struk tersimpan.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void tambahJenisSampah() {
        try {
            String nama = namaSampahBaruField.getText();
            double harga = parseDouble(hargaSampahField.getText(), "Harga/kg");
            String kategori = kategoriSampahField.getText();
            String aturan = aturanFromCombo();
            Sampah s = bank.tambahJenisSampah(nama, harga, kategori, aturan);
            JOptionPane.showMessageDialog(this, "Jenis sampah '" + s.getNama() + "' ditambahkan (" + Format.rupiah(s.getHargaPerKg()) + "/kg).",
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            namaSampahBaruField.setText(""); hargaSampahField.setText(""); kategoriSampahField.setText("");
            aturanSampahCombo.setSelectedIndex(0);
            refreshAll();
            setStatus("Jenis sampah " + s.getNama() + " ditambahkan dan disimpan.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void ubahHargaSampah() {
        try {
            String nama = namaSampahBaruField.getText();
            double harga = parseDouble(hargaSampahField.getText(), "Harga/kg");
            bank.updateHargaSampah(nama, harga);
            JOptionPane.showMessageDialog(this, "Harga jenis sampah '" + nama.trim() + "' diperbarui ke " + Format.rupiah(harga) + "/kg.",
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
            setStatus("Harga sampah " + nama.trim() + " diperbarui.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private String aturanFromCombo() {
        int i = aturanSampahCombo == null ? 0 : aturanSampahCombo.getSelectedIndex();
        if (i == 1) return "BONUS5";
        if (i == 2) return "BONUS5_MIN10";
        return "NORMAL";
    }

    private void exportRiwayat() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("riwayat_bank_sampah.csv"));
        fc.setFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Path p = ensureCsv(fc.getSelectedFile().toPath());
                bank.exportRiwayatCsv(p);
                JOptionPane.showMessageDialog(this, "Riwayat diekspor ke:\n" + p.toAbsolutePath(), "Berhasil", JOptionPane.INFORMATION_MESSAGE);
                setStatus("Riwayat diekspor ke " + p.getFileName());
            } catch (Exception ex) {
                showError("Gagal ekspor riwayat: " + ex.getMessage());
            }
        }
    }

    private void exportLaporan() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("laporan_bank_sampah.csv"));
        fc.setFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Path p = ensureCsv(fc.getSelectedFile().toPath());
                bank.exportLaporanCsv(p);
                JOptionPane.showMessageDialog(this, "Laporan diekspor ke:\n" + p.toAbsolutePath(), "Berhasil", JOptionPane.INFORMATION_MESSAGE);
                setStatus("Laporan diekspor ke " + p.getFileName());
            } catch (Exception ex) {
                showError("Gagal ekspor laporan: " + ex.getMessage());
            }
        }
    }

    private Path ensureCsv(Path p) {
        String s = p.toString();
        if (!s.toLowerCase().endsWith(".csv")) return p.resolveSibling(p.getFileName().toString() + ".csv");
        return p;
    }

    private void refreshAll() {
        refreshNasabahTable();
        refreshNasabahCombos();
        refreshSampahUI();
        refreshRiwayat();
        refreshLaporan();
        refreshDashboard();
        updateTujuanTransferInfo();
    }

    private void refreshNasabahTable() {
        if (nasabahTableModel == null) return;
        nasabahTableModel.setRowCount(0);
        for (Nasabah n : bank.getDaftarNasabah()) {
            nasabahTableModel.addRow(new Object[]{n.getId(), n.getNama(), n.getAlamat(), n.getNoHp(),
                    n.getBankEwallet(), n.getNoRekening(), Format.rupiah(n.getSaldo())});
        }
    }

    private void refreshNasabahCombos() {
        String selSetor = selectedId(setorNasabahCombo);
        String selTarik = selectedId(tarikNasabahCombo);
        String selRiwayatRaw = riwayatNasabahCombo != null && riwayatNasabahCombo.getSelectedItem() != null
                ? riwayatNasabahCombo.getSelectedItem().toString() : null;
        updateCombo(setorNasabahCombo);
        updateCombo(tarikNasabahCombo);
        updateCombo(riwayatNasabahCombo);
        if (selSetor != null) selectId(setorNasabahCombo, selSetor);
        if (selTarik != null) selectId(tarikNasabahCombo, selTarik);
        if (riwayatNasabahCombo != null) {
            riwayatNasabahCombo.insertItemAt("Semua Nasabah", 0);
            if (selRiwayatRaw != null && !"Semua Nasabah".equals(selRiwayatRaw)) {
                String id = selectedIdFromText(selRiwayatRaw);
                if (id != null) selectId(riwayatNasabahCombo, id);
                if (riwayatNasabahCombo.getSelectedIndex() < 0) riwayatNasabahCombo.setSelectedIndex(0);
            } else {
                riwayatNasabahCombo.setSelectedIndex(0);
            }
        }
    }

    private void refreshSampahUI() {
        int selIdx = setorSampahCombo == null ? -1 : setorSampahCombo.getSelectedIndex();
        String selName = null;
        if (selIdx >= 0 && selIdx < bank.getDaftarSampah().size()) {
            selName = bank.getDaftarSampah().get(selIdx).getNama();
        }
        if (setorSampahCombo != null) {
            setorSampahCombo.removeAllItems();
            for (Sampah s : bank.getDaftarSampah()) {
                setorSampahCombo.addItem(s.getNama() + " (" + Format.rupiah(s.getHargaPerKg()) + "/kg)");
            }
            if (selName != null) {
                for (int i = 0; i < bank.getDaftarSampah().size(); i++) {
                    if (bank.getDaftarSampah().get(i).getNama().equalsIgnoreCase(selName)) {
                        setorSampahCombo.setSelectedIndex(i); break;
                    }
                }
            }
        }
        if (sampahTableModel != null) {
            sampahTableModel.setRowCount(0);
            for (Sampah s : bank.getDaftarSampah()) {
                sampahTableModel.addRow(new Object[]{s.getNama(), Format.rupiah(s.getHargaPerKg()), s.getKategori(), s.getAturanDeskripsi()});
            }
        }
    }

    private String selectedIdFromText(String itemText) {
        if (itemText == null) return null;
        int idx = itemText.indexOf(" - ");
        return idx > 0 ? itemText.substring(0, idx).trim() : itemText.trim();
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
        if (riwayatNasabahCombo == null || riwayatNasabahCombo.getSelectedItem() == null) return;

        String sel = riwayatNasabahCombo.getSelectedItem().toString();
        String jenisFilter = riwayatJenisCombo == null ? "Semua" : (String) riwayatJenisCombo.getSelectedItem();
        String cari = riwayatCariField == null ? "" : riwayatCariField.getText().trim().toLowerCase();
        LocalDate dari = parseDate(riwayatDariField == null ? null : riwayatDariField.getText());
        LocalDate sampai = parseDate(riwayatSampaiField == null ? null : riwayatSampaiField.getText());
        if (dari != null && sampai != null && dari.isAfter(sampai)) {
            showError("Tanggal 'Dari' tidak boleh setelah 'Sampai'.");
            return;
        }

        List<Object[]> rows = new ArrayList<Object[]>();
        List<Nasabah> sumber = new ArrayList<Nasabah>();
        if ("Semua Nasabah".equals(sel)) {
            sumber.addAll(bank.getDaftarNasabah());
        } else {
            String id = selectedId(riwayatNasabahCombo);
            if (id != null) {
                try { sumber.add(bank.cariNasabah(id)); } catch (Exception ignored) {}
            }
        }

        for (Nasabah n : sumber) {
            for (Transaksi t : n.getRiwayat()) {
                boolean isSetoran = t instanceof Setoran || t.getKategoriSampah() != null;
                String tipe = isSetoran ? "Setoran" : "Pencairan/Transfer";
                if ("Setoran".equals(jenisFilter) && !isSetoran) continue;
                if ("Pencairan/Transfer".equals(jenisFilter) && isSetoran) continue;
                LocalDate d = t.getWaktu().toLocalDate();
                if (dari != null && d.isBefore(dari)) continue;
                if (sampai != null && d.isAfter(sampai)) continue;
                String deskripsi = t.getDeskripsi();
                String nas = n.getId() + " - " + n.getNama();
                if (!cari.isEmpty()) {
                    String hay = (t.getId() + " " + nas + " " + deskripsi + " " + tipe).toLowerCase();
                    if (hay.indexOf(cari) < 0) continue;
                }
                rows.add(new Object[]{t.getId(), t.getWaktu(), t.getWaktuFormatted(), nas, tipe, deskripsi, Format.rupiah(t.getJumlah())});
            }
        }
        Collections.sort(rows, new Comparator<Object[]>() {
            public int compare(Object[] a, Object[] b) {
                java.time.LocalDateTime ta = (java.time.LocalDateTime) a[1];
                java.time.LocalDateTime tb = (java.time.LocalDateTime) b[1];
                return ta.compareTo(tb);
            }
        });
        for (Object[] r : rows) {
            riwayatTableModel.addRow(new Object[]{r[0], r[2], r[3], r[4], r[5], r[6]});
        }
    }

    private LocalDate parseDate(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.isEmpty()) return null;
        try {
            return LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    private void refreshLaporan() {
        if (laporanLabel == null) return;
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
        double totalSaldo = bank.getTotalSaldoBeredar();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:sans-serif;'>");
        sb.append("<h2>Laporan TrashChain</h2>");
        sb.append("<p style='color:#2e7d32'><b>Pencairan = transfer digital (bukan tunai) + No. Referensi + struk di folder struk/.</b></p>");
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
                sb.append("<li>").append(escape(e.getKey())).append(": ").append(Format.angka(e.getValue())).append(" kg</li>");
            }
            sb.append("</ul>");
        }
        sb.append("<p style='color:#555'>Data di <code>data/</code>. Gunakan tombol Ekspor CSV untuk laporan/riwayat.</p>");
        sb.append("</body></html>");
        laporanLabel.setText(sb.toString());
    }

    private void refreshDashboard() {
        if (dashNasabahVal == null) return;
        int jumlahSetoran = 0, jumlahCair = 0;
        double totalBerat = 0, totalNilaiSetoran = 0, totalCair = 0;
        for (Nasabah nasabah : bank.getDaftarNasabah()) {
            for (Transaksi t : nasabah.getRiwayat()) {
                if (t.getKategoriSampah() != null) {
                    jumlahSetoran++;
                    totalBerat += t.getBeratKg();
                    totalNilaiSetoran += t.getJumlah();
                } else {
                    jumlahCair++;
                    totalCair += t.getJumlah();
                }
            }
        }
        dashNasabahVal.setText(String.valueOf(bank.getDaftarNasabah().size()));
        dashSetorVal.setText(jumlahSetoran + " transaksi");
        dashCairVal.setText(jumlahCair + " transaksi");
        dashBeratVal.setText(Format.angka(totalBerat) + " kg");
        dashSaldoVal.setText(Format.rupiah(bank.getTotalSaldoBeredar()));
        dashSetoranVal.setText(Format.rupiah(totalNilaiSetoran));
        dashDicairkanVal.setText(Format.rupiah(totalCair));
    }

    private void setStatus(String text) {
        if (statusLabel != null) statusLabel.setText(text);
    }

    private void row(StringBuilder sb, String label, String value) {
        sb.append("<tr><td><b>").append(escape(label)).append("</b></td><td>: ").append(escape(value)).append("</td></tr>");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String selectedId(JComboBox<String> combo) {
        if (combo == null) return null;
        Object sel = combo.getSelectedItem();
        if (sel == null) return null;
        return selectedIdFromText(sel.toString());
    }

    private void selectId(JComboBox<String> combo, String id) {
        if (combo == null || id == null) return;
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i);
            if (item.startsWith(id + " - ")) { combo.setSelectedIndex(i); return; }
        }
    }

    private double parseDouble(String text, String label) {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException(label + " tidak boleh kosong.");
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " harus berupa angka.");
        }
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE;
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
        table.setFillsViewportHeight(true);
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
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ex) {}
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new BankSampahGUI().setVisible(true); }
        });
    }
}
