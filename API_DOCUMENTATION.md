## Tasky API – Specifica Tecnica Completa

Documento di riferimento definitivo per tutte le rotte disponibili, con requisiti di input, formati di output, regole di visibilità e mappatura errori.

---
## 1. Convenzioni Generali
* Base path: `/api`
* Content-Type richiesto: `application/json`
* Date dominio (progetti / task): `YYYY-MM-DD`
* Budget in risposta: stringa con due decimali (es: `"10000.50"`)
* Autenticazione attuale: token nel body `{ "token": "..." }` (Roadmap: header `Authorization: Bearer <token>`)
* Risposta di successo generica:
  ```json
  { "data": { ... }, "message": "<opzionale>" }
  ```
* Risposta di errore generica:
  ```json
  { "error": { "code": "...", "message": "...", "fields": { "campo": ["violazione"] } } }
  ```
* I campi invaldi / mancanti vengono elencati in `error.fields` (quando disponibili) con `code = MISSING_PARAMS` (o `MISSING_CREDENTIALS` per casi login).
* `data_nascita` può essere formattata dal driver come data estesa (RFC 1123); standardizzazione pianificata.

## 2. Ruoli e Scope
| Ruolo | Capacità principali | Scope restituito |
|-------|--------------------|------------------|
| Manager | CRUD completo su Progetti e Task del proprio dipartimento; visibilità completa | `all` |
| Dipendente | Solo visibilità di progetti & task in cui ha Task assegnate; nessun CRUD | `own` |

## 3. Codici Errore
| Code | HTTP | Significato |
|------|------|------------|
| MISSING_PARAMS | 400 | Parametri obbligatori mancanti / validazione fallita |
| MISSING_CREDENTIALS | 400 | Richiesta login incompleta |
| INVALID_CREDENTIALS | 401 | Credenziali errate |
| AUTH_TOKEN_MISSING | 401 | Token non fornito |
| AUTH_TOKEN_INVALID | 403 | Token inesistente/invalidato |
| AUTH_FORBIDDEN_ROLE | 403 | Ruolo non autorizzato alla risorsa |
| AUTH_FORBIDDEN_DEPARTMENT | 403 | Manager fuori dal proprio dipartimento |
| NOT_FOUND | 404 | Risorsa da aggiornare/eliminare non trovata |
| DUPLICATE_USER | 409 | Email già registrata |
| DUPLICATE_PROJECT | 409 | Id progetto già esistente (inserimento esplicito) |
| DUPLICATE_DEPARTMENT | 409 | Dipartimento già esistente |
| DB_INTEGRITY_ERROR | 400/409 | Violazione FK / vincolo unico |
| DB_ERROR | 500 | Errore database generico |
| SERVER_ERROR | 500 | Errore interno generico |
| UNHANDLED_EXCEPTION | 500 | Eccezione non classificata |

## 4. Autenticazione & Utenti
### 4.1 POST /api/register/dipendente (Pubblico)
Registra un dipendente. I campi `sesso` e `numero_telefono` sono OBBLIGATORI.
Richiesta:
```json
{ "email":"user@example.com", "password":"pwd", "nome":"Mario", "cognome":"Rossi", "data_nascita":"1990-01-01", "sesso":"M", "numero_telefono":"+39 333 1234567", "Dipartimento_id_dipartimento":1 }
```
Risposta 201:
```json
{ "message":"User registered successfully", "data": { "token":"<token>" } }
```
Errori: MISSING_PARAMS, DUPLICATE_USER, DB_INTEGRITY_ERROR, DB_ERROR.

### 4.2 POST /api/register/manager (Pubblico)
Come dipendente + `anni_lavorativi` (int). I campi `sesso` e `numero_telefono` sono OBBLIGATORI.
Errori: come sopra + MISSING_PARAMS se campo aggiuntivo mancante.

### 4.3 POST /api/login (Pubblico)
Varianti input: `{ "token":"..." }` oppure `{ "email":"...", "password":"..." }`.
Risposta 200:
```json
{ "message":"Login effettuato", "data": { "token":"<token>", "type":"Manager|Dipendente" } }
```
Errori: MISSING_CREDENTIALS, INVALID_CREDENTIALS, AUTH_TOKEN_INVALID.

