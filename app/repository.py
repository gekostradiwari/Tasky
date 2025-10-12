from .utils import (
    generateToken, integrity_errors_project, integrity_errors,
    sql_insert_helper, sql_select_helper, sql_update_helper, sql_delete_helper
)
from .exceptions import ServerException, ValidationException, AuthException
from .db import get_db_connection
from flask import jsonify
from datetime import date, datetime
import threading


def _fmt_date(val):
    """Return val formatted as YYYY-MM-DD if it's a date/datetime, else unchanged."""
    if isinstance(val, (date, datetime)):
        return val.strftime('%Y-%m-%d')
    return val

def _format_items_dates(items, date_keys):
    """Format specific date keys inside list[dict] in-place (returns same list)."""
    if not items:
        return items
    for it in items:
        for k in date_keys:
            if k in it:
                it[k] = _fmt_date(it[k])
    return items

# -------------------- Push subscriptions helpers -------------------- #

def upsert_push_subscription(email: str, role: str, platform: str, fcm_token: str):
    """Insert or update a push subscription row by fcm_token (unique)."""
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = (
                "INSERT INTO push_subscriptions (email, role, platform, fcm_token) "
                "VALUES (%s,%s,%s,%s) "
                "ON DUPLICATE KEY UPDATE email=VALUES(email), role=VALUES(role), platform=VALUES(platform)"
            )
            cursor.execute(sql, (email, role, platform, fcm_token))
        conn.commit()
    finally:
        conn.close()


def delete_push_subscription(fcm_token: str):
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("DELETE FROM push_subscriptions WHERE fcm_token=%s", (fcm_token,))
        conn.commit()
    finally:
        conn.close()


def get_tokens_for_users(emails: list[str]):
    if not emails:
        return []
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            placeholders = ",".join(["%s"] * len(emails))
            sql = f"SELECT fcm_token FROM push_subscriptions WHERE email IN ({placeholders})"
            cursor.execute(sql, emails)
            rows = cursor.fetchall() or []
            return [r.get('fcm_token') for r in rows if r.get('fcm_token')]
    finally:
        conn.close()

def handle_numero_dipendenti(data, get_db_connection_fn):
    """Descrizione:
    Handler per ottenere il conteggio denormalizzato dei dipendenti di un
    dipartimento, basato sul campo `Dipartimento.numero_dipendenti`.

    Input:
    - data (dict): richiede chiave 'id_dipartimento' (validata a monte dallo schema).
    - get_db_connection_fn (callable): funzione/factory che restituisce una connessione.

    Output:
    - dict: { n_dipendenti: int }
    - Può sollevare ServerException per errori DB inattesi.
    """
    dept_id = data.get('id_dipartimento')
    conn = get_db_connection_fn()
    try:
        with conn.cursor() as cursor:
            sql = sql_select_helper('Dipartimento', columns=['numero_dipendenti'], where_cols=['id_dipartimento'])
            cursor.execute(sql, (dept_id,))
            row = cursor.fetchone() or {"numero_dipendenti": 0}
            return {"n_dipendenti": row.get('numero_dipendenti', 0)}
    except Exception as e:
        raise ServerException(details={"orig": str(e)})
    finally:
        conn.close()

def handle_task(data, insert_func, args_builder, success_message='Created'):
    """Descrizione:
    Handler generico per inserire una Task. Si appoggia a una funzione di insert
    e a un costruttore di tuple (args_builder) per disaccoppiare la logica
    dell'endpoint dalla persistenza.

    Input:
    - data (dict): dati già validati a monte (schema Marshmallow) per la Task.
    - insert_func (callable): funzione che esegue l'inserimento (es. insertTask).
    - args_builder (callable): funzione che riceve data e ritorna la tupla di argomenti per insert_func.
    - success_message (str): messaggio di successo personalizzabile.

    Output:
    - (Flask Response, status=201) JSON: { data: { id_task }, message }
    - Può sollevare ServerException in caso di errori DB o vincoli.
    """
    try:
        args = args_builder(data)
        id = insertTask(*args)
    except Exception as e:
        integrity_errors_project(e)
        raise ServerException(details={"orig": str(e)})

    # Fire-and-forget notification to assigned users (best-effort)
    try:
        id_progetto = data.get('id_progetto')
        email_dip = data.get('email_dipendente')
        email_mgr = data.get('email_manager')
        recipients = [e for e in {email_dip, email_mgr} if e]
        if recipients:
            _notify_emails(
                recipients,
                title="Task creata",
                body=f"Nuova task nel progetto {id_progetto}",
                data={
                    "type": "task.create",
                    "id": str(id),
                    "id_progetto": str(id_progetto),
                    "stato": str(data.get('stato', '')),
                },
            )
    except Exception:
        # non bloccare la response
        pass

    return jsonify({"data": {"id_task": id}, "message": success_message}), 201

def insertTask(id_task, stato, descrizione, data_inizio, data_fine, id_progetto, dipendente_email=None, manager_email=None):
    """Descrizione:
    Inserisce un record nella tabella TASK.

    Input:
    - id_task (int|None): se None usa AUTO_INCREMENT, altrimenti inserimento con id esplicito.
    - stato (str): stato (<=50 char).
    - descrizione (str): descrizione testuale.
    - data_inizio (str YYYY-MM-DD)
    - data_fine (str YYYY-MM-DD)
    - id_progetto (int): FK a Progetto.id_progetto
    - dipendente_email (str|None): FK a Dipendente.email
    - manager_email (str|None): FK a Manager.email

    Output:
    - int: id (generato o esplicito) della task inserita.
    - Può sollevare eccezioni DB propagate al chiamante.
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            if id_task is None:
                cols = ["stato","descrizione","data_inizio","data_fine","Progetto_id_progetto","Dipendente_email","Manager_email"]
                sql = sql_insert_helper('TASK', cols)
                cursor.execute(sql, (stato, descrizione, data_inizio, data_fine, id_progetto, dipendente_email, manager_email))
                new_id = cursor.lastrowid
            else:
                cols = ["id","stato","descrizione","data_inizio","data_fine","Progetto_id_progetto","Dipendente_email","Manager_email"]
                sql = sql_insert_helper('TASK', cols)
                cursor.execute(sql, (id_task, stato, descrizione, data_inizio, data_fine, id_progetto, dipendente_email, manager_email))
                new_id = id_task
        conn.commit()
    finally:
        conn.close()
    return new_id

def get_projects_from_department(id_dipartimento: str):
    """Descrizione:
    Restituisce tutti i progetti di un dipartimento ordinati per id.

    Input:
    - id_dipartimento (str|int): identificativo del dipartimento.

    Output:
    - list[dict]: lista (anche vuota) di righe Progetto.
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = sql_select_helper('Progetto', where_cols=['Dipartimento_id_dipartimento'], order_by='id_progetto')
            cursor.execute(sql, (id_dipartimento,))
            return cursor.fetchall()
    finally:
        conn.close()

