const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

const state = {
    page: 'dashboard',
    nasabah: [],
    sampah: [],
    riwayat: [],
    dashboard: null,
    laporan: null
};

const judul = {
    dashboard: ['Dashboard Utama', 'Ringkasan operasional dan aktivitas bank sampah.'],
    nasabah: ['Data Nasabah', 'Daftar nasabah terdaftar dan rekening tujuan transfer.'],
    setor: ['Setor Sampah', 'Pencatatan setoran sampah dengan kalkulasi otomatis.'],
    pencairan: ['Pencairan Saldo', 'Transfer saldo nasabah ke rekening atau e-wallet.'],
    riwayat: ['Riwayat Transaksi', 'Daftar seluruh transaksi setoran dan pencairan dana.'],
    jenis: ['Daftar Jenis Sampah', 'Katalog kategori sampah, tarif per kg, dan aturan bonus.'],
    laporan: ['Laporan Rekapitulasi', 'Ringkasan akumulasi setoran per kategori dan ekspor data.']
};

const esc = (value) => String(value == null ? '' : value).replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
}[c]));

const rupiah = (value) => new Intl.NumberFormat('id-ID', {
    style: 'currency', currency: 'IDR', maximumFractionDigits: 0
}).format(Number(value) || 0);

const angka = (value, digits) => new Intl.NumberFormat('id-ID', {
    minimumFractionDigits: digits == null ? 2 : digits,
    maximumFractionDigits: digits == null ? 2 : digits
}).format(Number(value) || 0);

async function api(path, options) {
    const response = await fetch(path, options);
    let data = null;
    try {
        data = await response.json();
    } catch (e) {
        data = null;
    }
    if (!response.ok) {
        throw new Error((data && data.error) || 'Permintaan gagal (' + response.status + ')');
    }
    return data;
}

function post(path, values) {
    return api(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: new URLSearchParams(values).toString()
    });
}

let toastTimer = null;
function toast(message, isError, titleOverride) {
    const el = $('#toast');
    if (!el) return;
    const badge = $('#toastBadge');
    const svg = $('#toastSvg');
    const title = $('#toastTitle');
    const msg = $('#toastMsg');

    if (badge && svg && title && msg) {
        badge.className = 'toast-badge' + (isError ? ' error' : '');
        svg.innerHTML = isError
            ? '<use href="#i-alert"/>'
            : '<use href="#i-check"/>';
        title.textContent = titleOverride || (isError ? 'Terjadi Kesalahan' : 'Berhasil');
        msg.textContent = message;
    }

    el.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.hidden = true; }, 5000);
}

function closeToast() {
    const el = $('#toast');
    if (el) el.hidden = true;
    clearTimeout(toastTimer);
}

const toastCloseBtn = $('#toastClose');
if (toastCloseBtn) toastCloseBtn.addEventListener('click', closeToast);

let modalConfirm = null;
let modalExtra = null;

function openModal(options) {
    const icon = options.icon || 'info';
    const iconMap = {
        success: '<use href="#i-check"/>',
        info:    '<use href="#i-alert"/>',
        leaf:    '<use href="#i-leaf"/>',
        receipt: '<use href="#i-receipt"/>',
        alert:   '<use href="#i-alert"/>'
    };

    const headerEl = $('#modalHeaderBlock');
    const iconEl   = $('#modalIcon');
    if (headerEl && iconEl) {
        iconEl.className = 'modal-badge-icon ' + icon;
        iconEl.innerHTML = '<svg style="width:22px;height:22px;">' + (iconMap[icon] || iconMap.info) + '</svg>';
    }

    if ($('#modalTitle')) $('#modalTitle').textContent = options.title;
    if ($('#modalSubtitle')) $('#modalSubtitle').textContent = options.subtitle || '';
    if ($('#modalBody')) $('#modalBody').innerHTML = options.body || '';

    const yesBtn = $('#modalYes');
    const noBtn  = $('#modalNo');
    const extBtn = $('#modalExtra');

    if (yesBtn) {
        yesBtn.textContent = options.confirmText || 'Ya, Lanjutkan';
        yesBtn.className   = 'btn ' + (options.confirmClass || 'btn-lime');
        yesBtn.classList.toggle('hidden', options.confirmText === false);
    }
    if (noBtn) {
        noBtn.textContent = options.cancelText || 'Batal';
        noBtn.classList.toggle('hidden', options.cancelText === false);
    }
    if (extBtn) {
        if (options.extraText) {
            extBtn.textContent   = options.extraText;
            extBtn.className     = 'btn ' + (options.extraClass || 'btn-ghost');
            extBtn.classList.remove('hidden');
        } else {
            extBtn.classList.add('hidden');
        }
    }

    modalConfirm = options.onConfirm || null;
    modalExtra   = options.onExtra   || null;
    $('#modal').hidden = false;
}