## 5. Dipartimenti
### 5.1 POST /api/add/Department (Manager)
```json
{ "token":"<manager>", "nome":"HR", "id_dipartimento":2, "numero_dipendenti":0 }
```
201:
```json
{ "message":"Dipartimento creato correttamente", "data": { "id_dipartimento":2 } }
```
Errori: AUTH_TOKEN_MISSING, AUTH_FORBIDDEN_ROLE, DUPLICATE_DEPARTMENT, MISSING_PARAMS, DB_INTEGRITY_ERROR.

### 5.2 POST /api/numeroDipendenti (Manager)
```json
{ "token":"<manager>", "id_dipartimento":1 }
```
200: `{ "data": { "n_dipendenti": 5 } }`
Errori: AUTH_*, MISSING_PARAMS.

### 5.3 POST /api/dipendenti/by-department (Manager)
Input: `{ "token":"<manager>", "id_dipartimento":1 }`
200:
```json
{ "data": { "items": [ { "email":"...", "nome":"...", "cognome":"...", "data_nascita":"..." } ], "count":1 } }
```
Errori: AUTH_*, MISSING_PARAMS.

### 5.4 POST /api/dipendenti/data/by-department (Manager)
Come sopra ma con attributi estesi (es. anni_lavorativi se Manager ecc.).

## 6. Progetti
### 6.1 POST /api/add/Project (Manager)
```json
{ "token":"<manager>", "descrizione":"Descr.", "budget":10000.5, "nome":"Alpha", "data_inizio":"2025-01-01", "data_fine":"2025-12-31", "id_dipartimento":1, "id_progetto":100 }
```
201: `{ "data": { "id_progetto":100 }, "message":"Progetto inserito correttamente" }`
Errori: AUTH_TOKEN_MISSING, AUTH_FORBIDDEN_ROLE, AUTH_FORBIDDEN_DEPARTMENT, MISSING_PARAMS, DUPLICATE_PROJECT, DB_INTEGRITY_ERROR.

### 6.2 POST /api/update/Project (Manager)
Richiede `id_progetto` + `id_dipartimento` + almeno un campo aggiornabile.
Esempio:
```json
{ "token":"<manager>", "id_progetto":100, "id_dipartimento":1, "nome":"Nuovo", "descrizione":"Agg." }
```
200 (shape indicativo):
```json
{ "data": { "id_progetto":100, "nome":"Nuovo", "descrizione":"Agg.", "budgetIstanziato":"10000.50", "dataInizio":"2025-01-01", "dataFine":"2025-12-31", "Dipartimento_id_dipartimento":1 } }
```
Errori: MISSING_PARAMS (nessun campo da aggiornare), NOT_FOUND, AUTH_*.

### 6.3 POST /api/delete/Project (Manager)
```json
{ "token":"<manager>", "id_progetto":100, "id_dipartimento":1 }
```
200: `{ "data": { "id_progetto":100, ... }, "message":"Progetto eliminato" }`
Errori: NOT_FOUND, AUTH_*, MISSING_PARAMS.

### 6.4 POST /api/project/by-department (Manager o Dipendente)
Manager: `{ "token":"<manager>", "id_dipartimento":1 }`
Dipendente: `{ "token":"<dipendente>", "id_dipartimento":1 }`
200:
```json
{ "data": { "items": [ { "id_progetto":100, "nome":"Alpha", "budgetIstanziato":"10000.50", "dataInizio":"2025-01-01", "dataFine":"2025-12-31" } ], "count":1, "scope":"all|own" } }
```
Errori: MISSING_PARAMS, AUTH_TOKEN_INVALID, AUTH_FORBIDDEN_DEPARTMENT (solo manager), AUTH_TOKEN_MISSING.

### 6.5 POST /api/projects/in-progress (Manager)
Input: `{ "token":"<manager>" }`
200: `{ "data": { "items":[...], "count":N, "scope":"all" } }`
Errori: AUTH_*.

### 6.6 POST /api/projects/by-dipendente (Pubblico)
Input: `{ "email_dipendente":"dip@example.com" }`
200: `{ "data": { "items":[...], "count":N } }`
Errori: MISSING_PARAMS.