def get_projects_by_department(id_dipartimento: str):
    """Descrizione:
    Alias di get_projects_from_department (compatibilità naming).

    Input:
    - id_dipartimento (str|int): id dipartimento.

    Output:
    - list[dict]: come get_projects_from_department.
    """
    return get_projects_from_department(id_dipartimento)


def get_projects_in_progress():
    """Restituisce tutti i progetti in corso (dataInizio <= CURDATE() <= dataFine).

    Input: none
    Output: list[dict]
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            # Use sql_select_helper for standardization; filter by current date via raw WHERE expr
            sql = sql_select_helper(
                table='Progetto',
                columns=['*'],
                where_exprs=['dataInizio <= CURDATE()', 'dataFine >= CURDATE()'],
                order_by='id_progetto'
            )
            cursor.execute(sql)
            items = cursor.fetchall() or []
            _format_items_dates(items, ['dataInizio','dataFine'])
            return items
    finally:
        conn.close()


def handle_projects_in_progress(data, get_user_by_token_fn, get_db_connection_fn):
    """Handler per /projects/in-progress che applica visibilità per ruolo.

    - Input JSON: { token: str }
    - Manager -> restituisce tutti i progetti in corso (scope: all)
    - Dipendente -> restituisce solo i progetti in corso a cui il dipendente è assegnato (scope: own)
    """
    token = data.get('token')
    from .exceptions import AuthException, ValidationException

    if not token:
        raise AuthException("AUTH_TOKEN_MISSING", "Token mancante", 401)

    user_wrap = get_user_by_token_fn(token)
    if not user_wrap:
        raise AuthException("AUTH_TOKEN_INVALID", "Token non valido", 403)

    utype = user_wrap.get('type')
    row = user_wrap.get('user') or {}
    if utype == 'Manager':
        items = get_projects_in_progress() or []
        _format_items_dates(items, ['dataInizio','dataFine'])
        return {"items": items, "count": len(items), "scope": "all"}

    if utype == 'Dipendente':
        email = row.get('email')
        conn = get_db_connection_fn()
        try:
            with conn.cursor() as cursor:
                sql = sql_select_helper(
                    table='Progetto p',
                    columns=['DISTINCT p.*'],
                    joins=[{'type': 'JOIN', 'table': 'TASK', 'alias': 't', 'on': 't.Progetto_id_progetto = p.id_progetto'}],
                    where_exprs=['p.dataInizio <= CURDATE()', 'p.dataFine >= CURDATE()', 't.Dipendente_email=%s'],
                    order_by='p.id_progetto'
                )
                cursor.execute(sql, (email,))
                items = cursor.fetchall() or []
                _format_items_dates(items, ['dataInizio','dataFine'])
        finally:
            conn.close()
        return {"items": items, "count": len(items), "scope": "own"}

    raise AuthException("AUTH_FORBIDDEN_ROLE", "Permesso negato: ruolo non autorizzato", 403)


def get_projects_for_employee(email: str, get_db_connection_fn=get_db_connection):
    """Ritorna i progetti associati a un dipendente (tramite TASK assignment)."""
    conn = get_db_connection_fn()
    try:
        with conn.cursor() as cursor:
            sql = sql_select_helper(
                table='Progetto p',
                columns=['DISTINCT p.*'],
                joins=[{'type': 'JOIN', 'table': 'TASK', 'alias': 't', 'on': 't.Progetto_id_progetto = p.id_progetto'}],
                where_exprs=['t.Dipendente_email=%s'],
                order_by='p.id_progetto'
            )
            cursor.execute(sql, (email,))
            items = cursor.fetchall() or []
            _format_items_dates(items, ['dataInizio','dataFine'])
            return items
    finally:
        conn.close()


def get_budget_for_project(id_progetto: int, get_db_connection_fn=get_db_connection):
    """Ritorna il budget istanziato per un progetto dato il suo id."""
    conn = get_db_connection_fn()
    try:
        with conn.cursor() as cursor:
            sql = sql_select_helper('Progetto', columns=['budgetIstanziato'], where_cols=['id_progetto'])
            cursor.execute(sql, (id_progetto,))
            row = cursor.fetchone() or {}
            val = row.get('budgetIstanziato')
            if val is None:
                return None
            # Always return fixed two-decimal string
            try:
                from decimal import Decimal
                d = Decimal(str(val)).quantize(Decimal('0.01'))
                return f"{d:.2f}"
            except Exception:
                try:
                    fval = float(val)
                    return f"{fval:.2f}"
                except Exception:
                    return str(val)
    finally:
        conn.close()

def get_tasks_from_project(id_progetto: str):
    """Descrizione:
    Ritorna la lista dei task associati a un progetto.

    Input:
    - id_progetto (str|int): identificativo del progetto.

    Output:
    - list[dict]: lista (anche vuota) di task.
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = sql_select_helper('TASK', where_cols=['Progetto_id_progetto'], order_by='id')
            cursor.execute(sql, (id_progetto,))
            items = cursor.fetchall() or []
            _format_items_dates(items, ['data_inizio','data_fine'])
            return items
    finally:
        conn.close()