function closeModal() {
    $('#modal').hidden = true;
    modalConfirm = null;
    modalExtra   = null;
}

$('#modalClose').addEventListener('click', closeModal);
$('#modal').addEventListener('click', (e) => { if (e.target === $('#modal')) closeModal(); });
$('#modalNo').addEventListener('click', closeModal);
$('#modalExtra').addEventListener('click', () => {
    const action = modalExtra;
    closeModal();
    if (action) Promise.resolve().then(action).catch((e) => toast(e.message, true));
});
$('#modalYes').addEventListener('click', () => {
    const action = modalConfirm;
    closeModal();
    if (action) Promise.resolve().then(action).catch((e) => toast(e.message, true));
});

function buildDetailRow(label, value, highlight) {
    return '<div class="detail-row' + (highlight ? ' highlight-' + highlight : '') + '">' +
        '<span class="detail-label">' + esc(label) + '</span>' +
        '<span class="detail-value">' + value + '</span></div>';
}

function buildResultCard(rows) {
    return '<div class="result-card"><div class="result-details">' +
        rows.map((r) => buildDetailRow(r[0], typeof r[1] === 'string' ? esc(r[1]) : r[1], r[2])).join('') +
        '</div></div>';
}

function buildReceiptCard(ref, waktu, nasabah, tujuan, jumlah, saldoSisa, refUrl) {
    return '<div class="digital-receipt-modal">' +
        '<div class="receipt-modal-head">' +
        '<div class="receipt-modal-badge"><svg style="width:14px;height:14px;"><use href="#i-check"/></svg>Transfer Berhasil</div>' +
        '<div class="receipt-modal-label">Jumlah Dicairkan</div>' +
        '<div class="receipt-modal-amount">' + esc(jumlah) + '</div>' +
        '</div>' +
        '<div class="receipt-modal-tear"></div>' +
        '<div class="receipt-modal-grid">' +
        '<div class="receipt-modal-row"><span class="receipt-modal-row-label">No. Referensi</span>' +
        '<span class="receipt-modal-row-val"><span class="badge-code">' + esc(ref) + '</span></span></div>' +
        '<div class="receipt-modal-row"><span class="receipt-modal-row-label">Waktu</span>' +
        '<span class="receipt-modal-row-val">' + esc(waktu || '-') + '</span></div>' +
        '<div class="receipt-modal-row"><span class="receipt-modal-row-label">Nasabah</span>' +
        '<span class="receipt-modal-row-val">' + esc(nasabah) + '</span></div>' +
        '<div class="receipt-modal-row"><span class="receipt-modal-row-label">Rekening / E-Wallet</span>' +
        '<span class="receipt-modal-row-val">' + esc(tujuan) + '</span></div>' +
        '<div class="receipt-modal-row"><span class="receipt-modal-row-label">Sisa Saldo</span>' +
        '<span class="receipt-modal-row-val" style="color:#059669;font-size:14px;">' + esc(saldoSisa) + '</span></div>' +
        '</div>' +
        '<div class="receipt-modal-foot">Dokumen digital ini diterbitkan oleh sistem TrashChain dan berlaku sebagai bukti transaksi resmi.</div>' +
        '</div>';
}

function go(page) {
    if (!judul[page]) return;
    state.page = page;
    $$('.nav-item').forEach((item) => item.classList.toggle('active', item.dataset.page === page));
    $$('.page').forEach((section) => section.classList.toggle('active', section.id === 'page-' + page));
    if ($('#pageTitle')) $('#pageTitle').textContent = judul[page][0];
    if ($('#pageSubtitle')) $('#pageSubtitle').textContent = judul[page][1];
    if ($('#pageEyebrow')) $('#pageEyebrow').textContent = (page === 'dashboard' ? 'DASHBOARD' : judul[page][0]).toUpperCase();
    loadPage(page);

    if (page !== 'dashboard') {
        const target = $('#mainContent');
        if (target) {
            const rect = target.getBoundingClientRect();
            if (rect.top < 0 || rect.top > 400) {
                target.scrollIntoView({ behavior: 'smooth' });
            }
        }
    }
}

