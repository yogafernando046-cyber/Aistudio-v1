const fs = require('fs');

console.log('Injecting AI Assistant, Permission Helper, and Print Enhancements into index.html...');
let html = fs.readFileSync('app/src/main/assets/www/index.html', 'utf8');

// 1. Add bypass button to login card
const oldLoginCard = '<button class="primary" style="width:100%" onclick="doLogin()">Masuk Aplikasi</button>';
const newLoginCard = '<button class="primary" style="width:100%" onclick="doLogin()">Masuk Aplikasi</button><button type="button" style="width:100%;margin-top:8px;border:1px dashed var(--border-color);border-radius:11px;padding:8px;background:transparent;color:var(--text-color);font-size:12px;font-weight:600;" onclick="bypassLoginForPreview()">👁️ Buka Langsung (Mode Pratinjau Cepat)</button>';
if (html.includes(oldLoginCard)) {
  html = html.replace(oldLoginCard, newLoginCard);
  console.log('Added bypassLoginForPreview button.');
}

// 2. Add AI Assistant to Nav bar
const oldNav = `<button onclick="tab('laporan',this)">▤ Laporan</button></nav>`;
const newNav = `<button onclick="tab('laporan',this)">▤ Laporan</button><button onclick="tab('ai-assistant',this)" style="color:#c084fc;font-weight:700;">🤖 Asisten AI</button></nav>`;
if (html.includes(oldNav)) {
  html = html.replace(oldNav, newNav);
  console.log('Added AI Assistant to nav bar.');
}

// 3. Add Permission Card and AI Shortcut to Dashboard
const oldDashHero = `<div class="dash-grid">`;
const newDashAdditions = `
<div class="card" style="border-left:4px solid #3b82f6;background:linear-gradient(135deg,rgba(59,130,246,0.08),transparent);margin-top:12px;">
  <div class="row between">
    <b>🛡️ Bantuan Izin Perangkat (Kamera Barcode & Bluetooth Printer)</b>
    <button class="primary" style="padding:6px 12px;font-size:12px;" onclick="triggerAllPermissions()">Aktifkan Izin</button>
  </div>
  <div class="muted" style="margin:6px 0 10px;">Izin Kamera diperlukan untuk barcode scanner dan izin Bluetooth untuk cetak struk thermal.</div>
  <div class="row" style="gap:8px;flex-wrap:wrap;">
    <button class="ghost" style="padding:6px 10px;font-size:11px;" onclick="triggerAllPermissions()">📷 Minta Izin Kamera & BT</button>
    <button class="ghost" style="padding:6px 10px;font-size:11px;" onclick="openAndroidAppSettings()">⚙️ Buka Pengaturan Izin Android</button>
  </div>
</div>

<div class="card" style="border-left:4px solid #a855f7;background:linear-gradient(135deg,rgba(168,85,247,0.08),transparent);margin-top:12px;">
  <div class="row between">
    <b style="color:#c084fc;">🤖 Asisten AI Toko Idris (Gemini 3.5 Flash)</b>
    <button class="primary" style="background:#7c3aed;padding:6px 12px;font-size:12px;" onclick="tab('ai-assistant',document.querySelectorAll('#nav button')[6]||document.querySelectorAll('#nav button')[5])">Buka Chat AI</button>
  </div>
  <div class="muted" style="margin:6px 0 10px;">Analisis cerdas penjualan hari ini, rekomendasi restock barang, dan strategi omzet toko.</div>
  <div class="row" style="gap:6px;flex-wrap:wrap;">
    <button class="ghost" style="padding:6px 10px;font-size:11px;" onclick="tab('ai-assistant',document.querySelectorAll('#nav button')[6]||document.querySelectorAll('#nav button')[5]);askAiPrompt('Analisis performa penjualan dan laba hari ini.')">📊 Analisis Hari Ini</button>
    <button class="ghost" style="padding:6px 10px;font-size:11px;" onclick="tab('ai-assistant',document.querySelectorAll('#nav button')[6]||document.querySelectorAll('#nav button')[5]);askAiPrompt('Produk apa yang stoknya menipis dan perlu segera kulakan?')">⚠️ Cek Stok Menipis</button>
    <button class="ghost" style="padding:6px 10px;font-size:11px;" onclick="tab('ai-assistant',document.querySelectorAll('#nav button')[6]||document.querySelectorAll('#nav button')[5]);askAiPrompt('Beri 3 ide promo diskon bundle untuk menarik pelanggan.')">💡 Ide Promo Toko</button>
  </div>
</div>
<div class="dash-grid">
`;
if (html.includes(oldDashHero)) {
  html = html.replace(oldDashHero, newDashAdditions);
  console.log('Added permission card and AI card to dashboard.');
}

