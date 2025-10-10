(() => {
  const qs = (sel) => document.querySelector(sel);
  const qsa = (sel) => Array.from(document.querySelectorAll(sel));
  const baseInput = qs('#baseUrl');
  let BASE = baseInput.value.trim();
  const lastReq = qs('#lastRequest');
  const lastRes = qs('#lastResponse');
  const tokenDip = qs('#tokenDip');
  const tokenMgr = qs('#tokenMgr');
  const STORAGE_KEY = 'taskyTesterStateV1';
  const bannerId = 'connectBanner';

  function show(obj) { return JSON.stringify(obj, null, 2); }
  function loadState() {
    try { const s = JSON.parse(localStorage.getItem(STORAGE_KEY)||'{}');
      if (s.base) { BASE = s.base; baseInput.value = s.base; }
      if (s.tokenDip) tokenDip.value = s.tokenDip;
      if (s.tokenMgr) tokenMgr.value = s.tokenMgr;
    } catch {}
  }
  function saveState() {
    const s = { base: BASE, tokenDip: tokenDip.value.trim(), tokenMgr: tokenMgr.value.trim() };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
  }

  function setRaw(req, res) {
    lastReq.textContent = req ? show(req) : '';
    if (!res) { lastRes.textContent=''; return; }
    if (typeof res === 'string') { lastRes.textContent = res; return; }
    // Enhance display of validation errors with fields map
    try {
      if (res.body && res.body.error && res.body.error.fields && typeof res.body.error.fields === 'object') {
        const lines = ['STATUS: '+res.status, 'ERROR CODE: '+res.body.error.code, 'MESSAGE: '+res.body.error.message, 'FIELDS:'];
        for (const [k,v] of Object.entries(res.body.error.fields)) {
          lines.push('  - '+k+': '+(Array.isArray(v)?v.join('; '):v));
        }
        lastRes.textContent = lines.join('\n');
        return;
      }
    } catch {}
    lastRes.textContent = show(res);
  }

  function statusChip(id, status) {
    const el = qs('#'+id);
    if (!el) return;
    el.className = 'status-chip';
    if (!status) { el.textContent=''; return; }
    el.textContent = status;
    if (String(status).startsWith('2')) el.classList.add('ok'); else el.classList.add('err');
  }

  function ensureBanner() {
    if (qs('#'+bannerId)) return qs('#'+bannerId);
    const b = document.createElement('div');
    b.id = bannerId;
    b.style.cssText = 'position:sticky;top:0;z-index:50;padding:.5rem .9rem;font-size:.7rem;font-weight:600;letter-spacing:.5px;background:#FDE68A;color:#92400E;display:flex;align-items:center;gap:.75rem;border-bottom:1px solid #f1c74d';
    b.innerHTML = '<span class="b-status">Checking connectivity...</span><button type="button" id="retryPing" style="margin-left:auto;background:#92400e;color:#fff;border:0;padding:.35rem .7rem;border-radius:4px;cursor:pointer;font-size:.65rem;">Retry</button>';
    document.body.insertBefore(b, document.body.firstChild.nextSibling);
    return b;
  }

  async function pingConnectivity() {
    const b = ensureBanner();
    const label = b.querySelector('.b-status');
    label.textContent = 'Checking connectivity...';
    b.style.background = '#FDE68A'; b.style.color = '#92400E';
  // Health endpoint è solo a livello root (non namespaced). Usiamo origin fisso.
  const healthUrl = window.location.origin + '/health';
    try {
      const res = await fetch(healthUrl, { method:'GET', headers:{'Accept':'application/json'}, cache:'no-store' });
      const text = await res.text();
      let parsed; try { parsed = JSON.parse(text); } catch { parsed = text; }
      if (res.status === 200 && parsed?.db === 'up') {
        label.textContent = 'Healthy (DB up)';
        b.style.background = '#DCFCE7'; b.style.color = '#065F46';
      } else if (res.status === 403) {
        label.textContent = 'API raggiunta ma DB down (403)';
        b.style.background = '#FEE2E2'; b.style.color = '#991B1B';
      } else {
        label.textContent = 'Health check anomalo ('+res.status+')';
        b.style.background = '#FEE2E2'; b.style.color = '#991B1B';
      }
    } catch (e) {
      // fallback legacy probe via /login with fake token
      try {
        const probeUrl = BASE + '/login';
        const resp = await fetch(probeUrl, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({token:'invalid_probe_token'}), cache:'no-store' });
        if (resp.ok || [400,401,403].includes(resp.status)) {
          label.textContent = 'API Reachable (fallback '+resp.status+')';
          b.style.background = '#DCFCE7'; b.style.color = '#065F46';
        } else {
          label.textContent = 'API Non-risponde (fallback '+resp.status+')';
          b.style.background = '#FEE2E2'; b.style.color = '#991B1B';
        }
      } catch (inner) {
        label.textContent = 'Connessione fallita: '+ inner.message;
        b.style.background = '#FEE2E2'; b.style.color = '#991B1B';
      }
    }
  }

  function buildPayload(formEl) {
    const data = {};
    new FormData(formEl).forEach((v,k) => { if (v !== '') data[k]=v; });
    // Auto-fill tokens if placeholder empty
    formEl.querySelectorAll('[data-autofill]').forEach(inp => {
      if (inp.value.trim() === '') {
        const mode = inp.dataset.autofill;
        if (mode === 'manager' && tokenMgr.value) inp.value = tokenMgr.value;
        if (mode === 'any') inp.value = tokenMgr.value || tokenDip.value;
      }
    });
    // Coerce known integer fields so backend strict Integer validation (strict=True) accepts them.
    // This prevents user needing to edit raw JSON to remove quotes around numbers.
    const INT_FIELDS = new Set([
      'Dipartimento_id_dipartimento',
      'anni_lavorativi',
      'id_dipartimento',
      'numero_dipendenti',
      'id_progetto',
      'id'
    ]);
    Object.keys(data).forEach(k => {
      if (INT_FIELDS.has(k) && typeof data[k] === 'string' && /^[0-9]+$/.test(data[k])) {
        // Safe integer coercion (no leading + sign or whitespace allowed by regex)
        data[k] = Number(data[k]);
      }
    });
    return data;
  }

  function syncTextareas() {
    // For each details.raw-toggle textarea -> regenerate JSON from current form
    qsa('form').forEach(f => {
      const ta = f.querySelector('details textarea');
      if (ta) {
        try { ta.value = show(buildPayload(f)); } catch {}
      }
    });
  }

  async function call(path, body, outEl, chipId) {
    const url = BASE + path;
    const req = { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) };
    setRaw({ url, ...req }, null);
    let res, text;
    try { res = await fetch(url, req); text = await res.text(); }
    catch(e){
      // Attempt automatic fallback if port mismatch (commonly 5000 vs 5001)
      const suggestion = suggestBaseFallback();
      outEl.textContent = 'NETWORK ERROR: '+e.message + (suggestion ? ('\nSuggerimento: '+suggestion) : '');
      statusChip(chipId,'ERR');
      return;
    }
    let parsed; try { parsed = JSON.parse(text);} catch { parsed = text; }
    outEl.textContent = show({ status: res.status, body: parsed });
    setRaw({ url, ...req }, { status: res.status, body: parsed });
    statusChip(chipId, res.status);
    return { status: res.status, body: parsed };
  }

  async function handleAction(action, formEl) {
    let payload;
    if (formEl) payload = buildPayload(formEl);
    switch(action) {
      case 'registerDipendente': {
        const outEl = qs('#outRegDip');
        const resp = await call('/register/dipendente', payload, outEl, 'chipRegDip');
        if (resp?.body?.data?.token) tokenDip.value = resp.body.data.token;
        break; }
      case 'registerManager': {
        const outEl = qs('#outRegMgr');
        const resp = await call('/register/manager', payload, outEl, 'chipRegMgr');
        if (resp?.body?.data?.token) tokenMgr.value = resp.body.data.token;
        break; }
      case 'login': {
        const outEl = qs('#outLogin');
        const resp = await call('/login', payload, outEl, 'chipLogin');
        if (resp?.body?.data?.token) {
          if (resp.body.data.type === 'Manager') tokenMgr.value = resp.body.data.token; else tokenDip.value = resp.body.data.token;
        }
        break; }
      case 'addProject': {
        const outEl = qs('#outProj');
        await call('/add/Project', payload, outEl, 'chipProj');
        break; }
      case 'addDepartment': {
        const outEl = qs('#outDept');
        await call('/add/Department', payload, outEl, 'chipDept');
        break; }
      case 'addTask': {
        const outEl = qs('#outTask');
        await call('/add/Task', payload, outEl, 'chipTask');
        break; }
      case 'projectByDept': {
        const outEl = qs('#outProjDept');
        await call('/project/by-department', payload, outEl, 'chipProjDept');
        break; }
      case 'taskByProject': {
        const outEl = qs('#outTaskProj');
        await call('/task/by-project', payload, outEl, 'chipTaskProj');
        break; }
      case 'numeroDipendenti': {
        const outEl = qs('#outNumDip');
        await call('/numeroDipendenti', payload, outEl, 'chipNumDip');
        break; }
      case 'projectsInProgress': {
        const outEl = qs('#outProjInProgress');
        const table = qs('#projInProgressTable');
        const form = formEl || document.querySelector('form[data-form="projectsInProgress"]');
        const payload = form ? buildPayload(form) : {};
        const resp = await call('/projects/in-progress', payload, outEl, 'chipProjInProgress');
        if (!resp) return;
        // Display raw response like other cards
        outEl.textContent = JSON.stringify(resp.body, null, 2);
        statusChip('chipProjInProgress', resp.status);
  // No visual table for this card; response is shown raw above.
        break; }
      case 'dipendentiByDept': {
        const outEl = qs('#outDipDept');
        await call('/dipendenti/by-department', payload, outEl, 'chipDipDept');
        break; }
      case 'dipendentiByProject': {
        const outEl = qs('#outDipProj');
        const resp = await call('/dipendenti/by-project', payload, outEl, 'chipDipProj');
        // Optionally render a small table if items present
        try {
          if (resp && resp.body && resp.body.data && Array.isArray(resp.body.data.items)) {
            const items = resp.body.data.items;
            if (items.length > 0) {
              const keys = Object.keys(items[0]);
              outEl.innerHTML = '<table class="table table-sm"><thead><tr>' + keys.map(k=>'<th>'+k+'</th>').join('') + '</tr></thead><tbody>' + items.map(it=>'<tr>'+keys.map(k=>'<td>'+(it[k]===null?'':String(it[k]))+'</td>').join('')+'</tr>').join('') + '</tbody></table>';
            }
          }
        } catch(e){/* ignore render errors */}
        break; }
      case 'managersByProject': {
        const outEl = qs('#outMgrProj');
        const resp = await call('/managers/by-project', payload, outEl, 'chipMgrProj');
        // render table if items present
        try {
          if (resp && resp.body && resp.body.data && Array.isArray(resp.body.data.items)) {
            const items = resp.body.data.items;
            if (items.length > 0) {
              const keys = Object.keys(items[0]);
              outEl.innerHTML = '<table class="table table-sm"><thead><tr>' + keys.map(k=>'<th>'+k+'</th>').join('') + '</tr></thead><tbody>' + items.map(it=>'<tr>'+keys.map(k=>'<td>'+(it[k]===null?'':String(it[k]))+'</td>').join('')+'</tr>').join('') + '</tbody></table>';
            }
          }
        } catch(e){/* ignore render errors */}
        break; }
      case 'projectsByDipendente': {
        const outEl = qs('#outProjByDip');
        const resp = await call('/projects/by-dipendente', payload, outEl, 'chipProjDip');
        if (!resp) return;
        // Render items if present
        try {
          if (resp.body && resp.body.data && Array.isArray(resp.body.data.items)) {
            const items = resp.body.data.items;
            if (items.length > 0) {
              const keys = Object.keys(items[0]);
              outEl.innerHTML = '<table class="table table-sm"><thead><tr>' + keys.map(k=>'<th>'+k+'</th>').join('') + '</tr></thead><tbody>' + items.map(it=>'<tr>'+keys.map(k=>'<td>'+(it[k]===null?'':String(it[k]))+'</td>').join('')+'</tr>').join('') + '</tbody></table>';
            }
          }
        } catch(e) { /* ignore */ }
        break; }
      case 'projectBudget': {
        const outEl = qs('#outProjBudget');
        const resp = await call('/projects/budget', payload, outEl, 'chipBudget');
        if (!resp) return;
        try {
          if (resp.body && resp.body.data && resp.body.data.budget !== undefined) {
            outEl.textContent = JSON.stringify(resp.body, null, 2);
          }
        } catch(e) { /* ignore */ }
        break; }
      case 'updateProject': {
        const outEl = qs('#outUpdProj');
        await call('/update/Project', payload, outEl, 'chipUpdProj');
        break; }
      case 'deleteProject': {
        const outEl = qs('#outDelProj');
        await call('/delete/Project', payload, outEl, 'chipDelProj');
        break; }
      case 'updateTask': {
        const outEl = qs('#outUpdTask');
        await call('/update/Task', payload, outEl, 'chipUpdTask');
        break; }
      case 'deleteTask': {
        const outEl = qs('#outDelTask');
        await call('/delete/Task', payload, outEl, 'chipDelTask');
        break; }
      case 'dipendentiDataByDept': {
        const outEl = qs('#outDipDeptData');
        const table = qs('#dipDeptTable');
        const copyBtn = qs('#copyDipCsv');
        // Clear table
        if (table) { table.querySelector('thead').innerHTML=''; table.querySelector('tbody').innerHTML=''; }
        const resp = await call('/dipendenti/data/by-department', payload, outEl, 'chipDipDeptData');
        if (!resp) break;
        const body = resp.body;
        if (!body || !body.data) { if (copyBtn) copyBtn.style.display='none'; break; }
        const items = body.data.items || [];
        if (items.length === 0) { if (copyBtn) copyBtn.style.display='none'; break; }
        // Build header from keys of first item
        const keys = Object.keys(items[0]);
        if (table) table.querySelector('thead').innerHTML = '<tr>' + keys.map(k=> '<th>'+k+'</th>').join('') + '</tr>';
        if (table) table.querySelector('tbody').innerHTML = items.map(it => '<tr>' + keys.map(k=> '<td>' + (it[k]===null? '': String(it[k])) + '</td>').join('') + '</tr>').join('');
        // Prepare CSV copy
        if (copyBtn) {
          copyBtn.style.display='inline-block';
          copyBtn.onclick = () => {
            const csv = [keys.join(',')].concat(items.map(it => keys.map(k=> '"'+String((it[k]===null?'':it[k])).replace(/"/g,'""')+'"').join(','))).join('\n');
            navigator.clipboard.writeText(csv).then(()=>{ copyBtn.textContent='CSV copiato'; setTimeout(()=>copyBtn.textContent='Copia CSV',1400); });
          };
        }
        break; }
      default: alert('Azione non riconosciuta: '+action);
    }
    saveState();
    syncTextareas();
    if (qs('#autoSnapshot')?.checked) {
      fetchSnapshot();
    }
  }

  document.body.addEventListener('click', e => {
    const btn = e.target.closest('button[data-action]');
    if (btn) {
      const form = btn.closest('form');
      e.preventDefault();
      handleAction(btn.getAttribute('data-action'), form);
    }
    const copyBtn = e.target.closest('button[data-copy]');
    if (copyBtn) {
      const id = copyBtn.getAttribute('data-copy');
      const val = qs('#'+id).value.trim();
      navigator.clipboard.writeText(val).then(()=>{
        copyBtn.textContent='Copiato!'; setTimeout(()=>copyBtn.textContent='Copia',1400);
      });
    }
  });

  qs('#setBase').addEventListener('click', () => {
    BASE = baseInput.value.trim().replace(/\/$/,'');
    qs('#status').textContent = 'Base aggiornata';
    saveState();
    setTimeout(()=> qs('#status').textContent='',1500);
  });

  function suggestBaseFallback() {
    try {
      const current = BASE;
      const parsed = new URL(current);
      // If running page on a different origin, prefer same origin + /api
      const pageOrigin = window.location.origin;
      if (!current.startsWith(pageOrigin)) {
        BASE = pageOrigin + '/api';
        baseInput.value = BASE;
        saveState();
        return 'Base aggiornata automaticamente a '+BASE;
      }
      // Toggle common dev ports 5000/5001
      if (parsed.port === '5000') {
        parsed.port = '5001';
        BASE = parsed.origin + parsed.pathname; // pathname ends with /api if present
        baseInput.value = BASE;
        saveState();
        return 'Prova anche porta 5001 → '+BASE;
      }
      if (parsed.port === '5001') {
        parsed.port = '5000';
        BASE = parsed.origin + parsed.pathname;
        baseInput.value = BASE;
        saveState();
        return 'Prova anche porta 5000 → '+BASE;
      }
    } catch {}
    return '';
  }

  // Auto-detect on first load if default looks mismatched
  function initialAutoDetect() {
    if (!localStorage.getItem(STORAGE_KEY)) {
      // If app served on different port, align to same origin /api
      const pageOrigin = window.location.origin;
      if (!BASE.startsWith(pageOrigin)) {
        BASE = pageOrigin + '/api';
        baseInput.value = BASE;
      }
    }
  }

  // Initialize
  loadState();
  initialAutoDetect();
  syncTextareas();
  // Connectivity banner + first ping
  setTimeout(()=> { pingConnectivity(); }, 50);
  document.addEventListener('click', (e)=>{
    if (e.target && e.target.id === 'retryPing') {
      pingConnectivity();
    }
  });

  async function fetchSnapshot() {
    const out = qs('#dbSnapshot');
    if (!out) return;
    const chip = 'chipSnapshot';
    const url = BASE + '/debug/snapshot';
    out.textContent = 'Caricamento snapshot...';
    statusChip(chip, '...');
    try {
      const res = await fetch(url, { method:'GET', headers: { 'Accept':'application/json' } });
      const text = await res.text();
      let parsed; try { parsed = JSON.parse(text); } catch { parsed = text; }
      out.textContent = show(parsed);
      statusChip(chip, res.status);
    } catch (e) {
      out.textContent = 'Errore caricamento snapshot: '+e.message;
      statusChip(chip, 'ERR');
    }
  }
  qs('#btnSnapshot')?.addEventListener('click', ()=> fetchSnapshot());
  // primo caricamento
  setTimeout(()=> fetchSnapshot(), 150);
})();