### 6.7 POST /api/projects/budget (Pubblico)
Input: `{ "id_progetto":100 }`
200: `{ "data": { "budget":"10000.50" } }`
Errori: MISSING_PARAMS.

## 7. Task
### 7.1 POST /api/add/Task (Manager)
```json
{ "token":"<manager>", "stato":"Open", "descrizione":"Task", "data_inizio":"2025-03-01", "data_fine":"2025-03-31", "id_progetto":100, "id_dipartimento":1, "email_dipendente":"dip@example.com", "email_manager":"mgr@example.com" }
```
201: `{ "data": { "id_task": 7 }, "message":"Task inserita correttamente" }`
Errori: AUTH_*, MISSING_PARAMS, DB_INTEGRITY_ERROR.

### 7.2 POST /api/update/Task (Manager)
Minimo: `token`, `id`, `id_progetto`, `id_dipartimento` + almeno un campo aggiornabile (`stato`, `descrizione`, `data_inizio`, `data_fine`, `email_dipendente`, `email_manager`).
200: `{ "data": { "items": [ <tutte le task progetto> ], "count": N } }`
Errori: MISSING_PARAMS, NOT_FOUND, AUTH_*.

### 7.3 POST /api/delete/Task (Manager)
```json
{ "token":"<manager>", "id":7, "id_progetto":100, "id_dipartimento":1 }
```
200: `{ "data": { "items": [...restanti...], "count": N }, "message":"Task eliminata" }`
Errori: NOT_FOUND, MISSING_PARAMS, AUTH_*.

### 7.4 POST /api/task/by-project (Manager o Dipendente)
Manager: `{ "token":"<manager>", "id_progetto":100, "id_dipartimento":1 }`
Dipendente: `{ "token":"<dipendente>", "id_progetto":100 }`
200: `{ "data": { "items": [...], "count": N, "scope":"all|own" } }`
Errori: MISSING_PARAMS, AUTH_TOKEN_INVALID, AUTH_FORBIDDEN_DEPARTMENT (manager mismatch), AUTH_TOKEN_MISSING.

## 8. Visibilità Persone per Progetto
### 8.1 POST /api/dipendenti/by-project (Manager)
```json
{ "token":"<manager>", "id_progetto":100, "id_dipartimento":1 }
```
200: `{ "data": { "items":[{"email":"..."}], "count":N, "scope":"all" } }`
Errori: AUTH_*, MISSING_PARAMS.

### 8.2 POST /api/managers/by-project (Manager o Dipendente)
Manager: `{ "token":"<manager>", "id_progetto":100, "id_dipartimento":1 }`
Dipendente: `{ "token":"<dipendente>", "id_progetto":100 }`
200: `{ "data": { "items":[], "count":0, "scope":"all|own" } }`
Errori: MISSING_PARAMS, AUTH_TOKEN_INVALID, AUTH_FORBIDDEN_DEPARTMENT (manager mismatch), AUTH_TOKEN_MISSING.

## 9. Debug
### 9.1 GET /api/debug/snapshot (Sviluppo)
Nessun input. Restituisce dump (tabelle chiave + token). NON usare in produzione.
Include anche la tabella `push_subscriptions`. Le tabelle degli utenti espongono anche `sesso` e `numero_telefono`.
200: `{ "data": { "Dipartimento": [...], "Manager": [{"email":"...","sesso":"...","numero_telefono":"...",...}], "Dipendente": [{"email":"...","sesso":"...","numero_telefono":"...",...}], "Progetto": [...], "TASK": [...], "push_subscriptions": [...] }, "message":"Snapshot OK" }`

## 10. Notifiche Push (FCM)

Questa API supporta notifiche push tramite Firebase Cloud Messaging (FCM).

Prerequisiti:
- Variabile d'ambiente `FCM_CREDENTIALS_PATH` impostata e puntata al JSON del service account.
- Il file credenziali montato nel container (read-only) e non versionato.
- Egress verso `fcm.googleapis.com:443` abilitato.

### 10.1 POST /api/push/register (Autenticato)
Registra o aggiorna un `fcm_token` per l'utente autenticato.