async function loadPage(page) {
    try {
        if (page === 'dashboard') {
            state.dashboard = await api('/api/dashboard');
            renderDashboard(state.dashboard);
        } else if (page === 'nasabah') {
            state.nasabah = await api('/api/nasabah');
            renderNasabah();
        } else if (page === 'setor') {
            const [nasabah, sampah] = await Promise.all([api('/api/nasabah'), api('/api/sampah')]);
            state.nasabah = nasabah;
            state.sampah = sampah;
            isiSelect($('#setorNasabah'), nasabah, (n) => n.id + ' · ' + n.nama, (n) => n.id, 'Pilih nasabah');
            isiSelect($('#setorSampah'), sampah, (s) => s.nama + ' (' + rupiah(s.hargaPerKg) + '/kg)', (s) => s.nama, 'Pilih jenis sampah');
            updateEstimate();
        } else if (page === 'pencairan') {
            state.nasabah = await api('/api/nasabah');
            isiSelect($('#tarikNasabah'), state.nasabah, (n) => n.id + ' · ' + n.nama, (n) => n.id, 'Pilih nasabah');
            updateSaldo();
        } else if (page === 'riwayat') {
            await loadRiwayat();
        } else if (page === 'jenis') {
            state.sampah = await api('/api/sampah');
            renderJenis();
        } else if (page === 'laporan') {
            state.laporan = await api('/api/laporan');
            renderLaporan(state.laporan);
        }
    } catch (e) {
        toast(e.message, true);
    }
}

function isiSelect(select, items, label, value, placeholder) {
    if (!select) return;
    const sebelumnya = select.value;
    select.innerHTML = '<option value="">' + esc(placeholder) + '</option>' + items.map((item) =>
        '<option value="' + esc(value(item)) + '">' + esc(label(item)) + '</option>').join('');
    if (sebelumnya) select.value = sebelumnya;
}

function table(element, headers, rows) {
    if (!element) return;
    element.innerHTML = '<thead><tr>' + headers.map((h) => '<th>' + esc(h) + '</th>').join('') +
        '</tr></thead><tbody>' + (rows.length ? rows.join('') :
        '<tr><td class="empty" colspan="' + headers.length + '">Belum ada data tersedia.</td></tr>') + '</tbody>';
}

function metric(label, value, note) {
    return '<div class="metric"><span>' + esc(label) + '</span><strong>' +
        esc(value) + '</strong><small>' + esc(note) + '</small></div>';
}

function summaryRow(label, value, note, icon, tone) {
    return '<div class="summary-row"><span class="summary-ico ' + tone + '"><svg class="icon"><use href="#i-' + icon + '"/></svg></span>' +
        '<div><span>' + esc(label) + '</span><small>' + esc(note) + '</small></div>' +
        '<strong class="summary-value">' + esc(value) + '</strong></div>';
}

function grafikAktivitas(items) {
    const recent = (items || []).slice(0, 12).reverse();
    const basis = recent.map((t) => Math.abs(Number(t.jumlah) || 0));
    const values = [];
    for (let i = 0; i < 12; i++) {
        values.push(basis.length ? basis[i % basis.length] : 45 + ((i * 37) % 75));
    }
    const maks = Math.max.apply(null, values) || 1;
    const points = values.map((v, i) => ({
        x: 12 + i * (576 / 11),
        y: 125 - (v / maks) * 98
    }));
    const garis = points.map((p, i) => (i ? 'L' : 'M') + p.x.toFixed(1) + ' ' + p.y.toFixed(1)).join(' ');
    const area = garis + ' L588 138 L12 138 Z';
    const titikAkhir = points[points.length - 1];
    return '<svg viewBox="0 0 600 145" preserveAspectRatio="none" role="img">' +
        '<defs><linearGradient id="chartGlow" x1="0" y1="0" x2="0" y2="1">' +
        '<stop offset="0" stop-color="#0284c7" stop-opacity="0.28"/>' +
        '<stop offset="1" stop-color="#0284c7" stop-opacity="0"/></linearGradient></defs>' +
        [25, 65, 105].map((y) => '<line x1="10" y1="' + y + '" x2="590" y2="' + y + '" stroke="#edf2f7" stroke-width="1"/>').join('') +
        '<path d="' + area + '" fill="url(#chartGlow)"/>' +
        '<path d="' + garis + '" fill="none" stroke="#0284c7" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" vector-effect="non-scaling-stroke"/>' +
        '<circle cx="' + titikAkhir.x.toFixed(1) + '" cy="' + titikAkhir.y.toFixed(1) + '" r="5" fill="#0284c7" stroke="#ffffff" stroke-width="2.5" vector-effect="non-scaling-stroke"/>' +
        '</svg>';
}

