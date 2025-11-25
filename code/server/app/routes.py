from marshmallow import ValidationError
from flask import Blueprint, request, jsonify, g
import logging
import time

from .utils import crypt, manager_of_department, manager_required
from .exceptions import ValidationException
from . import manager as manager_module

from .schemas import (
    TaskCreateSchema, ProjectCreateSchema,
    RegisterDipendenteSchema, RegisterManagerSchema,
    LoginSchema, ProjectVisibilitySchema, TaskVisibilitySchema
)
from .schemas import DepartmentCreateSchema
from .repository import (
    insertDipendente, handle_register, handle_login,
    get_projects_by_department, get_tasks_from_project,
    handle_project_by_department, handle_task_by_project,
    get_budget_for_project,get_projects_for_employee, handle_update_project, 
    handle_delete_project, handle_update_task, handle_delete_task
)

from .schemas import ProjectUpdateSchema, ProjectDeleteSchema, TaskUpdateSchema, TaskDeleteSchema
from .schemas import PushRegisterSchema, PushUnregisterSchema, PushTestSchema

bp = Blueprint('api', __name__, url_prefix='/api')
logger = logging.getLogger(__name__)

@bp.before_request
def log_request():
    """Log incoming requests with method, path, and client IP."""
    g.start_time = time.time()
    logger.info(f"Incoming request: {request.method} {request.path} from {request.remote_addr}")

@bp.after_request
def log_response(response):
    """Log outgoing responses with status code and duration."""
    if hasattr(g, 'start_time'):
        duration = time.time() - g.start_time
        logger.info(f"Response: {response.status_code} for {request.method} {request.path} ({duration:.3f}s)")
    return response

@bp.route('/dipendenti/by-project', methods=['POST'])
@manager_of_department('id_dipartimento')
def dipendenti_by_project(manager=None, **kwargs):
    """Restituisce i dipendenti che lavorano su un progetto.

    Input (JSON): token (str), id_progetto (int), per Manager anche id_dipartimento (int)

    Output: 200 JSON { data: { items: [...], count: int, scope: 'all'|'own' } }
    """
    raw = request.get_json() or {}
    try:
        data = TaskVisibilitySchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})

    # delegate to manager layer (enforced by @manager_of_department)
    return manager_module.get_dipendenti_by_project(data)

@bp.route('/dipendenti/data/by-department', methods=['POST'])
@manager_of_department('id_dipartimento')
def getDipendentiDataFromDipartimento(manager=None, **kwargs):
    """Descrizione:
    Restituisce tutti i dati (tutte le colonne) dei dipendenti appartenenti al dipartimento indicato.

    Input (JSON): token (str), id_dipartimento (int)

    Output:
    - 200 JSON { data: { items: [...], count: int } }
    - Errori: MISSING_PARAMS, AUTH_*, DB_ERROR
    """
    raw = request.get_json() or {}
    try:
        data = ProjectVisibilitySchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})

    # delegate to manager layer
    return manager_module.get_dipendenti_data(data)
@bp.route('/dipendenti/by-department', methods=['POST'])
@manager_of_department('id_dipartimento')
def getDipendentiFromDipartimento(manager=None, **kwargs):
    """Descrizione:
    Restituisce tutti i dipendenti appartenenti al dipartimento indicato.

    Input (JSON): token (str), id_dipartimento (int)

    Output:
    - 200 JSON { data: { items: [...], count: int } }
    - Errori: MISSING_PARAMS, AUTH_*, DB_ERROR
    """
    raw = request.get_json() or {}
    try:
        data = ProjectVisibilitySchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})

    # delegate to manager layer
    return manager_module.get_dipendenti_summary(data)

@bp.route('/numeroDipendenti', methods=['POST'])
@manager_of_department('id_dipartimento')
def getNDipendenti(manager=None, **kwargs):
    """Descrizione:
    Restituisce il conteggio dei dipendenti del dipartimento indicato usando
    il campo denormalizzato `numero_dipendenti` della tabella `Dipartimento`.

    Input (JSON): token (str), id_dipartimento (int)

    Output:
    - 200 JSON { data: { n_dipendenti: int } }
    - Errori: MISSING_PARAMS, AUTH_*, DB_ERROR
    """
    raw = request.get_json() or {}
    try:
        data = ProjectVisibilitySchema().load(raw)  # riuso schema (token + id_dipartimento)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})

    # delegate to manager layer
    return manager_module.get_numero_dipendenti(data)