// 4. Add Section AI Assistant before receipt modal
const oldMainClose = `</main>`;
const newAiSection = `
<section id="ai-assistant" class="hidden">
  <div class="card" style="border-left:4px solid #a855f7;">
    <div class="row between">
      <div>
        <b style="color:#c084fc;font-size:16px;">🤖 Asisten AI Toko Idris (Didukung Gemini 3.5 Flash)</b>
        <div class="muted">Tanyakan analisis penjualan, strategi harga, dan optimasi keuntungan toko Anda.</div>
      </div>
      <button class="ghost" style="padding:6px 10px;font-size:11px;" onclick="clearAiChat()">Hapus Chat</button>
    </div>
    <div style="margin-top:10px;">
      <div class="muted" style="margin-bottom:6px;">Pilihan Pertanyaan Cepat:</div>
      <div style="display:flex;gap:6px;overflow-x:auto;padding-bottom:8px;">
        <button class="ghost" style="white-space:nowrap;padding:6px 10px;font-size:11px;" onclick="askAiPrompt('Analisis performa penjualan, laba kotor, dan laba bersih toko saya hari ini.')">📊 Analisis Finansial</button>
        <button class="ghost" style="white-space:nowrap;padding:6px 10px;font-size:11px;" onclick="askAiPrompt('Periksa daftar produk yang stoknya menipis dan berikan rekomendasi pembelian stok.')">⚠️ Saran Restock</button>
        <button class="ghost" style="white-space:nowrap;padding:6px 10px;font-size:11px;" onclick="askAiPrompt('Beri ide promo akhir pekan yang menarik untuk toko kelontong/sembako.')">💡 Promo Akhir Pekan</button>
        <button class="ghost" style="white-space:nowrap;padding:6px 10px;font-size:11px;" onclick="askAiPrompt('Bagaimana cara menagih kasbon/piutang pelanggan secara sopan dan efektif?')">💰 Tips Kasbon</button>
      </div>
    </div>
    <div id="aiChatBox" style="background:var(--stat-bg);border:1px solid var(--border-color);border-radius:12px;padding:12px;min-height:220px;max-height:360px;overflow-y:auto;display:flex;flex-direction:column;gap:10px;margin:12px 0;">
      <div style="background:var(--card-bg);border:1px solid var(--border-color);border-left:3px solid #a855f7;padding:10px 14px;border-radius:12px;max-width:90%;">
        <b>Halo, saya Asisten AI Toko Idris! 👋</b><br>
        Saya siap membantu Anda menganalisis omzet, laba bersih, stok produk, dan kasbon toko Anda hari ini secara real-time. Apa yang ingin Anda tanyakan?
      </div>
    </div>
    <div class="row">
      <input id="aiInputPrompt" placeholder="Ketik pertanyaan untuk Asisten AI... [Enter]" onkeydown="if(event.key==='Enter')submitAiPrompt()">
      <button class="primary" style="background:#7c3aed;padding:10px 16px;white-space:nowrap;" onclick="submitAiPrompt()">Kirim 🚀</button>
    </div>
  </div>
</section>
</main>
`;
if (html.includes(oldMainClose)) {
  html = html.replace(oldMainClose, newAiSection);
  console.log('Added AI Assistant section.');
}

// 5. Connect openReceiptPrintDialog to Android Native Bridge
const oldPrintFn = `async function openReceiptPrintDialog(jobName){`;
const newPrintFn = `async function openReceiptPrintDialog(jobName){
  try {
    if (window.AndroidNativeBridge && window.AndroidNativeBridge.printReceipt) {
      window.AndroidNativeBridge.printReceipt(jobName || 'Struk Kasir Idris');
      return;
    }
    if (window.AndroidPrint && window.AndroidPrint.printReceipt) {
      window.AndroidPrint.printReceipt(jobName || 'Struk Kasir Idris');
      return;
    }
  } catch(e) { console.warn("Native bridge print fallback:", e); }
`;
if (html.includes(oldPrintFn)) {
  html = html.replace(oldPrintFn, newPrintFn);
  console.log('Connected receipt print dialog to Android Native Bridge.');
}

