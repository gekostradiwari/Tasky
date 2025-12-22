"""
Scheduler module for time-triggered background tasks.

This module implements the Time-Triggered control style described in RAD section 5.1.B.
Responsibilities:
- Monitor task deadlines (data_fine) and automatically suspend overdue tasks
- Execute scheduled jobs at regular intervals without blocking HTTP requests
"""

from apscheduler.schedulers.background import BackgroundScheduler
from datetime import datetime
import logging

logger = logging.getLogger(__name__)


def check_overdue_tasks():
    """
    Time-triggered job: checks for overdue tasks and sets their status to 'Sospeso'.
    
    Execution: Daily at midnight (00:00).
    Logic:
    - Query all tasks where data_fine < today AND stato NOT IN ('Sospeso', 'Completato')
    - For each overdue task, update stato = 'Sospeso'
    - Log actions for monitoring
    
    This implements the automatic deadline monitoring requirement from RAD.
    """
    from .db import get_db_connection
    
    today = datetime.now().date()
    logger.info(f"[SCHEDULER] Avvio controllo scadenze task per {today}")
    
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            # Find overdue tasks (deadline passed, not already suspended/completed)
            query = """
                SELECT id, nome, data_fine, stato, Progetto_id_progetto
                FROM TASK
                WHERE data_fine < %s
                AND stato NOT IN ('Sospeso', 'Completato')
            """
            cursor.execute(query, (today,))
            overdue_tasks = cursor.fetchall()
            
            if not overdue_tasks:
                logger.info("[SCHEDULER] Nessuna task scaduta trovata")
                return
            
            logger.info(f"[SCHEDULER] Trovate {len(overdue_tasks)} task scadute")
            
            # Update each overdue task to 'Sospeso'
            update_query = "UPDATE TASK SET stato = 'Sospeso' WHERE id = %s"
            suspended_count = 0
            
            for task in overdue_tasks:
                try:
                    cursor.execute(update_query, (task['id'],))
                    suspended_count += 1
                    logger.info(
                        f"[SCHEDULER] Task {task['id']} '{task['nome']}' "
                        f"(progetto {task['Progetto_id_progetto']}) "
                        f"sospesa - scadenza: {task['data_fine']}"
                    )
                except Exception as e:
                    logger.error(f"[SCHEDULER] Errore aggiornamento task {task['id']}: {e}")
            
            conn.commit()
            logger.info(f"[SCHEDULER] Completato: {suspended_count}/{len(overdue_tasks)} task sospese")
            
    except Exception as e:
        logger.error(f"[SCHEDULER] Errore durante controllo scadenze: {e}")
        conn.rollback()
    finally:
        conn.close()


def init_scheduler(app):
    """
    Initialize and start the background scheduler.
    
    Registers time-triggered jobs and starts the scheduler in daemon mode.
    The scheduler runs in a separate thread and does not block the main application.
    
    Args:
        app: Flask application instance (for logger configuration)
    """
    scheduler = BackgroundScheduler(daemon=True)
    
    # Schedule deadline monitoring: daily at midnight
    scheduler.add_job(
        func=check_overdue_tasks,
        trigger="cron",
        hour=0,
        minute=0,
        id="check_overdue_tasks",
        name="Controllo scadenze task",
        replace_existing=True
    )
    
    scheduler.start()
    app.logger.info("[SCHEDULER] Scheduler avviato - controllo scadenze giornaliero alle 00:00")
    
    # Cleanup on shutdown
    import atexit
    atexit.register(lambda: scheduler.shutdown())