def handle_project_by_department(data, get_user_by_token_fn, get_projects_by_department_fn, get_db_connection_fn):
    """Descrizione:
    Gestisce la logica dell'endpoint /project/by-department con differenziazione
    di visibilità tra Manager e Dipendente.

    Input:
    - data (dict): deve contenere token e id_dipartimento.
    - get_user_by_token_fn (callable): risolve token -> {'type','user'}.
    - get_projects_by_department_fn (callable): funzione di fetch progetti.
    - get_db_connection_fn (callable): factory connessione DB per query complesse.

    Output:
    - dict: { items: list, count: int, scope: 'all'|'own' }.
    - Eccezioni: ValidationException, AuthException.
    """
    dept_id = data.get('id_dipartimento')
    if dept_id is None:
        raise ValidationException(
            "MISSING_PARAMS",
            "Parametri richiesti mancanti",
            400,
            {"fields": {"id_dipartimento": ["Missing data for required field."]}}
        )
    token = data.get('token')
    if not token:
        raise AuthException("AUTH_TOKEN_MISSING", "Token mancante", 401)
    user_wrap = get_user_by_token_fn(token)
    if not user_wrap:
        raise AuthException("AUTH_TOKEN_INVALID", "Token non valido", 403)
    utype = user_wrap.get('type')
    row = user_wrap.get('user') or {}
    if utype == 'Manager':
        if str(row.get('Dipartimento_id_dipartimento')) != str(dept_id):
            raise AuthException("AUTH_FORBIDDEN_DEPARTMENT", "Permesso negato: manager di altro dipartimento", 403)
        items = get_projects_by_department_fn(dept_id) or []
        # Progetto columns: dataInizio,dataFine nel DB -> unify to dataInizio/dataFine
        _format_items_dates(items, ['dataInizio','dataFine','data_inizio','data_fine'])
        return {"items": items, "count": len(items), "scope": "all"}
    if utype == 'Dipendente':
        conn = get_db_connection_fn()
        try:
            with conn.cursor() as cursor:
                sql = sql_select_helper(
                    table='Progetto p',
                    columns=['DISTINCT p.*'],
                    joins=[{'type': 'JOIN', 'table': 'TASK', 'alias': 't', 'on': 't.Progetto_id_progetto = p.id_progetto'}],
                    where_exprs=['t.Dipendente_email=%s'],
                    where_cols=['p.Dipartimento_id_dipartimento'],
                    order_by='p.id_progetto'
                )
                cursor.execute(sql, (row.get('email'), dept_id))
                items = cursor.fetchall() or []
                _format_items_dates(items, ['dataInizio','dataFine'])
        finally:
            conn.close()
        return {"items": items, "count": len(items), "scope": "own"}
    raise AuthException("AUTH_FORBIDDEN_ROLE", "Permesso negato: ruolo non autorizzato", 403)


def handle_dipendenti_by_department(data, get_db_connection_fn):
    """Descrizione:
    Handler per ottenere la lista dei Dipendenti di un dipartimento.

    Input:
    - data (dict): deve contenere 'id_dipartimento' (validato a monte).
    - get_db_connection_fn (callable): factory per connessione DB.

    Output:
    - dict: { items: list[dict], count: int }
    - Eccezioni: ValidationException o ServerException.
    """
    dept_id = data.get('id_dipartimento')
    if dept_id is None:
        raise ValidationException(
            "MISSING_PARAMS",
            "Parametri richiesti mancanti",
            400,
            {"fields": {"id_dipartimento": ["Missing data for required field."]}}
        )

    conn = get_db_connection_fn()
    try:
        with conn.cursor() as cursor:
            sql = sql_select_helper('Dipendente', columns=['email','nome','cognome','data_nascita','sesso','numero_telefono'], where_cols=['Dipartimento_id_dipartimento'], order_by='email')
            cursor.execute(sql, (dept_id,))
            items = cursor.fetchall() or []
        return {"items": items, "count": len(items)}
    except Exception as e:
        raise ServerException(details={"orig": str(e)})
    finally:
        conn.close()


def handle_dipendenti_data_by_department(data, get_db_connection_fn):
    """Descrizione:
    Handler per ottenere tutti i dati dei Dipendenti di un dipartimento.

    Input:
    - data (dict): deve contenere 'id_dipartimento'.
    - get_db_connection_fn (callable): factory per connessione DB.

    Output:
    - dict: { items: list[dict], count: int }
    - Eccezioni: ValidationException, ServerException.
    """
    dept_id = data.get('id_dipartimento')
    if dept_id is None:
        raise ValidationException(
            "MISSING_PARAMS",
            "Parametri richiesti mancanti",
            400,
            {"fields": {"id_dipartimento": ["Missing data for required field."]}}
        )

    conn = get_db_connection_fn()
    try:
        with conn.cursor() as cursor:
            # Select explicit non-sensitive columns. Exclude `password` and `token`.
            sql = sql_select_helper('Dipendente', columns=['email','nome','cognome','data_nascita','sesso','numero_telefono','Dipartimento_id_dipartimento'], where_cols=['Dipartimento_id_dipartimento'], order_by='email')
            cursor.execute(sql, (dept_id,))
            items = cursor.fetchall() or []
        return {"items": items, "count": len(items)}
    except Exception as e:
        raise ServerException(details={"orig": str(e)})
    finally:
        conn.close()

