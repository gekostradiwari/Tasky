Endpoint API
===========

Panoramica completa degli endpoint esposti da TaskyAPI. Questa pagina sintetizza parametri richiesti, risposta e codici errore comuni. Per i dettagli esaustivi vedere il file `API_DOCUMENTATION.md` nel repository.

Legenda colonne:
- Metodo/Path: Verbo HTTP e percorso relativo al prefisso /api
- Ruolo: ruolo minimo richiesto (Manager, Dipendente, Pubblico)
- Parametri Body: campi JSON obbligatori (quelli tra parentesi sono opzionali)
- Parametri Query: chiavi querystring (se usate)
- Output principale: struttura dati di ritorno (semplificata)
- Errori chiave: codici errore principali (vedi tabella codici per elenco completo)

.. list-table:: Endpoint principali
   :widths: 20 10 25 10 20 15
   :header-rows: 1

   * - Metodo/Path
     - Ruolo
     - Parametri Body
     - Query
     - Output principale
     - Errori chiave
   * - POST /register/dipendente
     - Pubblico
     - email, password, nome, cognome, data_nascita, Dipartimento_id_dipartimento
     - –
     - data.token
     - MISSING_PARAMS, DUPLICATE_USER
   * - POST /register/manager
     - Pubblico
     - email, password, nome, cognome, data_nascita, anni_lavorativi, Dipartimento_id_dipartimento
     - –
     - data.token
     - MISSING_PARAMS, DUPLICATE_USER
   * - POST /login
     - Pubblico
     - token | (email, password)
     - –
     - data.token, data.type
     - MISSING_CREDENTIALS, INVALID_CREDENTIALS, AUTH_TOKEN_INVALID
   * - POST /add/Department
     - Manager
     - token, nome, (id_dipartimento), (numero_dipendenti)
     - –
     - data.id_dipartimento
     - AUTH_*, MISSING_PARAMS, DUPLICATE_DEPARTMENT
   * - POST /numeroDipendenti
     - Manager
     - token, id_dipartimento
     - –
     - data.n_dipendenti
     - AUTH_*, MISSING_PARAMS
   * - POST /dipendenti/by-department
     - Manager
     - token, id_dipartimento
     - –
     - data.items[], data.count
     - AUTH_*, MISSING_PARAMS
   * - POST /dipendenti/data/by-department
     - Manager
     - token, id_dipartimento
     - –
     - data.items[], data.count
     - AUTH_*, MISSING_PARAMS
   * - POST /add/Project
     - Manager
     - token, descrizione, budget, nome, data_inizio, data_fine, id_dipartimento, (id_progetto)
     - –
     - data.id_progetto
     - AUTH_*, MISSING_PARAMS, DUPLICATE_PROJECT
   * - POST /update/Project
     - Manager
     - token, id_progetto, id_dipartimento, <≥1 campo aggiornabile>
     - –
     - data.<progetto>
     - AUTH_*, NOT_FOUND, MISSING_PARAMS
   * - POST /delete/Project
     - Manager
     - token, id_progetto, id_dipartimento
     - –
     - data.<progetto>
     - AUTH_*, NOT_FOUND, MISSING_PARAMS
   * - POST /project/by-department
     - Manager/Dipendente
     - token, id_dipartimento
     - –
     - data.items[], count, scope
     - AUTH_*, MISSING_PARAMS
   * - POST /projects/in-progress
     - Manager
     - token
     - –
     - data.items[], count, scope
     - AUTH_*
   * - POST /projects/by-dipendente
     - Pubblico
     - email_dipendente
     - –
     - data.items[], count
     - MISSING_PARAMS
   * - POST /projects/budget
     - Pubblico
     - id_progetto
     - –
     - data.budget
     - MISSING_PARAMS
   * - POST /tasks/suspended
     - Manager/Dipendente
     - token, email_dipendente
     - –
     - data.items[], count
     - AUTH_*, MISSING_PARAMS
   * - POST /tasks/completed
     - Manager/Dipendente
     - token, email_dipendente
     - –
     - data.items[], count
     - AUTH_*, MISSING_PARAMS
   * - POST /tasks/in-progress
     - Manager/Dipendente
     - token, email_dipendente
     - –
     - data.items[], count
     - AUTH_*, MISSING_PARAMS
   * - POST /add/Task
     - Manager
     - token, nome, stato, descrizione, data_inizio, data_fine, id_progetto, id_dipartimento, email_dipendente, email_manager, (id)
     - –
     - data.id_task
     - AUTH_*, MISSING_PARAMS, DB_INTEGRITY_ERROR
   * - POST /update/Task
     - Manager
     - token, id, id_dipartimento, <≥1 campo aggiornabile>
     - –
     - data.items[], count
     - AUTH_*, NOT_FOUND, MISSING_PARAMS
   * - POST /delete/Task
     - Manager
     - token, id, id_dipartimento
     - –
     - data.items[], count
     - AUTH_*, NOT_FOUND, MISSING_PARAMS
   * - POST /update/Task/Status
     - Dipendente
     - token, id, stato
     - –
     - data.id, data.stato
     - AUTH_FORBIDDEN_ACCESS, NOT_FOUND, MISSING_PARAMS
   * - POST /task/by-project
     - Manager/Dipendente
     - token, id_progetto, (id_dipartimento*)
     - –
     - data.items[], count, scope
     - AUTH_*, MISSING_PARAMS
   * - POST /dipendenti/by-project
     - Manager
     - token, id_progetto, id_dipartimento
     - –
     - data.items[], count, scope
     - AUTH_*, MISSING_PARAMS
   * - POST /managers/by-project
     - Manager/Dipendente
     - token, id_progetto, (id_dipartimento*)
     - –
     - data.items[], count, scope
     - AUTH_*, MISSING_PARAMS
   * - POST /push/register
     - Autenticato
     - token, fcm_token, (platform)
     - –
     - message
     - AUTH_*, MISSING_PARAMS
   * - POST /push/unregister
     - Autenticato
     - token, fcm_token
     - –
     - message
     - AUTH_*, MISSING_PARAMS