Input:
```json
{ "token": "<token_api>", "fcm_token": "<FCM_TOKEN>", "platform": "android" }
```
Output:
```json
{ "message": "Push token registrato" }
```
Errori: AUTH_TOKEN_MISSING, AUTH_TOKEN_INVALID, MISSING_PARAMS.

Note:
- Il token è unico (UNIQUE). Una nuova registrazione con lo stesso fcm_token aggiorna email/role/platform.

### 10.2 POST /api/push/unregister (Autenticato)
Rimuove un `fcm_token` dal backend.

Input:
```json
{ "token": "<token_api>", "fcm_token": "<FCM_TOKEN>" }
```
Output:
```json
{ "message": "Push token rimosso" }
```
Errori: AUTH_TOKEN_MISSING, AUTH_TOKEN_INVALID, MISSING_PARAMS.

### 10.3 (DEV) POST /api/debug/push/test
Endpoint di sviluppo per inviare una notifica di prova.

Input (fornire `email` oppure `fcm_token`):
```json
{ "token": "<token_api>", "email": "user@example.com", "title": "Ping", "body": "Hello", "data": {"k":"v"} }
```
Output successo:
```json
{ "data": { "tokens": ["<FCM_TOKEN>"], "result": { "success": 1, "failure": 0, "invalid": [] } } }
```
Output errore invio (non 500, ma 200 con payload di errore):
```json
{ "data": { "tokens": ["<FCM_TOKEN>"] }, "error": { "code": "FCM_SEND_FAILED", "message": "..." } }
```

Pulizia automatica:
- In caso di `messaging/registration-token-not-registered`, il token viene rimosso da `push_subscriptions`.

### 10.5 (DEV) GET /api/debug/push/status
Verifica stato configurazione FCM lato backend.

Input: nessuno

Output 200:
```json
{
  "data": {
    "FCM_CREDENTIALS_PATH": "/run/secrets/firebase_service_account.json",
    "credentials_exists": true,
    "initialized": true
  }
}
```
Note: endpoint di diagnostica sviluppo; non esporre in produzione.

### 10.4 Hook sugli eventi Task
- Creazione Task: invia push a dipendente assegnato e manager.
- Update Task: invia push a dipendente assegnato e manager.
- Delete Task: invia push a dipendente assegnato e manager.

L’invio è asincrono (thread best-effort) e non incide sui tempi di risposta.

## 11. Riepilogo CRUD Modifica / Eliminazione
| Endpoint | Metodo | Ruolo | Requisiti minimi | Errore specifico |
|----------|--------|-------|------------------|------------------|
| /api/update/Project | POST | Manager | id_progetto + id_dipartimento + ≥1 campo | MISSING_PARAMS se nessun campo |
| /api/delete/Project | POST | Manager | id_progetto + id_dipartimento | NOT_FOUND se assente |
| /api/update/Task | POST | Manager | id + id_progetto + id_dipartimento + ≥1 campo | MISSING_PARAMS / NOT_FOUND |
| /api/delete/Task | POST | Manager | id + id_progetto + id_dipartimento | NOT_FOUND |

## 11. Pattern di Output Ricorrenti
| Caso | Struttura |
|------|-----------|
| Creazione entità | `{ "data": { <id_field>: <val> }, "message": "..." }` |
| Lista con scope | `{ "data": { "items": [...], "count": N, "scope": "all|own" } }` |
| Lista semplice | `{ "data": { "items": [...], "count": N } }` |
| Aggiornamento progetto | `{ "data": { <campi_progetto_aggiornati> } }` |
| Eliminazione progetto | `{ "data": { <riga_progetto> }, "message": "Progetto eliminato" }` |
| Eliminazione task | `{ "data": { "items": [...], "count": N }, "message": "Task eliminata" }` |
| Budget progetto | `{ "data": { "budget": "NNNNN.nn" } }` |
| Errore validazione | `{ "error": { "code":"MISSING_PARAMS", "message":"Validazione fallita", "fields": { ... } } }` |

## 12. Roadmap
* Supporto Authorization Bearer token (retrocompatibilità temporanea body `token`)
* Uniformare `data_nascita` a `YYYY-MM-DD`
* Versioning `/api/v2` per breaking changes
* Test unitari e CI pipeline

---
Ultimo aggiornamento: 2025-10-12