def handle_task_by_project(data, get_user_by_token_fn, get_tasks_from_project_fn, get_db_connection_fn):
    """Descrizione:
    Gestisce l'endpoint /task/by-project differenziando l'accesso tra Manager e Dipendente.

    Input:
    - data (dict): token, id_progetto; per Manager anche id_dipartimento.
    - get_user_by_token_fn (callable): risolve token in utente.
    - get_tasks_from_project_fn (callable): ritorna tutte le task del progetto.
    - get_db_connection_fn (callable): connessione per query filtrate per dipendente.

    Output:
    - dict: { items, count, scope: 'all'|'own' }.
    - Eccezioni: ValidationException, AuthException.
    """
    proj_id = data.get('id_progetto')
    if proj_id is None:
        raise ValidationException(
            "MISSING_PARAMS",
            "Parametri richiesti mancanti",
            400,
            {"fields": {"id_progetto": ["Missing data for required field."]}}
        )
    token = data.get('token')
    if not token:
        raise AuthException("AUTH_TOKEN_MISSING", "Token mancante", 401)
    user_wrap = get_user_by_token_fn(token)
    if not user_wrap:
        raise AuthException("AUTH_TOKEN_INVALID", "Token non valido", 403)
    utype = user_wrap.get('type')
    row = user_wrap.get('user') or {}
    if utype == 'Manager':
        dept_id = data.get('id_dipartimento')
        if dept_id is None:
            raise ValidationException(
                "MISSING_PARAMS",
                "Parametri richiesti mancanti",
                400,
                {"fields": {"id_dipartimento": ["Missing data for required field."]}}
            )
        if str(row.get('Dipartimento_id_dipartimento')) != str(dept_id):
            raise AuthException("AUTH_FORBIDDEN_DEPARTMENT", "Permesso negato: manager di altro dipartimento", 403)
        items = get_tasks_from_project_fn(proj_id) or []
        return {"items": items, "count": len(items), "scope": "all"}
    if utype == 'Dipendente':
        conn = get_db_connection_fn()
        try:
            with conn.cursor() as cursor:
                    sql = sql_select_helper(
                        table='TASK',
                        where_cols=['Progetto_id_progetto', 'Dipendente_email'],
                        order_by='id'
                    )
                    cursor.execute(sql, (proj_id, row.get('email')))
                    items = cursor.fetchall() or []
                    _format_items_dates(items, ['data_inizio','data_fine'])
        finally:
            conn.close()
        return {"items": items, "count": len(items), "scope": "own"}
    raise AuthException("AUTH_FORBIDDEN_ROLE", "Permesso negato: ruolo non autorizzato", 403)


def handle_dipendenti_by_project(data, get_user_by_token_fn, get_db_connection_fn):
    """Descrizione:
    Restituisce i dipendenti che lavorano su un progetto dato (`id_progetto`).

    Comportamento:
    - Manager: se appartiene al dipartimento fornito (id_dipartimento) ritorna tutti
      i dipendenti assegnati al progetto (scope: all).
    - Dipendente: ritorna solo se l'utente è assegnato al progetto (scope: own).

    Input:
    - data (dict): deve contenere 'id_progetto' e 'token'; per Manager anche 'id_dipartimento'.
    - get_user_by_token_fn (callable): risolve token -> {'type','user'}.
    - get_db_connection_fn (callable): factory per connessione DB.

    Output:
    - dict: { items: list[dict], count: int, scope: 'all'|'own' }
    - Eccezioni: ValidationException, AuthException, ServerException.
    """
    proj_id = data.get('id_progetto')
    if proj_id is None:
        raise ValidationException(
            "MISSING_PARAMS",
            "Parametri richiesti mancanti",
            400,
            {"fields": {"id_progetto": ["Missing data for required field."]}}
        )

    token = data.get('token')
    if not token:
        raise AuthException("AUTH_TOKEN_MISSING", "Token mancante", 401)

    user_wrap = get_user_by_token_fn(token)
    if not user_wrap:
        raise AuthException("AUTH_TOKEN_INVALID", "Token non valido", 403)

    utype = user_wrap.get('type')
    row = user_wrap.get('user') or {}
    if utype == 'Manager':
        dept_id = data.get('id_dipartimento')
        if dept_id is None:
            raise ValidationException(
                "MISSING_PARAMS",
                "Parametri richiesti mancanti",
                400,
                {"fields": {"id_dipartimento": ["Missing data for required field."]}}
            )
        if str(row.get('Dipartimento_id_dipartimento')) != str(dept_id):
            raise AuthException("AUTH_FORBIDDEN_DEPARTMENT", "Permesso negato: manager di altro dipartimento", 403)

        conn = get_db_connection_fn()
        try:
            with conn.cursor() as cursor:
                sql = sql_select_helper(
                    table='Dipendente d',
                    columns=['DISTINCT d.email','d.nome','d.cognome','d.data_nascita','d.sesso','d.numero_telefono'],
                    joins=[{'type':'JOIN','table':'TASK','alias':'t','on':'t.Dipendente_email = d.email'}],
                    where_cols=['d.Dipartimento_id_dipartimento'],
                    where_exprs=['t.Progetto_id_progetto=%s'],
                    order_by='d.email'
                )
                cursor.execute(sql, (proj_id, dept_id))
                items = cursor.fetchall() or []
        finally:
            conn.close()
        _format_items_dates(items, ['data_nascita'])
        return {"items": items, "count": len(items), "scope": "all"}

    elif utype == 'Dipendente':
        conn = get_db_connection_fn()
        try:
            with conn.cursor() as cursor:
                sql = sql_select_helper(
                    table='Dipendente d',
                    columns=['DISTINCT d.email','d.nome','d.cognome','d.data_nascita','d.sesso','d.numero_telefono'],
                    joins=[{'type':'JOIN','table':'TASK','alias':'t','on':'t.Dipendente_email = d.email'}],
                    where_exprs=['t.Progetto_id_progetto=%s'],
                    where_cols=['d.email'],
                    order_by='d.email'
                )
                cursor.execute(sql, (proj_id, row.get('email')))
                items = cursor.fetchall() or []
        finally:
            conn.close()
        _format_items_dates(items, ['data_nascita'])
        return {"items": items, "count": len(items), "scope": "own"}

    raise AuthException("AUTH_FORBIDDEN_ROLE", "Permesso negato: ruolo non autorizzato", 403)