function renderDashboard(d) {
    if ($('#balanceAmount')) $('#balanceAmount').textContent = rupiah(d.saldoBeredar);
    if ($('#trendIn')) $('#trendIn').textContent = rupiah(d.totalNilaiSetoran);
    if ($('#trendOut')) $('#trendOut').textContent = rupiah(d.totalDanaDicairkan);
    if ($('#navNasabahCount')) $('#navNasabahCount').textContent = d.jumlahNasabah;
    if ($('#balanceChart')) $('#balanceChart').innerHTML = grafikAktivitas(d.terbaru);

    // Live update Sky Hero showcase cards
    if ($('#heroStatSaldo')) $('#heroStatSaldo').textContent = rupiah(d.saldoBeredar);
    if ($('#heroStatBerat')) $('#heroStatBerat').textContent = angka(d.totalBerat, 1) + ' kg';
    if ($('#heroStatNasabah')) $('#heroStatNasabah').textContent = d.jumlahNasabah + ' Nasabah';
    if ($('#heroStatSetoran')) $('#heroStatSetoran').textContent = d.jumlahSetoran + ' Transaksi';
    if ($('#bentoStatBerat')) $('#bentoStatBerat').textContent = angka(d.totalBerat, 1) + ' kg';

    if ($('#summaryList')) {
        $('#summaryList').innerHTML = [
            summaryRow('Saldo Nasabah', rupiah(d.saldoBeredar), d.jumlahNasabah + ' nasabah aktif', 'wallet', 'blue'),
            summaryRow('Nilai Setoran', rupiah(d.totalNilaiSetoran), d.jumlahSetoran + ' setoran tercatat', 'leaf', 'green'),
            summaryRow('Dana Dicairkan', rupiah(d.totalDanaDicairkan), d.jumlahPencairan + ' transfer berhasil', 'out', 'rose'),
            summaryRow('Sampah Terkelola', angka(d.totalBerat, 2) + ' kg', 'seluruh kategori aktif', 'grid', 'lime')
        ].join('');
    }

    if ($('#dashboardMetrics')) {
        $('#dashboardMetrics').innerHTML = [
            metric('Total Nasabah', d.jumlahNasabah, 'Nasabah aktif'),
            metric('Total Setoran', d.jumlahSetoran, 'Transaksi masuk'),
            metric('Berat Terkelola', angka(d.totalBerat, 2) + ' kg', 'Akumulasi timbangan'),
            metric('Nilai Setoran', rupiah(d.totalNilaiSetoran), 'Konversi saldo'),
            metric('Dana Dicairkan', rupiah(d.totalDanaDicairkan), d.jumlahPencairan + ' transfer'),
            metric('Saldo Beredar', rupiah(d.saldoBeredar), 'Hak saldo nasabah')
        ].join('');
    }

    if ($('#recentTable')) {
        table($('#recentTable'), ['Waktu', 'Nasabah', 'Transaksi', 'Jumlah'], d.terbaru.map((t) =>
            '<tr><td class="muted">' + esc(t.waktu) + '</td><td><strong>' + esc(t.namaNasabah) + '</strong></td><td>' +
            '<span class="tag ' + (t.tipe === 'Setoran' ? 'in' : 'out') + '">' + esc(t.tipe) + '</span>' +
            '<div class="muted" style="margin-top:3px;font-size:11px">' + esc(t.deskripsi) + '</div></td>' +
            '<td class="num font-bold">' + rupiah(t.jumlah) + '</td></tr>'));
    }

    const maks = d.kategori.reduce((m, k) => Math.max(m, Number(k.berat) || 0), 0) || 1;
    const warna = ['lime', 'blue', 'dark'];
    if ($('#categoryBars')) {
        $('#categoryBars').innerHTML = d.kategori.length ? d.kategori.map((k, i) =>
            '<div class="bar-row"><div class="bar-label"><span>' + esc(k.nama) + '</span><strong>' +
            angka(k.berat, 2) + ' kg</strong></div><div class="bar-track"><div class="bar-fill ' +
            warna[i % warna.length] + '" style="width:' + Math.max(4, (Number(k.berat) / maks) * 100) +
            '%"></div></div></div>').join('') : '<p class="muted" style="text-align:center;padding:16px;">Belum ada setoran sampah tercatat.</p>';
    }
}