@bp.route('/project/by-department', methods=['POST'])
def project_by_department():
    """Descrizione:
    Restituisce progetti visibili all'utente autenticato per un dipartimento.

    Input (JSON): token (str), id_dipartimento (int)

    Output:
    - 200 JSON { data: { items: [...], count, scope: 'all'|'own' } }
    - Errori: MISSING_PARAMS, AUTH_*.
    """
    raw = request.get_json() or {}
    try:
        data = ProjectVisibilitySchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    
    payload = handle_project_by_department(
        data,
        get_user_by_token_fn=_get_user_by_token,
        get_projects_by_department_fn=get_projects_by_department,
        get_db_connection_fn=_get_db_connection
    )
    return jsonify({"data": payload}), 200


@bp.route('/projects/in-progress', methods=['POST'])
@manager_required
def projects_in_progress(manager=None, **kwargs):
    """Restituisce tutti i progetti attualmente in corso (no input).

    Output: 200 JSON { data: [ <Progetto rows> ] }
    """
    raw = request.get_json() or {}
    from .repository import handle_projects_in_progress
    try:
        payload = handle_projects_in_progress(raw, get_user_by_token_fn=_get_user_by_token, get_db_connection_fn=_get_db_connection)
    except Exception as e:
        # let exceptions bubble through app's error handlers
        raise
    return jsonify({"data": payload}), 200


@bp.route('/task/by-project', methods=['POST'])
def task_by_project():
    """Descrizione:
    Restituisce le task di un progetto secondo il ruolo:
      - Manager: tutte (scope=all) se appartiene al dipartimento fornito.
      - Dipendente: solo le proprie (scope=own).

    Input (JSON): token (str), id_progetto (int); per Manager anche id_dipartimento (int)

    Output:
    - 200 JSON { data: { items: [...], count, scope } }
    - Errori: MISSING_PARAMS, AUTH_*.
    """
    raw = request.get_json() or {}
    try:
        data = TaskVisibilitySchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    
    payload = handle_task_by_project(
        data,
        get_user_by_token_fn=_get_user_by_token,
        get_tasks_from_project_fn=get_tasks_from_project,
        get_db_connection_fn=_get_db_connection
    )
    return jsonify({"data": payload}), 200


@bp.route('/projects/by-dipendente', methods=['POST'])
def projects_by_dipendente():
    """Restituisce i progetti a cui è assegnato un dipendente (input: email_dipendente).

    Input JSON: { email_dipendente: str }
    Output: 200 JSON { data: { items: [...], count: int } }
    """
    raw = request.get_json() or {}
    from .schemas import EmployeeProjectsSchema
    try:
        data = EmployeeProjectsSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    items = get_projects_for_employee(data.get('email_dipendente'), get_db_connection_fn=_get_db_connection)
    return jsonify({"data": {"items": items, "count": len(items)}}), 200


@bp.route('/projects/budget', methods=['POST'])
def projects_budget():
    """Restituisce il budget istanziato di un progetto.

    Input JSON: { id_progetto: int }
    Output: 200 JSON { data: { budget: <decimal> } }
    """
    raw = request.get_json() or {}
    from .schemas import ProjectBudgetSchema
    try:
        data = ProjectBudgetSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    budget = get_budget_for_project(data.get('id_progetto'), get_db_connection_fn=_get_db_connection)
    return jsonify({"data": {"budget": budget}}), 200


@bp.route('/managers/by-project', methods=['POST'])
def managers_by_project():
    """Descrizione:
    Restituisce i manager coinvolti in un progetto.

    Input (JSON): token (str), id_progetto (int); per Manager anche id_dipartimento (int)
    Output: 200 JSON { data: { items: [...], count, scope } }
    """
    raw = request.get_json() or {}
    try:
        data = TaskVisibilitySchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})

    return manager_module.get_managers_by_project(data)