Esempi JSON
===========

Linee guida:
* I token sono mostrati come placeholder (`<TOKEN_MANAGER>`, `<TOKEN_DIPENDENTE>`).
* Le date sono nel formato `dd/MM/yyyy`.
* Il budget è restituito come stringa con due decimali.
* Gli esempi mostrano solo i campi principali (possono essercene altri nelle tabelle del DB o output più esteso in futuro).

---
**1. Registrazione Dipendente**  
Request:
.. code-block:: json

  {
    "email": "dip@example.com",
    "password": "Secret123",
    "nome": "Luca",
    "cognome": "Bianchi",
    "data_nascita": "20/04/1995",
    "Dipartimento_id_dipartimento": 1
  }
Response 201:
.. code-block:: json

  { "message": "User registered successfully", "data": { "token": "<TOKEN_DIPENDENTE>" } }

**2. Registrazione Manager**  
Request:
.. code-block:: json

  {
    "email": "mgr@example.com",
    "password": "Secret123",
    "nome": "Marco",
    "cognome": "Verdi",
    "data_nascita": "10/02/1988",
    "anni_lavorativi": 5,
    "Dipartimento_id_dipartimento": 1
  }
Response 201 (identica shape):
.. code-block:: json

  { "message": "User registered successfully", "data": { "token": "<TOKEN_MANAGER>" } }

**3. Login (con credenziali)**  
Request:
.. code-block:: json

  { "email": "mgr@example.com", "password": "Secret123" }
Response 200:
.. code-block:: json

  { "message": "Login effettuato", "data": { "token": "<TOKEN_MANAGER>", "type": "Manager", "email": "mgr@example.com", "id_dipartimento": 1, "sesso": "M" } }

**4. Login (con token)**  
Request:
.. code-block:: json

  { "token": "<TOKEN_MANAGER>" }
Response 200 (stessa struttura):
.. code-block:: json

  { "message": "Login effettuato", "data": { "token": "<TOKEN_MANAGER>", "type": "Manager", "email": "mgr@example.com", "id_dipartimento": 1, "sesso": "M" } }

**5. Creazione Dipartimento**  
Request:
.. code-block:: json

  {
    "token": "<TOKEN_MANAGER>",
    "nome": "IT",
    "id_dipartimento": 2,
    "numero_dipendenti": 0
  }
Response 201:
.. code-block:: json

  { "message": "Dipartimento creato correttamente", "data": { "id_dipartimento": 2 } }

**6. Numero Dipendenti Dipartimento**  
Request:
.. code-block:: json

  { "token": "<TOKEN_MANAGER>", "id_dipartimento": 1 }
Response 200:
.. code-block:: json

  { "data": { "n_dipendenti": 5 } }

**7. Dipendenti (summary) by Department**  
Request:
.. code-block:: json

  { "token": "<TOKEN_MANAGER>", "id_dipartimento": 1 }
Response 200:
.. code-block:: json

  {
    "data": {
     "items": [ { "email": "dip@example.com", "nome": "Luca", "cognome": "Bianchi", "data_nascita": "20/04/1995" } ],
     "count": 1
    }
  }

