#!/usr/bin/env python
"""
Script di seed SOLO PER TEST (non esposto via endpoint).

Crea N dipartimenti aggiuntivi (oltre a quelli esistenti), per ognuno crea M progetti
ed ogni progetto riceve K task. Non rende idempotente: ogni esecuzione aggiunge dati.

Uso (dentro al container API oppure con le variabili d'ambiente corrette):
    python scripts/seed_test_data.py --departments 2 --projects 3 --tasks 4

Di default: 2 dipartimenti, 2 progetti per dipartimento, 2 task per progetto.

Richiede che le variabili d'ambiente di connessione (ad es. host, user, password, db)
coincidano con quelle usate da app.db.get_db_connection oppure che il modulo app.db
non usi parametri fissi.
"""
import argparse
import random
import sys
import os
from datetime import date

# Bootstrap PYTHONPATH so that 'app' package is importable when script is run directly.
CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(CURRENT_DIR, '..'))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

try:
    from app.repository import insertProject, insertTask
    from app.db import get_db_connection
except ModuleNotFoundError as e:
    print("[seed_test_data] ERRORE: impossibile importare il package 'app'.")
    print("Suggerimenti:")
    print("  1. Assicurati che la directory 'app' sia nella root del progetto montata nel container.")
    print("  2. Verifica il Dockerfile: WORKDIR dovrebbe essere /app e copiarsi la cartella app/.")
    print("  3. Esegui dal tuo host: docker compose exec api ls -1 /app")
    print("  4. Se vedi un path diverso, aggiungi PYTHONPATH=/app nell'env del container.")
    print(f"Dettagli eccezione: {e}\n")
    print("[DEBUG] sys.path attuale:")
    for p in sys.path:
        print("   ", p)
    print("[DEBUG] CWD nel container:", os.getcwd())
    # Prova a mostrare contenuto /app se esiste
    maybe_root = '/app'
    if os.path.isdir(maybe_root):
        print("[DEBUG] Contenuto /app:")
        try:
            for name in os.listdir(maybe_root):
                print("   ", name)
        except Exception as ie:
            print("   (errore listdir /app)", ie)
    sys.exit(1)


def create_department(nome: str, numero_dipendenti: int = 0) -> int:
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "INSERT INTO Dipartimento (nome, numero_dipendenti) VALUES (%s, %s)",
                (nome, numero_dipendenti),
            )
            dep_id = cursor.lastrowid
        conn.commit()
        return dep_id
    finally:
        conn.close()


def seed(departments: int, projects_per_dep: int, tasks_per_project: int):
    deps_created = []
    projs_created = []
    tasks_created = []

    for d in range(departments):
        dep_name = f"DeptSeed_{random.randint(1000,9999)}_{d}"
        dep_id = create_department(dep_name, 0)
        deps_created.append({"id_dipartimento": dep_id, "nome": dep_name})

        for p in range(projects_per_dep):
            descr = f"Seed project {p} for {dep_name}"
            budget = round(random.uniform(1000, 5000), 2)
            nome = f"ProjSeed_{dep_id}_{p}"
            data_inizio = date(2025, 1, 1).isoformat()
            data_fine = date(2025, 12, 31).isoformat()
            proj_id = insertProject(None, descr, budget, nome, data_inizio, data_fine, dep_id)
            projs_created.append({"id_progetto": proj_id, "dipartimento": dep_id})

            for t in range(tasks_per_project):
                stato = "Open"
                task_descr = f"Task {t} for project {proj_id}"
                data_in = date(2025, 2, 1).isoformat()
                data_out = date(2025, 3, 1).isoformat()
                task_id = insertTask(None, stato, task_descr, data_in, data_out, proj_id, None, None)
                tasks_created.append({"id_task": task_id, "progetto": proj_id})

    return {
        "departments": deps_created,
        "projects": projs_created,
        "tasks": tasks_created,
    }


def main():
    parser = argparse.ArgumentParser(description="Seed di test (solo sviluppo)")
    parser.add_argument("--departments", type=int, default=2)
    parser.add_argument("--projects", type=int, default=2, help="Progetti per dipartimento")
    parser.add_argument("--tasks", type=int, default=2, help="Task per progetto")
    args = parser.parse_args()

    summary = seed(args.departments, args.projects, args.tasks)
    print("Seed completato:")
    print(f"  Dipartimenti creati: {len(summary['departments'])}")
    print(f"  Progetti creati: {len(summary['projects'])}")
    print(f"  Task creati: {len(summary['tasks'])}")

    # Mostra primi elementi a scopo riassunto
    if summary['departments']:
        print("  Esempio dipartimento:", summary['departments'][0])
    if summary['projects']:
        print("  Esempio progetto:", summary['projects'][0])
    if summary['tasks']:
        print("  Esempio task:", summary['tasks'][0])

if __name__ == "__main__":
    main()
