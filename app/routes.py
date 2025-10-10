from marshmallow import ValidationError
from flask import Blueprint, request, jsonify

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

bp = Blueprint('api', __name__, url_prefix='/api')

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
        ("Manager", "SELECT email,nome,cognome,data_nascita,anni_lavorativi,token,Dipartimento_id_dipartimento FROM Manager ORDER BY email"),
        ("Dipendente", "SELECT email,nome,cognome,data_nascita,token,Dipartimento_id_dipartimento FROM Dipendente ORDER BY email"),
        ("Progetto", "SELECT * FROM Progetto ORDER BY id_progetto"),
        ("TASK", "SELECT * FROM TASK ORDER BY id"),
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