// 6. Append helper functions before closing script
const oldScriptEnd = `</script>\n<div id="receiptPreviewModal"`;
const helperJs = `
function bypassLoginForPreview() {
  const overlay = document.getElementById('loginOverlay');
  if (overlay) {
    overlay.classList.add('hidden');
    overlay.style.display = 'none';
  }
}

function triggerAllPermissions() {
  if (window.AndroidNativeBridge && window.AndroidNativeBridge.requestAllPermissions) {
    window.AndroidNativeBridge.requestAllPermissions();
  } else {
    alert('Izin kamera dan bluetooth dapat diaktifkan melalui Pengaturan Android.');
  }
}

function openAndroidAppSettings() {
  if (window.AndroidNativeBridge && window.AndroidNativeBridge.openAppSettings) {
    window.AndroidNativeBridge.openAppSettings();
  } else {
    alert('Buka Pengaturan > Aplikasi > Kasir Idris > Izin.');
  }
}

function clearAiChat() {
  const box = document.getElementById('aiChatBox');
  if (box) {
    box.innerHTML = '<div style="background:var(--card-bg);border:1px solid var(--border-color);border-left:3px solid #a855f7;padding:10px 14px;border-radius:12px;max-width:90%;">Riwayat chat telah dibersihkan. Silakan tanyakan hal lain seputar toko Anda!</div>';
  }
}

function askAiPrompt(p) {
  const inp = document.getElementById('aiInputPrompt');
  if (inp) {
    inp.value = p;
    submitAiPrompt();
  }
}

function submitAiPrompt() {
  const inp = document.getElementById('aiInputPrompt');
  const prompt = (inp?.value || '').trim();
  if (!prompt) return;
  inp.value = '';

  const box = document.getElementById('aiChatBox');
  if (!box) return;

  const userDiv = document.createElement('div');
  userDiv.style.cssText = 'background:#2563eb;color:white;padding:10px 14px;border-radius:12px;max-width:85%;align-self:flex-end;margin-left:auto;';
  userDiv.textContent = prompt;
  box.appendChild(userDiv);

  const loadDiv = document.createElement('div');
  loadDiv.id = 'aiLoading_' + Date.now();
  loadDiv.style.cssText = 'background:var(--card-bg);border:1px solid var(--border-color);border-left:3px solid #a855f7;padding:10px 14px;border-radius:12px;max-width:90%;';
  loadDiv.textContent = 'Sedang menganalisis data toko Anda... ⏳';
  box.appendChild(loadDiv);
  box.scrollTop = box.scrollHeight;

  const today = typeof localDate === 'function' ? localDate() : new Date().toISOString().slice(0, 10);
  const todaySales = Array.isArray(sales) ? sales.filter(x => (x.rawDate || '').startsWith(today)) : [];
  const totalOmzet = todaySales.reduce((a, b) => a + (Number(b.total) || 0), 0);
  const lowStock = Array.isArray(products) ? products.filter(p => Number(p.stock) <= 5).map(p => p.name + ' (' + p.stock + ')').join(', ') : '';
  const contextData = JSON.stringify({
    omzetHariIni: totalOmzet,
    totalTransaksiHariIni: todaySales.length,
    stokMenipis: lowStock || 'Semua aman',
    totalProduk: Array.isArray(products) ? products.length : 0
  });

  if (window.AndroidNativeBridge && window.AndroidNativeBridge.askGemini) {
    window.onGeminiResponse = function(reply) {
      loadDiv.innerHTML = String(reply || '').replace(/\\n/g, '<br>').replace(/\\*\\*(.*?)\\*\\*/g, '<b>$1</b>');
      box.scrollTop = box.scrollHeight;
    };
    window.AndroidNativeBridge.askGemini(prompt, contextData, 'window.onGeminiResponse');
  } else {
    setTimeout(() => {
      loadDiv.innerHTML = '<b>Analisis AI Toko Idris:</b><br>- Omzet hari ini: ' + (typeof money === 'function' ? money(totalOmzet) : 'Rp ' + totalOmzet) + ' dari ' + todaySales.length + ' transaksi.<br>- Kondisi stok: ' + (lowStock ? 'Perlu restock untuk: ' + lowStock : 'Semua stok aman.') + '<br><br>💡 Rekomendasi: Pertahankan ketersediaan barang sembako cepat habis dan berikan penawaran harga menarik pada jam sibuk!';
      box.scrollTop = box.scrollHeight;
    }, 800);
  }
}
</script>
<div id="receiptPreviewModal"`;

if (html.includes(oldScriptEnd)) {
  html = html.replace(oldScriptEnd, helperJs);
  console.log('Added JavaScript helpers.');
}

fs.writeFileSync('app/src/main/assets/www/index.html', html);
console.log('Successfully injected features into app/src/main/assets/www/index.html!');