def handle_managers_by_project(data, get_user_by_token_fn, get_db_connection_fn):
    """Descrizione:
    Restituisce i manager coinvolti in un progetto dato (`id_progetto`).

    Comportamento:
    - Manager: se appartiene al dipartimento fornito (id_dipartimento) ritorna tutti
      i manager assegnati al progetto nel dipartimento (scope: all).
    - Dipendente: ritorna solo i manager assegnati alle task del progetto a cui il
      dipendente è associato (scope: own).

    Input:
    - data (dict): deve contenere 'id_progetto' e 'token'; per Manager anche 'id_dipartimento'.
    - get_user_by_token_fn (callable): risolve token -> {'type','user'}.
    - get_db_connection_fn (callable): factory per connessione DB.

    Output:
    - dict: { items: list[dict], count: int, scope: 'all'|'own' }
    - Eccezioni: ValidationException, AuthException, ServerException.
    """
    proj_id = data.get('id_progetto')
    if proj_id is None:
        raise ValidationException(
            "MISSING_PARAMS",
            "Parametri richiesti mancanti",
            400,
            {"fields": {"id_progetto": ["Missing data for required field."]}}
        )

    token = data.get('token')
    if not token:
        raise AuthException("AUTH_TOKEN_MISSING", "Token mancante", 401)

    user_wrap = get_user_by_token_fn(token)
    if not user_wrap:
        raise AuthException("AUTH_TOKEN_INVALID", "Token non valido", 403)

    utype = user_wrap.get('type')
    row = user_wrap.get('user') or {}
    if utype == 'Manager':
        dept_id = data.get('id_dipartimento')
        if dept_id is None:
            raise ValidationException(
                "MISSING_PARAMS",
                "Parametri richiesti mancanti",
                400,
                {"fields": {"id_dipartimento": ["Missing data for required field."]}}
            )
        if str(row.get('Dipartimento_id_dipartimento')) != str(dept_id):
            raise AuthException("AUTH_FORBIDDEN_DEPARTMENT", "Permesso negato: manager di altro dipartimento", 403)

        conn = get_db_connection_fn()
        try:
            with conn.cursor() as cursor:
                sql = sql_select_helper(
                    table='Manager m',
                    columns=['DISTINCT m.email','m.nome','m.cognome','m.data_nascita','m.sesso','m.numero_telefono'],
                    joins=[{'type':'JOIN','table':'TASK','alias':'t','on':'t.Manager_email = m.email'}],
                    where_cols=['m.Dipartimento_id_dipartimento'],
                    where_exprs=['t.Progetto_id_progetto=%s'],
                    order_by='m.email'
                )
                cursor.execute(sql, (proj_id, dept_id))
                items = cursor.fetchall() or []
        finally:
            conn.close()
        _format_items_dates(items, ['data_nascita'])
        return {"items": items, "count": len(items), "scope": "all"}

    elif utype == 'Dipendente':
        conn = get_db_connection_fn()
        try:
            with conn.cursor() as cursor:
                sql = sql_select_helper(
                    table='Manager m',
                    columns=['DISTINCT m.email','m.nome','m.cognome','m.data_nascita','m.sesso','m.numero_telefono'],
                    joins=[{'type':'JOIN','table':'TASK','alias':'t','on':'t.Manager_email = m.email'}],
                    where_exprs=['t.Progetto_id_progetto=%s','t.Dipendente_email=%s'],
                    order_by='m.email'
                )
                cursor.execute(sql, (proj_id, row.get('email')))
                items = cursor.fetchall() or []
        finally:
            conn.close()
        _format_items_dates(items, ['data_nascita'])
        return {"items": items, "count": len(items), "scope": "own"}

    raise AuthException("AUTH_FORBIDDEN_ROLE", "Permesso negato: ruolo non autorizzato", 403)

def handle_project(data, insert_func, args_builder, success_message='Created'):
    """Descrizione:
    Handler generico per inserire un Progetto.

    Input:
    - data (dict): dati già validati (schema Marshmallow).
    - insert_func (callable): funzione di inserimento (insertProject).
    - args_builder (callable): build tuple parametri.
    - success_message (str): messaggio custom.

    Output:
    - (Flask Response, 201) JSON con id_progetto.
    - Eccezioni: ServerException.
    """
    try:
        args = args_builder(data)
        id = insert_func(*args)
    except Exception as e:
        integrity_errors_project(e)
        raise ServerException(details={"orig": str(e)})

    return jsonify({"data": {"id_progetto": id}, "message": success_message}), 201

def handle_login(data):
    """Descrizione:
    Gestisce login via token oppure coppia email/password.

    Input:
    - data (dict): token oppure email & password (già validati nello strato route).

    Output:
    - (Flask Response, 200) con {token, type} in caso di successo.
    - Eccezioni: AuthException, ValidationException.
    """
    from .utils import decide_login, crypt

    decision = decide_login(data, get_user_by_token, getManager, getDipendente, crypt)

    tag = decision[0]
    if tag == 'ok_token':
        _, token, tipo = decision
        return jsonify({"data": {"token": token, "type": tipo}, "message": "Login effettuato"}), 200
    
    if tag == 'invalid_token':
        raise AuthException("AUTH_TOKEN_INVALID", "Token non valido", 403)
    
    if tag == 'missing_credentials':
        raise ValidationException("MISSING_CREDENTIALS", "Email o password mancanti", 400)
    
    if tag == 'ok':
        _, token, tipo = decision
        return jsonify({"data": {"token": token, "type": tipo}, "message": "Login effettuato"}), 200
    
    raise AuthException("INVALID_CREDENTIALS", "Email o password non valide", 401)