**8. Dipendenti (data estesa) by Department**  
Request identica (#7).  
Response 200 (campi aggiuntivi, esempio):
.. code-block:: json

  {
    "data": {
     "items": [ {
      "email": "dip@example.com",
      "nome": "Luca",
      "cognome": "Bianchi",
      "data_nascita": "20/04/1995",
      "Dipartimento_id_dipartimento": 1
     }],
     "count": 1
    }
  }

**9. Creazione Progetto**  
Request:
.. code-block:: json

  {
    "token": "<TOKEN_MANAGER>",
    "descrizione": "Onboarding piattaforma",
    "budget": 15000.5,
    "nome": "Onboarding",
    "data_inizio": "15/01/2025",
    "data_fine": "30/06/2025",
    "id_dipartimento": 1
  }
Response 201:
.. code-block:: json

  { "message": "Progetto inserito correttamente", "data": { "id_progetto": 101 } }

**10. Update Progetto**  
Request (almeno un campo aggiornabile):
.. code-block:: json

  { "token": "<TOKEN_MANAGER>", "id_progetto": 101, "id_dipartimento": 1, "descrizione": "Aggiornata" }
Response 200:
.. code-block:: json

  {
    "data": {
     "id_progetto": 101,
     "nome": "Onboarding",
     "descrizione": "Aggiornata",
     "budgetIstanziato": "15000.50",
     "dataInizio": "15/01/2025",
     "dataFine": "30/06/2025",
     "Dipartimento_id_dipartimento": 1
    }
  }

**11. Delete Progetto**  
Request:
.. code-block:: json

  { "token": "<TOKEN_MANAGER>", "id_progetto": 101, "id_dipartimento": 1 }
Response 200:
.. code-block:: json

  { "data": { "id_progetto": 101, "nome": "Onboarding" }, "message": "Progetto eliminato" }

**12. Progetti by Department (Manager)**  
Request:
.. code-block:: json

  { "token": "<TOKEN_MANAGER>", "id_dipartimento": 1 }
Response 200:
.. code-block:: json

  { "data": { "items": [ { "id_progetto": 101, "nome": "Onboarding", "budgetIstanziato": "15000.50" } ], "count": 1, "scope": "all" } }

**13. Progetti by Department (Dipendente)**  
Request:
.. code-block:: json

  { "token": "<TOKEN_DIPENDENTE>", "id_dipartimento": 1 }
Response 200 (solo progetti con sue task):
.. code-block:: json

  { "data": { "items": [ { "id_progetto": 101, "nome": "Onboarding" } ], "count": 1, "scope": "own" } }

**14. Progetti In Progress**  
Request:
.. code-block:: json

  { "token": "<TOKEN_MANAGER>" }
Response 200:
.. code-block:: json

  { "data": { "items": [ { "id_progetto": 101, "nome": "Onboarding" } ], "count": 1, "scope": "all" } }

**15. Progetti di un Dipendente**  
Request (pubblico):
.. code-block:: json

  { "email_dipendente": "dip@example.com" }
Response 200:
.. code-block:: json

  { "data": { "items": [ { "id_progetto": 101, "nome": "Onboarding" } ], "count": 1 } }

**16. Budget Progetto**  
Request (pubblico):
.. code-block:: json

  { "id_progetto": 101 }
Response 200:
.. code-block:: json

  { "data": { "budget": "15000.50" } }

**17. Creazione Task**  
Request:
.. code-block:: json

  {
    "token": "<TOKEN_MANAGER>",
    "nome": "Setup ambiente",
    "stato": "Open",
    "descrizione": "Configurazione iniziale ambiente di sviluppo",
    "data_inizio": "01/02/2025",
    "data_fine": "15/02/2025",
    "id_progetto": 101,
    "id_dipartimento": 1,
    "email_dipendente": "dip@example.com",
    "email_manager": "mgr@example.com"
  }
Response 201:
.. code-block:: json

  { "data": { "id_task": 7 }, "message": "Task inserita correttamente" }

**18. Update Task**  
Request (almeno un campo aggiornabile: nome, stato, descrizione, data_inizio, data_fine, email_dipendente, email_manager):
.. code-block:: json

  { "token": "<TOKEN_MANAGER>", "id": 7, "id_dipartimento": 1, "stato": "InProgress" }
Response 200:
.. code-block:: json

  { "data": { "items": [ { "id": 7, "stato": "InProgress", "descrizione": "Setup ambiente" } ], "count": 1 } }

**19. Delete Task**  
Request:
.. code-block:: json

  { "token": "<TOKEN_MANAGER>", "id": 7, "id_dipartimento": 1 }
Response 200:
.. code-block:: json

  { "data": { "items": [], "count": 0 }, "message": "Task eliminata" }

**20. Update Task Status (Dipendente)**
Request:
.. code-block:: json

  { "token": "<TOKEN_DIPENDENTE>", "id": 7, "stato": "Completed" }
Response 200:
.. code-block:: json

  { "data": { "id": 7, "stato": "Completed", "nome": "Setup ambiente", ... }, "message": "Success" }

**21. Task by Project (Manager)**  
Request:
.. code-block:: json

  { "token": "<TOKEN_MANAGER>", "id_progetto": 101, "id_dipartimento": 1 }
Response 200:
.. code-block:: json

  { "data": { "items": [ { "id": 7, "stato": "Open" } ], "count": 1, "scope": "all" } }

**22. Task by Project (Dipendente)**  
Request:
.. code-block:: json

  { "token": "<TOKEN_DIPENDENTE>", "id_progetto": 101 }
Response 200:
.. code-block:: json

  { "data": { "items": [ { "id": 7, "stato": "Open" } ], "count": 1, "scope": "own" } }

**23. Dipendenti by Project**  
Request:
.. code-block:: json

  { "token": "<TOKEN_MANAGER>", "id_progetto": 101, "id_dipartimento": 1 }
Response 200:
.. code-block:: json

  { "data": { "items": [ { "email": "dip@example.com" } ], "count": 1, "scope": "all" } }

**24. Managers by Project (Manager)**  
Request:
.. code-block:: json

  { "token": "<TOKEN_MANAGER>", "id_progetto": 101, "id_dipartimento": 1 }
Response 200:
.. code-block:: json

  { "data": { "items": [ { "email": "mgr@example.com" } ], "count": 1, "scope": "all" } }

**25. Managers by Project (Dipendente)**  
Request:
.. code-block:: json

  { "token": "<TOKEN_DIPENDENTE>", "id_progetto": 101 }
Response 200 (visibilità own -> potrebbe essere vuoto o limitato):
.. code-block:: json

  { "data": { "items": [], "count": 0, "scope": "own" } }

**26. Suspended Tasks (Manager/Dipendente)**
Request:
.. code-block:: json

  { "token": "<TOKEN>", "email_dipendente": "dip@example.com" }
Response 200:
.. code-block:: json

  { "data": { "items": [ { "id": 30, "stato": "Sospeso", "nome": "Overdue Task", "data_fine": "01/01/2024" } ], "count": 1 } }

**27. Completed Tasks (Manager/Dipendente)**
Request:
.. code-block:: json

  { "token": "<TOKEN>", "email_dipendente": "dip@example.com" }
Response 200:
.. code-block:: json

  { "data": { "items": [ { "id": 31, "stato": "Completato", "nome": "Done Task", "data_fine": "10/01/2025" } ], "count": 1 } }

**28. In Progress Tasks (Manager/Dipendente)**
Request:
.. code-block:: json

  { "token": "<TOKEN>", "email_dipendente": "dip@example.com" }
Response 200:
.. code-block:: json

  { "data": { "items": [ { "id": 32, "stato": "InProgress", "nome": "Active Task", "data_fine": "20/02/2025" } ], "count": 1 } }

---
**29. Push Register**  
Request:
.. code-block:: json

  { "token": "<TOKEN_AUTH>", "fcm_token": "<FCM_TOKEN>", "platform": "android" }
Response 200:
.. code-block:: json

  { "message": "Push token registrato" }

**30. Push Unregister**  
Request:
.. code-block:: json

  { "token": "<TOKEN_AUTH>", "fcm_token": "<FCM_TOKEN>" }
Response 200:
.. code-block:: json

  { "message": "Push token rimosso" }

**31. (DEV) Debug Push Test**  
Nota: endpoint di sviluppo, non esporre in produzione.
Request (una tra email o fcm_token):
.. code-block:: json

  { "token": "<TOKEN_AUTH>", "email": "user@example.com", "title": "Ping", "body": "Hello" }

Response 200 (successo o errore incapsulato):
.. code-block:: json

  { "data": { "tokens": ["<FCM_TOKEN>", "..."] , "result": { "success": 1, "failure": 0, "invalid": [] } } }

Oppure, in caso di errore invio FCM (sempre 200 per test):
.. code-block:: json

  { "data": { "tokens": ["<FCM_TOKEN>"] }, "error": { "code": "FCM_SEND_FAILED", "message": "..." } }

---
Fine esempi.