@bp.route('/add/Task', methods=['POST'])
@manager_of_department('id_dipartimento')
def addTask(manager=None, **kwargs):
    raw = request.get_json() or {}
    try:
        data = TaskCreateSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    return manager_module.add_task(data)

@bp.route('/add/Project', methods=['POST'])
@manager_of_department('id_dipartimento')
def addProject(manager=None, **kwargs):
    raw = request.get_json() or {}
    try:
        data = ProjectCreateSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    return manager_module.add_project(data)


@bp.route('/add/Department', methods=['POST'])
@manager_required
def addDepartment(manager=None, **kwargs):
    """Crea un nuovo Dipartimento. Richiede token Manager (manager_required).

    Input (JSON): token (str), nome (str), id_dipartimento (int, opzionale), numero_dipendenti (int, opzionale)
    """
    raw = request.get_json() or {}
    try:
        data = DepartmentCreateSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})

    # delegate to manager layer for the actual insert
    return manager_module.add_department(data)


@bp.route('/login', methods=['POST'])
def login():
    """Descrizione:
    Effettua il login via token già valido oppure coppia email/password.

    Input (JSON):
    - token (str) OR (email (str) & password (str))

    Output:
    - 200 JSON { data: { token, type }, message }
    - Errori: MISSING_CREDENTIALS, INVALID_CREDENTIALS, AUTH_TOKEN_INVALID.
    """
    raw = request.get_json() or {}
    try:
        data = LoginSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_CREDENTIALS", "Validazione login fallita", 400, {"fields": ve.messages})
    return handle_login(data)

def _dipendente_args(d):
    return (
        d.get('email'),
        crypt(d.get('password')),
        d.get('nome'),
        d.get('cognome'),
        d.get('data_nascita'),
        d.get('Dipartimento_id_dipartimento'),
        d.get('sesso'),
        d.get('numero_telefono'),
    )

def _manager_args(d):
    return (
        d.get('email'),
        crypt(d.get('password')),
        d.get('nome'),
        d.get('cognome'),
        d.get('data_nascita'),
        d.get('anni_lavorativi'),
        d.get('Dipartimento_id_dipartimento'),
        d.get('sesso'),
        d.get('numero_telefono'),
    )

@bp.route('/register/dipendente', methods=['POST'])
def register():
    """Descrizione:
    Registra un nuovo Dipendente.

    Input (JSON): email, password, nome, cognome, data_nascita (YYYY-MM-DD), Dipartimento_id_dipartimento (int)

    Output:
    - 201 JSON { data: { token }, message }
    - Errori: MISSING_PARAMS, DUPLICATE_USER, DB_INTEGRITY_ERROR.
    """
    raw = request.get_json() or {}
    try:
        data = RegisterDipendenteSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    
    return handle_register(
        data,
        insert_func=insertDipendente,
        args_builder=_dipendente_args,
        success_message='User registered successfully'
    )

@bp.route('/register/manager', methods=['POST'])
def register_manager():
    """Descrizione:
    Registra un nuovo Manager.

    Input (JSON): email, password, nome, cognome, data_nascita, anni_lavorativi, Dipartimento_id_dipartimento

    Output:
    - 201 JSON { data: { token }, message }
    - Errori: MISSING_PARAMS, DUPLICATE_USER, DB_INTEGRITY_ERROR.
    """
    raw = request.get_json() or {}
    try:
        data = RegisterManagerSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})

    # delegate to manager layer
    return manager_module.register_manager(data)

def _get_user_by_token(token):
    from .repository import get_user_by_token
    return get_user_by_token(token)

def _get_db_connection():
    from .db import get_db_connection
    return get_db_connection()


# -------------------- Push register/unregister -------------------- #

@bp.route('/push/register', methods=['POST'])
def push_register():
    raw = request.get_json() or {}
    try:
        data = PushRegisterSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    user = _get_user_by_token(data.get('token'))
    if not user:
        from .exceptions import AuthException
        raise AuthException("AUTH_TOKEN_INVALID", "Token non valido", 403)
    email = (user.get('user') or {}).get('email')
    role = user.get('type')
    platform = data.get('platform') or 'android'
    from .repository import upsert_push_subscription
    upsert_push_subscription(email=email, role=role, platform=platform, fcm_token=data.get('fcm_token'))
    return jsonify({"message": "Push token registrato"}), 200