def handle_register(data, insert_func, args_builder, success_message='Created'):
    """Descrizione:
    Effettua la registrazione (dipendente o manager) delegando la creazione al
    livello di persistenza e restituendo il token generato.

    Input:
    - data (dict): payload validato.
    - insert_func (callable): funzione insertDipendente / insertManager.
    - args_builder (callable): costruisce i parametri per insert_func.
    - success_message (str): messaggio di successo.

    Output:
    - (Flask Response, 201) JSON { token }.
    - Eccezioni: ServerException.
    """
    try:
        args = args_builder(data)
        token = insert_func(*args)
    except Exception as e:
        integrity_errors(e)
        raise ServerException(details={"orig": str(e)})

    return jsonify({"data": {"token": token}, "message": success_message}), 201

def _insert_user(table: str, columns: list, values: tuple) -> str:
    """Descrizione:
    Helper generico per inserire un utente-like e restituire il token generato.

    Input:
    - table (str): nome tabella ('Dipendente'|'Manager').
    - columns (list[str]): colonne da inserire (ultima attesa: token).
    - values (tuple): valori ordinati rispetto alle colonne.

    Output:
    - str: token generato.
    - Eccezioni propagate del DB.
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = sql_insert_helper(table, columns)
            cursor.execute(sql, values)
        conn.commit()
    finally:
        conn.close()
    # Attempt to return the token value (identified by column name) for clarity
    try:
        if 'token' in columns:
            return values[columns.index('token')]
    except Exception:
        pass
    return values[-1]

def insertDipendente(email: str, password: str, nome: str, cognome: str, data_nascita: str, Dipartimento_id_dipartimento: int, sesso: str | None = None, numero_telefono: str | None = None) -> str:
    """Descrizione:
    Inserisce un nuovo Dipendente generando un token e aggiorna
    atomicamente il campo denormalizzato `Dipartimento.numero_dipendenti`.

    Input:
    - email, password (hash pre-gestito a monte se necessario), nome, cognome, data_nascita
    - Dipartimento_id_dipartimento (int)

    Output:
    - str: token associato al dipendente.
    Side effect: `Dipartimento.numero_dipendenti` incrementato di 1.
    """
    token = generateToken()
    # Inserimento + aggiornamento contatore nello stesso commit per coerenza.
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cols = ['email','password','nome','cognome','data_nascita','sesso','numero_telefono','Dipartimento_id_dipartimento','token']
            sql = sql_insert_helper('Dipendente', cols)
            cursor.execute(sql, (email, password, nome, cognome, data_nascita, sesso, numero_telefono, Dipartimento_id_dipartimento, token))
            cursor.execute(
                "UPDATE Dipartimento SET numero_dipendenti = numero_dipendenti + 1 WHERE id_dipartimento=%s",
                (Dipartimento_id_dipartimento,)
            )
        conn.commit()
    finally:
        conn.close()
    return token

def insertProject(id_progetto, descrizione, budget, nome, data_inizio, data_fine, id_dipartimento):
    """Descrizione:
    Inserisce o forza l'inserimento di un Progetto.

    Input:
    - id_progetto (int|None): None => auto-increment, altrimenti inserimento esplicito.
    - descrizione (str), budget (Decimal/str), nome (str), data_inizio/fine (str), id_dipartimento (int).

    Output:
    - int: id del progetto (nuovo o specificato).
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            if id_progetto is None:
                cols = ["descrizione","budgetIstanziato","nome","dataInizio","dataFine","Dipartimento_id_dipartimento"]
                sql = sql_insert_helper('Progetto', cols)
                cursor.execute(sql, (descrizione, budget, nome, data_inizio, data_fine, id_dipartimento))
                new_id = cursor.lastrowid
            else:
                cols = ["id_progetto","descrizione","budgetIstanziato","nome","dataInizio","dataFine","Dipartimento_id_dipartimento"]
                sql = sql_insert_helper('Progetto', cols)
                cursor.execute(sql, (id_progetto, descrizione, budget, nome, data_inizio, data_fine, id_dipartimento))
                new_id = id_progetto
        conn.commit()
    finally:
        conn.close()
    return new_id

def updateProject(id_progetto: int, updates: dict):
    """Aggiorna un progetto. 'updates' contiene colonne valide da modificare.

    Ritorna la riga aggiornata o None se non esiste.
    """
    if not updates:
        return None
    allowed = {"descrizione","budgetIstanziato","nome","dataInizio","dataFine","Dipartimento_id_dipartimento"}
    cols = [c for c in updates.keys() if c in allowed]
    if not cols:
        return None
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = sql_update_helper('Progetto', cols, ['id_progetto'])
            cursor.execute(sql, tuple(updates[c] for c in cols) + (id_progetto,))
            if cursor.rowcount == 0:
                return None
        conn.commit()
    finally:
        conn.close()
    # fetch updated row
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM Progetto WHERE id_progetto=%s", (id_progetto,))
            row = cursor.fetchone()
            if row:
                _format_items_dates([row], ['dataInizio','dataFine'])
            return row
    finally:
        conn.close()

def deleteProject(id_progetto: int) -> bool:
    """Elimina un progetto. Ritorna True se eliminato, False se non trovato."""
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = sql_delete_helper('Progetto', ['id_progetto'])
            cursor.execute(sql, (id_progetto,))
            deleted = cursor.rowcount > 0
        conn.commit()
        return deleted
    finally:
        conn.close()

def updateTask(id_task: int, id_progetto: int, updates: dict):
    """Aggiorna una task, assicurandosi che appartenga al progetto indicato.
    Ritorna la riga aggiornata o None se non esiste.
    """
    if not updates:
        return None
    allowed = {"stato","descrizione","data_inizio","data_fine","Dipendente_email","Manager_email"}
    cols = [c for c in updates.keys() if c in allowed]
    if not cols:
        return None
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = sql_update_helper('TASK', cols, ['id','Progetto_id_progetto'])
            cursor.execute(sql, tuple(updates[c] for c in cols) + (id_task, id_progetto))
            if cursor.rowcount == 0:
                return None
        conn.commit()
    finally:
        conn.close()
    # fetch updated row
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM TASK WHERE id=%s AND Progetto_id_progetto=%s", (id_task, id_progetto))
            row = cursor.fetchone()
            if row:
                _format_items_dates([row], ['data_inizio','data_fine'])
            return row
    finally:
        conn.close()