function renderNasabah() {
    const cari = $('#searchNasabah') ? $('#searchNasabah').value.trim().toLowerCase() : '';
    const filtered = state.nasabah.filter((n) => !cari ||
        (n.nama + ' ' + n.id + ' ' + n.bankEwallet + ' ' + (n.alamat || '')).toLowerCase().includes(cari));
    const rows = filtered.map((n) =>
        '<tr><td><span class="badge-code">' + esc(n.id) + '</span></td><td><strong>' + esc(n.nama) + '</strong>' +
        '<div class="muted" style="margin-top:2px;font-size:11px">' + esc(n.alamat || '-') + '</div></td><td>' +
        '<span class="bank-pill">' + esc(n.tujuanTransfer) + '</span></td><td class="num font-bold">' + rupiah(n.saldo) + '</td><td class="num muted">' +
        n.jumlahTransaksi + '</td></tr>');
    table($('#nasabahTable'), ['ID Nasabah', 'Nama Nasabah', 'Tujuan Transfer', 'Saldo Saat Ini', 'Transaksi'], rows);
}

function updateEstimate() {
    const sampah = state.sampah.find((s) => s.nama === $('#setorSampah').value);
    const berat = Number($('#setorBerat').value) || 0;
    if (!sampah || berat <= 0) {
        $('#setorEstimate').textContent = 'Rp0';
        $('#setorRule').textContent = sampah ? sampah.aturanLabel : 'Pilih jenis sampah dan masukkan berat timbangan';
        return;
    }
    let nilai = berat * Number(sampah.hargaPerKg);
    if (sampah.aturan === 'BONUS5') nilai *= 1.05;
    else if (sampah.aturan === 'BONUS5_MIN10' && berat >= 10) nilai *= 1.05;
    else if (sampah.aturan === 'BONUS5_MIN5' && berat >= 5) nilai *= 1.05;
    $('#setorEstimate').textContent = rupiah(nilai);
    $('#setorRule').textContent = sampah.aturanLabel + ' · Tarif ' + rupiah(sampah.hargaPerKg) + '/kg';
}

function updateSaldo() {
    const n = state.nasabah.find((item) => item.id === $('#tarikNasabah').value);
    $('#tarikSaldo').textContent = n ? rupiah(n.saldo) : 'Rp0';
}

async function loadRiwayat() {
    const query = new URLSearchParams({
        idNasabah: $('#filterNasabah') ? $('#filterNasabah').value : '',
        jenis: $('#filterJenis') ? $('#filterJenis').value : '',
        dari: $('#filterDari') ? $('#filterDari').value : '',
        sampai: $('#filterSampai') ? $('#filterSampai').value : '',
        q: $('#filterCari') ? $('#filterCari').value : ''
    });
    state.riwayat = await api('/api/riwayat?' + query.toString());

    const nasabah = await api('/api/nasabah');
    state.nasabah = nasabah;
    isiSelect($('#filterNasabah'), nasabah, (n) => n.id + ' · ' + n.nama, (n) => n.id, 'Semua Nasabah');

    table($('#riwayatTable'), ['Waktu', 'ID / Ref', 'Nasabah', 'Tipe', 'Deskripsi', 'Jumlah', 'Struk Bukti'],
        state.riwayat.map((t) =>
            '<tr><td class="muted">' + esc(t.waktu) + '</td><td><span class="badge-code">' + esc(t.id) + '</span></td><td><strong>' + esc(t.namaNasabah) +
            '</strong></td><td><span class="tag ' + (t.tipe === 'Setoran' ? 'in' : 'out') + '">' + esc(t.tipe) +
            '</span></td><td class="muted">' + esc(t.deskripsi) + '</td><td class="num font-bold">' + rupiah(t.jumlah) +
            '</td><td>' + (t.struk ? '<a class="link-btn" href="' + esc(t.struk) + '" target="_blank" rel="noopener">Unduh Struk</a>' : '-') +
            '</td></tr>'));
}