@bp.route('/push/unregister', methods=['POST'])
def push_unregister():
    raw = request.get_json() or {}
    try:
        data = PushUnregisterSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    user = _get_user_by_token(data.get('token'))
    if not user:
        from .exceptions import AuthException
        raise AuthException("AUTH_TOKEN_INVALID", "Token non valido", 403)
    from .repository import delete_push_subscription
    delete_push_subscription(data.get('fcm_token'))
    return jsonify({"message": "Push token rimosso"}), 200


@bp.route('/debug/snapshot', methods=['GET'])
def debug_snapshot():
    """Descrizione:
    Fornisce uno snapshot delle principali tabelle per debug locale.

    Input: (none) GET

    Output:
    - 200 JSON { data: { <TabName>: [...] }, message }
    - Note sicurezza: da non esporre in produzione (contiene token).
    """
    tables = [
        ("Dipartimento", "SELECT * FROM Dipartimento ORDER BY id_dipartimento"),
        ("Manager", "SELECT email,nome,cognome,data_nascita,sesso,numero_telefono,anni_lavorativi,token,Dipartimento_id_dipartimento FROM Manager ORDER BY email"),
        ("Dipendente", "SELECT email,nome,cognome,data_nascita,sesso,numero_telefono,token,Dipartimento_id_dipartimento FROM Dipendente ORDER BY email"),
        ("Progetto", "SELECT * FROM Progetto ORDER BY id_progetto"),
        ("TASK", "SELECT * FROM TASK ORDER BY id"),
        ("push_subscriptions", "SELECT * FROM push_subscriptions ORDER BY id"),
    ]
    conn = _get_db_connection()
    snapshot = {}
    try:
        with conn.cursor() as cursor:
            for name, sql in tables:
                cursor.execute(sql)
                snapshot[name] = cursor.fetchall() or []
        return jsonify({"data": snapshot, "message": "Snapshot OK"}), 200
    finally:
        conn.close()


@bp.route('/debug/push/test', methods=['POST'])
def debug_push_test():
    """DEV: invia una notifica di test ad un'email registrata o direttamente ad un fcm_token.

    Input JSON: { token: str, email?: str, fcm_token?: str, title?: str, body?: str, data?: {..} }
    Requisiti: almeno uno tra email e fcm_token.
    """
    raw = request.get_json() or {}
    try:
        data = PushTestSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})

    user = _get_user_by_token(data.get('token'))
    if not user:
        from .exceptions import AuthException
        raise AuthException("AUTH_TOKEN_INVALID", "Token non valido", 403)

    tokens = []
    if data.get('email'):
        from .repository import get_tokens_for_users
        tokens = get_tokens_for_users([data['email']])
    if data.get('fcm_token'):
        tokens = list(set(tokens + [data['fcm_token']]))

    if not tokens:
        return jsonify({"message": "Nessun token trovato"}), 200

    from .notifications import send_to_tokens
    try:
        result = send_to_tokens(tokens, data.get('title') or 'Test', data.get('body') or 'Hello', data.get('data') or {})
    except Exception as e:
        # Non propagare: mostra info utili in DEV
        return jsonify({
            "data": {"tokens": tokens},
            "error": {"code": "FCM_SEND_FAILED", "message": str(e)[:500]}
        }), 200

    # Pulizia token invalidi
    invalid = result.get('invalid') or []
    if invalid:
        from .repository import delete_push_subscription
        for t in invalid:
            delete_push_subscription(t)

    return jsonify({"data": {"tokens": tokens, "result": result}}), 200


@bp.route('/debug/push/status', methods=['GET'])
def debug_push_status():
    import os
    cred_path = os.environ.get('FCM_CREDENTIALS_PATH')
    exists = bool(cred_path and os.path.exists(cred_path))
    try:
        from .notifications import is_ready
        ready = is_ready()
    except Exception:
        ready = False
    return jsonify({
        "data": {
            "FCM_CREDENTIALS_PATH": cred_path,
            "credentials_exists": exists,
            "initialized": bool(ready)
        }
    }), 200