def deleteTask(id_task: int, id_progetto: int) -> bool:
    """Elimina una task specifica del progetto. Ritorna True se eliminata."""
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = sql_delete_helper('TASK', ['id','Progetto_id_progetto'])
            cursor.execute(sql, (id_task, id_progetto))
            deleted = cursor.rowcount > 0
        conn.commit()
        return deleted
    finally:
        conn.close()

# -------------------- Update/Delete Handlers (Business Logic) -------------------- #
def handle_update_project(data, update_func=updateProject, get_db_connection_fn=get_db_connection):
    """Gestisce la logica di aggiornamento progetto.

    Input data: { id_progetto, <campi opzionali aggiornabili> }
    Output: dict riga aggiornata
    Errori: ValidationException (nessun campo), NotFoundException.
    """
    from .exceptions import ValidationException, NotFoundException, ServerException
    idp = data.get('id_progetto')
    if idp is None:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": {"id_progetto": ["Missing data for required field."]}})
    field_mapping = {
        'descrizione': 'descrizione',
        'budget': 'budgetIstanziato',
        'nome': 'nome',
        'data_inizio': 'dataInizio',
        'data_fine': 'dataFine',
        'id_dipartimento': 'Dipartimento_id_dipartimento'
    }
    updates = {}
    for src, dest in field_mapping.items():
        if data.get(src) is not None:
            updates[dest] = data.get(src)
    if not updates:
        raise ValidationException("MISSING_PARAMS", "Nessun campo da aggiornare", 400, {"fields": {"_": ["Specificare almeno un campo aggiornabile."]}})
    try:
        updated = update_func(idp, updates)
    except Exception as e:
        # tenta di mappare eventuali integrity
        try:
            integrity_errors_project(e)
        except Exception:
            pass
        raise ServerException(details={"orig": str(e)})
    if not updated:
        raise NotFoundException("Progetto non trovato")
    if updated:
        _format_items_dates([updated], ['dataInizio','dataFine'])
    return updated

def handle_delete_project(data, delete_func=deleteProject, get_db_connection_fn=get_db_connection):
    from .exceptions import ValidationException, NotFoundException, ServerException
    idp = data.get('id_progetto')
    if idp is None:
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": {"id_progetto": ["Missing data for required field."]}})
    # fetch before delete
    conn = get_db_connection_fn()
    row = None
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM Progetto WHERE id_progetto=%s", (idp,))
            row = cursor.fetchone()
    finally:
        conn.close()
    if not row:
        raise NotFoundException("Progetto non trovato")
    try:
        ok = delete_func(idp)
    except Exception as e:
        raise ServerException(details={"orig": str(e)})
    if not ok:
        raise NotFoundException("Progetto non trovato")
    if row:
        _format_items_dates([row], ['dataInizio','dataFine'])
    return row

def handle_update_task(data, update_func=updateTask, get_tasks_fn=get_tasks_from_project):
    from .exceptions import ValidationException, NotFoundException, ServerException
    idt = data.get('id')
    idp = data.get('id_progetto')
    if idt is None or idp is None:
        missing = {}
        if idt is None: missing['id'] = ["Missing data for required field."]
        if idp is None: missing['id_progetto'] = ["Missing data for required field."]
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": missing})
    field_mapping = {
        'stato': 'stato',
        'descrizione': 'descrizione',
        'data_inizio': 'data_inizio',
        'data_fine': 'data_fine',
        'email_dipendente': 'Dipendente_email',
        'email_manager': 'Manager_email'
    }
    updates = {}
    for src, dest in field_mapping.items():
        if data.get(src) is not None:
            updates[dest] = data.get(src)
    if not updates:
        raise ValidationException("MISSING_PARAMS", "Nessun campo da aggiornare", 400, {"fields": {"_": ["Specificare almeno un campo aggiornabile."]}})
    try:
        updated = update_func(idt, idp, updates)
    except Exception as e:
        try:
            integrity_errors_project(e)
        except Exception:
            pass
        raise ServerException(details={"orig": str(e)})
    if not updated:
        raise NotFoundException("Task non trovata")
    # return full task list of project
    items = get_tasks_fn(idp) or []
    _format_items_dates(items, ['data_inizio','data_fine'])

    # Notify assigned users (best-effort, async)
    try:
        recipients = [e for e in {updated.get('Dipendente_email'), updated.get('Manager_email')} if e]
        if recipients:
            _notify_emails(
                recipients,
                title="Task aggiornata",
                body=f"Task {idt} aggiornata nel progetto {idp}",
                data={
                    "type": "task.update",
                    "id": str(idt),
                    "id_progetto": str(idp),
                    "stato": str(updated.get('stato', '')),
                },
            )
    except Exception:
        pass
    return {"items": items, "count": len(items)}

