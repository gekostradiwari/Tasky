# Panoramica

Questa sezione riassume gli obiettivi del progetto TaskyAPI e rimanda al file `README.md` per una guida pratica.

## Obiettivi
- Gestione utenti (Manager e Dipendenti)
- Creazione e visibilità di Progetti e Task con logica di scope
- Endpoint di diagnostica (`/health`) e snapshot database per sviluppo
- Interfaccia tester HTML per prove manuali

## Struttura Codice
- `api.py` / `app/__init__.py`: bootstrap dell'app Flask
- `app/routes.py`: definizione endpoint HTTP
- `app/repository.py`: logica di accesso e manipolazione dati
- `app/utils.py`: helper, decorators, gestione errori custom
- `app/schemas.py`: validazione e serializzazione (Marshmallow)
- `app/error_handlers.py`: traduzione eccezioni in risposta JSON unificata
- `app/tester/`: UI statica per test manuali

## Documentazione API
Le specifiche endpoint complete, esempi di richiesta/risposta e codici d'errore sono nel `README.md` e progressivamente verranno migrate qui.

## Prossimi Passi Documentazione
1. Aggiungere pagina dedicata agli endpoint con tabelle parametri
2. Integrare esempi di flusso (es. registrazione + creazione progetto + task)
3. Generare OpenAPI e linkarla
4. Pubblicare la documentazione su GitHub Pages / Read the Docs