function renderJenis() {
    table($('#jenisTable'), ['Kategori / Nama', 'Harga per Kg', 'Kelompok', 'Aturan Perhitungan'], state.sampah.map((s) =>
        '<tr><td><strong>' + esc(s.nama) + '</strong></td><td class="num font-bold">' + rupiah(s.hargaPerKg) +
        '</td><td class="muted">' + esc(s.kategori) + '</td><td><span class="tag neutral">' +
        esc(s.aturanLabel) + '</span></td></tr>'));
    isiSelect($('#hargaSampah'), state.sampah, (s) => s.nama + ' (' + rupiah(s.hargaPerKg) + '/kg)',
        (s) => s.nama, 'Pilih jenis sampah');
}

function renderLaporan(l) {
    if ($('#laporanMetrics')) {
        $('#laporanMetrics').innerHTML = [
            metric('Nilai Setoran', rupiah(l.totalNilaiSetoran), l.jumlahSetoran + ' transaksi setoran'),
            metric('Dana Dicairkan', rupiah(l.totalDanaDicairkan), l.jumlahPencairan + ' transfer dicairkan'),
            metric('Saldo Beredar', rupiah(l.saldoBeredar), l.jumlahNasabah + ' nasabah aktif'),
            metric('Total Berat', angka(l.totalBerat, 2) + ' kg', 'Akumulasi sampah')
        ].join('');
    }
    table($('#laporanTable'), ['Kategori Sampah', 'Total Berat (kg)', 'Total Nilai (Rp)'], l.kategori.map((k) =>
        '<tr><td><strong>' + esc(k.nama) + '</strong></td><td class="num">' + angka(k.berat, 2) + ' kg' +
        '</td><td class="num font-bold">' + rupiah(k.nilai) + '</td></tr>'));
}

// Navigation Handlers
$$('.nav-item').forEach((item) => item.addEventListener('click', () => go(item.dataset.page)));
$$('[data-go]').forEach((item) => item.addEventListener('click', () => go(item.dataset.go)));
$('#refreshBtn').addEventListener('click', () => loadPage(state.page));

if ($('#todayChip')) {
    $('#todayChip').textContent = new Intl.DateTimeFormat('id-ID', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    }).format(new Date());
}

// Quick Search & Shortcut (Ctrl/Cmd + K)
const pintasan = {
    dashboard: 'dashboard', beranda: 'dashboard', home: 'dashboard',
    nasabah: 'nasabah', user: 'nasabah', warga: 'nasabah',
    setor: 'setor', setoran: 'setor', sampah: 'setor',
    pencairan: 'pencairan', cair: 'pencairan', transfer: 'pencairan', tarik: 'pencairan',
    riwayat: 'riwayat', history: 'riwayat', transaksi: 'riwayat',
    jenis: 'jenis', harga: 'jenis', tarif: 'jenis',
    laporan: 'laporan', lapor: 'laporan', rekap: 'laporan'
};

$('#globalSearch').addEventListener('keydown', (event) => {
    if (event.key !== 'Enter') return;
    const cari = event.target.value.trim().toLowerCase();
    if (!cari) return;
    const cocok = Object.keys(pintasan).find((kata) => kata.includes(cari));
    if (cocok) {
        go(pintasan[cocok]);
        event.target.value = '';
    } else {
        toast('Menu tidak ditemukan. Coba: nasabah, setor, riwayat, laporan.', true);
    }
});

document.addEventListener('keydown', (event) => {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        $('#globalSearch').focus();
    }
});

// Nasabah Form Handlers
$('#toggleNasabahForm').addEventListener('click', () => $('#formNasabah').classList.toggle('hidden'));
$$('[data-close]').forEach((button) => button.addEventListener('click', () =>
    $('#' + button.dataset.close).classList.add('hidden')));
$('#searchNasabah').addEventListener('input', renderNasabah);

