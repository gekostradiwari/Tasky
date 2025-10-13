# TaskyAPI

API Flask per gestione di Dipendenti, Manager, Progetti e Task con controllo di visibilità basato sul ruolo.

## Indice
1. Panoramica
2. Avvio rapido (Docker & locale)
3. Configurazione Database
4. Autenticazione & Ruoli
5. Schema Risposte & Errori
6. Endpoints principali
7. Script di test (`scripts/curl_tests.sh`)
8. Sicurezza & Roadmap
9. Variabili d'Ambiente
10. Documentazione Sphinx & Tester
11. Flusso end‑to‑end esempio
12. Comandi Docker utili

## 1. Panoramica
L'API espone operazioni per:
- Registrare utenti (Manager, Dipendenti)
- Effettuare login (token o credenziali)
- Creare Progetti (solo Manager del dipartimento)
- Creare Task (solo Manager del dipartimento)
- Ottenere visibilità progetti / task con filtro dinamico (Manager vs Dipendente) tramite endpoint unificati:
	- `/api/project/by-department`
	- `/api/task/by-project`

## 2. Avvio rapido
### Docker (stack completo: DB + API + Docs)
```powershell
docker compose up --build
```
Servizi default:
- API: http://localhost:5001/api
- Tester UI (se montata sulla stessa porta dell'app): http://localhost:5001/tester
- Docs Sphinx statiche: http://localhost:8080/

## UI Tester Rapido

È stata aggiunta una semplice interfaccia HTML per test manuali degli endpoint senza dover usare `curl`.

Percorso: tipicamente `http://127.0.0.1:5001/tester` (o la porta configurata nell'ENV `PORT`).

Funzionalità incluse:
- Registrazione Dipendente / Manager
- Login (email+password o token)
- Creazione Progetto / Task
- Endpoint visibilità: progetti per dipartimento e task per progetto
- Copia rapida dei token
- Visualizzazione ultima richiesta/risposta

Come usare:
1. Apri `/tester` nel browser.
2. Imposta la Base URL se differente (default `http://127.0.0.1:5000/api`).
3. Esegui prima una registrazione Manager e Dipendente per ottenere i token.
4. I placeholder `__MANAGER_TOKEN__` e `__TOKEN__` vengono sostituiti automaticamente con i token inseriti.
5. Controlla le risposte nelle card o nel pannello Raw Response.

### Esecuzione locale (senza Docker)
1. Crea e configura MySQL (stesso schema di `database.sql`).
2. Crea un virtualenv:
```powershell
python -m venv venv; ./venv/Scripts/Activate.ps1
pip install -r requirements.txt
```
3. Esporta variabili (PowerShell):
```powershell
$env:DB_HOST="127.0.0.1"; $env:DB_USER="root"; $env:DB_PASSWORD="rootpassword"; $env:DB_NAME="tasky"; $env:PORT="5001"
python api.py
```

## 3. Configurazione Database
- File schema: `database.sql` (eseguito automaticamente al primo avvio del container MySQL tramite init scripts).
- Tabelle principali: `Manager`, `Dipendente`, `Progetto`, `TASK`, `Dipartimento`.

## 4. Autenticazione & Ruoli
- Token (hex) generato alla registrazione, memorizzato nella tabella dell'utente.
- Login via token oppure email/password.
- Ruoli: `Manager`, `Dipendente`.
- Decorator principale per creazione risorse: `@manager_of_department('id_dipartimento')` (verifica token, ruolo e dipartimento coerente con l'utente Manager).
- Per la visibilità progetti/task NON si usa il decorator: la logica di filtro è delegata a handler nel repository che determinano `scope`.

### Visibilità (scope)
| Endpoint | Manager | Dipendente |
|----------|---------|-----------|
| `/api/project/by-department` | Tutti i progetti del suo dipartimento (`scope=all`) | Solo progetti dove ha almeno una task (`scope=own`) |
| `/api/task/by-project` | Tutte le task del progetto (richiede anche `id_dipartimento`) (`scope=all`) | Solo le proprie task su quel progetto (`scope=own`) |

## 5. Schema Risposte & Errori
Successo:
```json
{
	"message": "<facoltativo>",
	"data": { ... }
}
```
Errore unificato:
```json
{
	"error": {
		"code": "UPPER_SNAKE_CODE",
		"message": "Messaggio in italiano",
		"...": "campi extra opzionali"
	}
}
```
Esempi:
```json
{"error":{"code":"AUTH_TOKEN_MISSING","message":"Token mancante"}}
{"error":{"code":"DUPLICATE_USER","message":"L'utente è già registrato"}}
{"error":{"code":"MISSING_PARAMS","message":"Parametri richiesti mancanti","required":["id_dipartimento"]}}
```

### Codici Errore Principali
| Codice | Significato |
|--------|-------------|
| MISSING_PARAMS | Parametri richiesti mancanti |
| MISSING_CREDENTIALS | Mancano email o password nel login |
| INVALID_CREDENTIALS | Credenziali errate |
| AUTH_TOKEN_MISSING | Token non presente |
| AUTH_TOKEN_INVALID | Token inesistente o non valido |
| AUTH_FORBIDDEN_ROLE | Ruolo non autorizzato alla risorsa |
| AUTH_FORBIDDEN_DEPARTMENT | Manager di altro dipartimento |
| DUPLICATE_USER | Utente già registrato |
| DUPLICATE_PROJECT | Progetto con stesso id già esistente |
| DB_INTEGRITY_ERROR | Violazione integrità referenziale/unique |
| DB_ERROR | Errore DB generico |
| SERVER_ERROR | Errore interno generico |

## 6. Endpoints principali

### Registrazione Dipendente
POST `/api/register/dipendente`
Body richiesto: `email,password,nome,cognome,data_nascita,Dipartimento_id_dipartimento`
Risposta 201:
```json
{ "message": "User registered successfully", "data": { "token": "..." } }
```

### Registrazione Manager
POST `/api/register/manager`
Body richiesto: precedente + `anni_lavorativi`
Risposta 201 analoga.

### Login
POST `/api/login`
Modalità alternative:
1. `{ "token": "..." }`
2. `{ "email": "...", "password": "..." }`
Successo 200:
```json
{ "message": "Login effettuato", "data": { "token": "...", "type": "Manager" } }
```

### Creazione Progetto
POST `/api/add/Project`
Richiede token Manager del dipartimento passato (`id_dipartimento`).
Body minimo: `token, descrizione, budget, nome, data_inizio, data_fine, id_dipartimento` (+ opzionale `id_progetto`).
Risposta 201:
```json
{ "message": "Progetto inserito correttamente", "data": { "id_progetto": 42 } }
```

### Creazione Task
POST `/api/add/Task`
Richiede token Manager coerente col dipartimento (`id_dipartimento`).
Body: `token, stato, descrizione, data_inizio, data_fine, id_progetto, id_dipartimento, email_dipendente, email_manager` (+ opzionale `id`).
Risposta 201: `{ "data": { "id_task": <int> }, "message": "Task inserita correttamente" }`

### Creazione Dipartimento
POST `/api/add/Department`
Richiede token Manager (ruolo Manager). Questo endpoint permette ai manager di creare un nuovo dipartimento.
Body minimo:
```json
{
	"token": "<token_manager>",
	"nome": "NomeDipartimento",
	"id_dipartimento": 10,           // opzionale
	"numero_dipendenti": 0           // opzionale
}
```
Risposta 201:
```json
{ "message": "Dipartimento creato correttamente", "data": { "id_dipartimento": 2 } }
```
Errori possibili:
- 400 MISSING_PARAMS (campo richiesto mancante)
- 401 AUTH_TOKEN_MISSING (token non fornito)
- 403 AUTH_FORBIDDEN_ROLE (token di Dipendente)
- 409 DUPLICATE_DEPARTMENT (tentativo di creare dipartimento con id/esistenza duplicata)
- 400/409 DB_INTEGRITY_ERROR (FK o altri vincoli)
- 500 DB_ERROR / SERVER_ERROR

### Visibilità Progetti
POST `/api/project/by-department`
Body: `token, id_dipartimento`.
Risposta 200:
```json
{ "data": { "items": [ {...} ], "count": 2, "scope": "all" } }
```

### Visibilità Task Progetto
POST `/api/task/by-project`
Manager body: `token, id_progetto, id_dipartimento`
Dipendente body: `token, id_progetto`
Risposta 200: `{ "data": { "items": [...], "count": N, "scope": "own|all" } }`

## 7. Script di Test
File: `scripts/curl_tests.sh`
Esegue test end‑to‑end includendo:
- Registrazioni e duplicati
- Login token & credenziali
- Creazione progetto/task
- Visibilità (scope all/own) e casi negativi (parametri mancanti, dipartimento errato, token invalido)

Esecuzione (PowerShell + Git Bash o WSL):
```powershell
$env:API_PORT=5001; bash scripts/curl_tests.sh
```

## 8. Sicurezza & Roadmap
- Password: attuale hashing semplice (SHA-256) → passare a bcrypt
- Token senza scadenza → introdurre expiry & refresh
- Validazione schema → Uso di Marshmallow
- Logging strutturato / tracing
- OpenAPI / Swagger UI
- Rate limiting / antiflood
- Header Authorization Bearer invece del body token

## 9. Variabili d'Ambiente Principali
| Nome | Descrizione | Default docker-compose |
|------|-------------|------------------------|
| DB_HOST | Host MySQL | mysql |
| DB_USER | Utente DB | root |
| DB_PASSWORD | Password DB | rootpassword |
| DB_NAME | Nome database | tasky |
| PORT | Porta Flask | 5001 |

## 10. Documentazione Sphinx & Tester
Componenti documentazione:
1. `API_DOCUMENTATION.md` – Specifica dettagliata (serve come sorgente verità).
2. Sphinx (cartella `docs/`) – Documentazione navigabile (inclusa pagina `endpoints.html`).
3. Pagina Tester – Interfaccia manuale per provare le rotte.

Link rapidi (valori di default):
| Risorsa | URL |
|---------|-----|
| API Base | http://localhost:5001/api |
| Sphinx Docs | http://localhost:8080/index.html |
| Sphinx Endpoints | http://localhost:8080/endpoints.html |
| Tester | http://localhost:5001/tester |

### Rigenerare Sphinx localmente

### Installazione dipendenze docs (locale)
```powershell
python -m venv venv; ./venv/Scripts/Activate.ps1
pip install -r requirements.txt
pip install -r requirements-docs.txt
cd docs; make html
```
Output: `docs/_build/html/index.html`

Se usi Docker, lo stage `docs` nel `docker-compose.yml` serve direttamente `docs/_build/html` via `python -m http.server`.


## 11. Flusso end‑to‑end esempio (Manager crea progetto & task)
```text
1. POST /api/register/manager  -> ottieni token_manager
2. POST /api/register/dipendente -> ottieni token_dipendente
3. POST /api/add/Project (usa token_manager) -> id_progetto
4. POST /api/add/Task (token_manager + email_dipendente)
5. POST /api/project/by-department (token_manager) -> scope=all, lista progetti
6. POST /api/task/by-project (token_dipendente) -> scope=own (solo le sue task)
7. POST /api/projects/budget (pubblico) -> budget formattato "NNNNN.nn"
```

## 12. Comandi Docker utili
Avvio completo (ricostruzione forzata):
```powershell
docker compose build --no-cache; docker compose up -d
```

Chiusura docker
```powershell
docker compose down
```

Cancellazioni volumi del db
```powershell
docker volume rm taskyapi_db_data
```

Logs in streaming di tutti i servizi:
```powershell
docker compose logs -f
```
Logs solo API:
```powershell
docker compose logs -f api
```

## Note sul futuro supporto Bearer
Il passaggio previsto è permettere opzionalmente l'invio del token anche via header:
`Authorization: Bearer <token>` mantenendo per un periodo il body `{"token":"..."}` per retrocompatibilità.

---
Ultimo aggiornamento: 2025-10-08