@bp.route('/debug/scheduler/check-overdue', methods=['POST'])
@manager_required
def debug_scheduler_check_overdue(manager=None, **kwargs):
    """Debug endpoint to manually trigger scheduler check for overdue tasks.
    
    In production, this runs automatically at midnight via APScheduler.
    This endpoint allows testing the scheduler logic without waiting.
    """
    try:
        from .scheduler import check_overdue_tasks
        check_overdue_tasks()
        return jsonify({
            "message": "Scheduler check completed",
            "note": "In production, this runs automatically daily at 00:00"
        }), 200
    except Exception as e:
        return jsonify({
            "error": {
                "code": "SCHEDULER_ERROR",
                "message": str(e)
            }
        }), 500

@bp.route('/debug/logs', methods=['GET'])
@manager_required
def debug_logs(manager=None, **kwargs):
    """Debug endpoint to view recent log entries.
    
    Query parameters:
    - file: Log file to read (app, error, scheduler) - default: app
    - lines: Number of recent lines to retrieve - default: 100
    - level: Filter by log level (DEBUG, INFO, WARNING, ERROR, CRITICAL) - optional
    
    Returns: JSON array of log entries with parsed fields
    """
    from .logging_config import get_recent_logs
    import os
    
    # Parse query parameters
    log_file = request.args.get('file', 'app')
    lines = int(request.args.get('lines', 100))
    level_filter = request.args.get('level')
    
    # Map file parameter to actual log file path
    log_file_map = {
        'app': 'logs/app.log',
        'error': 'logs/error.log',
        'scheduler': 'logs/scheduler.log'
    }
    
    if log_file not in log_file_map:
        return jsonify({
            "error": {
                "code": "INVALID_LOG_FILE",
                "message": f"Invalid log file. Choose from: {', '.join(log_file_map.keys())}"
            }
        }), 400
    
    file_path = log_file_map[log_file]
    
    # Check if file exists
    if not os.path.exists(file_path):
        return jsonify({
            "data": {
                "entries": [],
                "message": f"Log file {log_file}.log does not exist yet"
            }
        }), 200
    
    try:
        entries = get_recent_logs(file_path, lines, level_filter)
        return jsonify({
            "data": {
                "file": log_file,
                "total_entries": len(entries),
                "level_filter": level_filter,
                "entries": entries
            }
        }), 200
    except Exception as e:
        logger.error(f"Failed to retrieve logs from {file_path}: {e}")
        return jsonify({
            "error": {
                "code": "LOG_RETRIEVAL_ERROR",
                "message": str(e)
            }
        }), 500

# -------------------- Update / Delete Endpoints -------------------- #

@bp.route('/update/Project', methods=['POST'])
@manager_of_department('id_dipartimento')
def update_project(manager=None, **kwargs):
    raw = request.get_json() or {}
    try:
        data = ProjectUpdateSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    payload = handle_update_project(data)
    return jsonify({"data": payload}), 200

@bp.route('/delete/Project', methods=['POST'])
@manager_of_department('id_dipartimento')
def delete_project(manager=None, **kwargs):
    raw = request.get_json() or {}
    try:
        data = ProjectDeleteSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    project_row = handle_delete_project(data)
    return jsonify({"data": project_row, "message": "Progetto eliminato"}), 200

@bp.route('/update/Task', methods=['POST'])
@manager_of_department('id_dipartimento')
def update_task(manager=None, **kwargs):
    raw = request.get_json() or {}
    try:
        data = TaskUpdateSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    payload = handle_update_task(data)
    return jsonify({"data": payload}), 200

@bp.route('/delete/Task', methods=['POST'])
@manager_of_department('id_dipartimento')
def delete_task(manager=None, **kwargs):
    raw = request.get_json() or {}
    try:
        data = TaskDeleteSchema().load(raw)
    except ValidationError as ve:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": ve.messages})
    payload = handle_delete_task(data)
    return jsonify({"data": payload, "message": "Task eliminata"}), 200