$('#formNasabah').addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
        await post('/api/nasabah', Object.fromEntries(new FormData(event.target)));
        event.target.reset();
        event.target.classList.add('hidden');
        toast('Data nasabah berhasil ditambahkan.');
        loadPage('nasabah');
    } catch (e) {
        toast(e.message, true);
    }
});

// Setoran Handlers
$('#setorSampah').addEventListener('change', updateEstimate);
$('#setorBerat').addEventListener('input', updateEstimate);

$('#formSetor').addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
        const hasil = await post('/api/setoran', Object.fromEntries(new FormData(event.target)));
        $('#setorBerat').value = '';
        updateEstimate();
        toast('Setoran ' + hasil.nama + ' berhasil dicatat (' + rupiah(hasil.jumlah) + '). Saldo baru: ' + rupiah(hasil.saldo) + '.');
        loadPage('setor');
    } catch (e) {
        toast(e.message, true);
    }
});

// Pencairan Saldo Handlers
$('#tarikNasabah').addEventListener('change', updateSaldo);

$('#formPencairan').addEventListener('submit', (event) => {
    event.preventDefault();
    const id = $('#tarikNasabah').value;
    const jumlah = Number($('#tarikJumlah').value);
    const nasabah = state.nasabah.find((n) => n.id === id);
    if (!nasabah) { toast('Silakan pilih nasabah terlebih dahulu.', true); return; }
    if (!(jumlah > 0)) { toast('Jumlah pencairan harus lebih dari 0.', true); return; }
    if (jumlah > nasabah.saldo) { toast('Saldo nasabah tidak mencukupi.', true); return; }

    openModal({
        title: 'Konfirmasi Pencairan Dana',
        body: '<p>Transferkan saldo sebesar <strong>' + rupiah(jumlah) + '</strong> ke rekening:<br><strong>' + esc(nasabah.tujuanTransfer) + '</strong> (' + esc(nasabah.nama) + ')?</p>',
        confirmText: 'Ya, Proses Transfer',
        onConfirm: async () => {
            try {
                const hasil = await post('/api/pencairan', { idNasabah: id, jumlah: jumlah });
                $('#tarikJumlah').value = '';
                const bukti = hasil.struk ? '<p style="margin-top:12px;">Struk Bukti: <a href="' + esc(hasil.struk) + '" target="_blank" rel="noopener">Unduh File Struk Transfer (.txt)</a></p>' : '';
                openModal({
                    title: 'Pencairan Berhasil Diproses',
                    body: '<p>Nomor Referensi: <strong>' + esc(hasil.ref) + '</strong><br>Nominal: ' + rupiah(hasil.jumlah) +
                        '<br>Tujuan: ' + esc(hasil.tujuan) + '<br>Sisa Saldo Nasabah: ' + rupiah(hasil.saldoSisa) + '</p>' + bukti,
                    confirmText: 'Selesai',
                    cancelText: false
                });
                loadPage('pencairan');
            } catch (e) {
                toast(e.message, true);
            }
        }
    });
});

// Riwayat Filters
['filterNasabah', 'filterJenis', 'filterDari', 'filterSampai'].forEach((id) => {
    const el = $('#' + id);
    if (el) el.addEventListener('change', () => loadRiwayat().catch((e) => toast(e.message, true)));
});
if ($('#filterCari')) {
    $('#filterCari').addEventListener('input', () => loadRiwayat().catch((e) => toast(e.message, true)));
}

// Jenis Sampah Handlers
$('#formJenis').addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
        await post('/api/sampah', Object.fromEntries(new FormData(event.target)));
        event.target.reset();
        toast('Jenis sampah berhasil ditambahkan.');
        loadPage('jenis');
    } catch (e) {
        toast(e.message, true);
    }
});

$('#formHarga').addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
        await post('/api/sampah/harga', Object.fromEntries(new FormData(event.target)));
        event.target.reset();
        toast('Tarif jenis sampah berhasil diperbarui.');
        loadPage('jenis');
    } catch (e) {
        toast(e.message, true);
    }
});

// CSV Exports
$('#exportRiwayat').addEventListener('click', () => { window.location.href = '/api/export/riwayat'; });
$('#exportLaporan').addEventListener('click', () => { window.location.href = '/api/export/laporan'; });

// Start directly on Dashboard
go('dashboard');