def handle_delete_task(data, delete_func=deleteTask, get_tasks_fn=get_tasks_from_project):
    from .exceptions import ValidationException, NotFoundException, ServerException
    idt = data.get('id')
    idp = data.get('id_progetto')
    if idt is None or idp is None:
        missing = {}
        if idt is None: missing['id'] = ["Missing data for required field."]
        if idp is None: missing['id_progetto'] = ["Missing data for required field."]
        raise ValidationException("MISSING_PARAMS", "Validazione fallita", 400, {"fields": missing})
    # fetch before delete per notification recipients
    conn = get_db_connection()
    row_before = None
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM TASK WHERE id=%s AND Progetto_id_progetto=%s", (idt, idp))
            row_before = cursor.fetchone()
    finally:
        conn.close()

    try:
        ok = delete_func(idt, idp)
    except Exception as e:
        raise ServerException(details={"orig": str(e)})
    if not ok:
        raise NotFoundException("Task non trovata")
    items = get_tasks_fn(idp) or []
    _format_items_dates(items, ['data_inizio','data_fine'])

    # Notify assigned users (best-effort, async)
    try:
        recipients = []
        if row_before:
            recipients = [e for e in {row_before.get('Dipendente_email'), row_before.get('Manager_email')} if e]
        if recipients:
            _notify_emails(
                recipients,
                title="Task eliminata",
                body=f"Task {idt} eliminata dal progetto {idp}",
                data={
                    "type": "task.delete",
                    "id": str(idt),
                    "id_progetto": str(idp),
                },
            )
    except Exception:
        pass
    return {"items": items, "count": len(items)}


# -------------------- Notification helpers -------------------- #
def _notify_emails(emails: list[str], title: str, body: str, data: dict):
    if not emails:
        return
    # Resolve tokens, then send async and cleanup invalid tokens
    tokens = get_tokens_for_users(emails)
    if not tokens:
        return

    def _worker(tok_list: list[str]):
        try:
            from .notifications import send_to_tokens
            result = send_to_tokens(tok_list, title, body, data)
            invalid = result.get('invalid') or []
            if invalid:
                for t in invalid:
                    try:
                        delete_push_subscription(t)
                    except Exception:
                        pass
        except Exception:
            # swallow errors in async path
            pass

    threading.Thread(target=_worker, args=(tokens,), daemon=True).start()

def insertManager(email: str, password: str, nome: str, cognome: str, data_nascita: str, anni_lavorativi: int, Dipartimento_id_dipartimento: int, sesso: str | None = None, numero_telefono: str | None = None) -> str:
    """Descrizione:
    Inserisce un Manager generando un token.

    Input:
    - email, password, nome, cognome, data_nascita, anni_lavorativi, Dipartimento_id_dipartimento.

    Output:
    - str: token generato per il manager.
    """
    token = generateToken()
    columns = [
        'email','password','nome','cognome','data_nascita','sesso','numero_telefono','anni_lavorativi','Dipartimento_id_dipartimento','token'
    ]
    values = (email, password, nome, cognome, data_nascita, sesso, numero_telefono, anni_lavorativi, Dipartimento_id_dipartimento, token)
    return _insert_user('Manager', columns, values)


def insertDipartimento(id_dipartimento, nome, numero_dipendenti=0):
    """Inserisce un Dipartimento.

    Input:
    - id_dipartimento (int|None): se None usa AUTO_INCREMENT
    - nome (str)
    - numero_dipendenti (int)

    Output:
    - int: id del dipartimento (nuovo o specificato)
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            if id_dipartimento is None:
                cols = ['nome', 'numero_dipendenti']
                sql = sql_insert_helper('Dipartimento', cols)
                cursor.execute(sql, (nome, numero_dipendenti))
                new_id = cursor.lastrowid
            else:
                cols = ['id_dipartimento', 'nome', 'numero_dipendenti']
                sql = sql_insert_helper('Dipartimento', cols)
                cursor.execute(sql, (id_dipartimento, nome, numero_dipendenti))
                new_id = id_dipartimento
        conn.commit()
    finally:
        conn.close()
    return new_id


def handle_insert_dipartimento(data, insert_func, args_builder, success_message='Created'):
    """Handler generico per creare un Dipartimento.

    Questo segue il pattern usato per progetti/task/register: costruisce gli argomenti
    e delega la logica di insert alla funzione di persistenza fornita. Viene usato
    dal livello manager per esporre un'API protetta.
    """
    try:
        args = args_builder(data)
        id = insert_func(*args)
    except Exception as e:
        # Traduce errori di integrità specifici per Dipartimento (duplicate -> DUPLICATE_DEPARTMENT)
        from .utils import integrity_errors_department
        integrity_errors_department(e)
        raise ServerException(details={"orig": str(e)})

    return jsonify({"data": {"id_dipartimento": id}, "message": success_message}), 201


def getDipendente(email: str):
    """Descrizione:
    Recupera un Dipendente per email.

    Input:
    - email (str)

    Output:
    - dict|None: riga dipendente oppure None.
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM Dipendente WHERE email = %s", (email,))
            return cursor.fetchone()
    finally:
        conn.close()

def getManager(email: str):
    """Descrizione:
    Recupera un Manager per email.

    Input:
    - email (str)

    Output:
    - dict|None: riga manager oppure None.
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM Manager WHERE email = %s", (email,))
            return cursor.fetchone()
    finally:
        conn.close()

def get_user_by_email(email: str):
    """Descrizione:
    Tenta di risolvere un utente (Manager o Dipendente) a partire dall'email.

    Input:
    - email (str)

    Output:
    - dict: {'type': 'Manager'|'Dipendente', 'user': row} oppure None.
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM Manager WHERE email = %s", (email,))
            row = cursor.fetchone()
            if row:
                return {'type': 'Manager', 'user': row}
            cursor.execute("SELECT * FROM Dipendente WHERE email = %s", (email,))
            row = cursor.fetchone()
            if row:
                return {'type': 'Dipendente', 'user': row}
        return None
    finally:
        conn.close()

def get_user_by_token(token: str):
    """Descrizione:
    Risolve un utente dal token memorizzato (Manager o Dipendente).

    Input:
    - token (str)

    Output:
    - dict|None: {'type': ..., 'user': row} oppure None se non trovato.
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM Manager WHERE token = %s", (token,))
            row = cursor.fetchone()
            if row:
                return {'type': 'Manager', 'user': row}
            cursor.execute("SELECT * FROM Dipendente WHERE token = %s", (token,))
            row = cursor.fetchone()
            if row:
                return {'type': 'Dipendente', 'user': row}
        return None
    finally:
        conn.close()