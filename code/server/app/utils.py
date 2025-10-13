def crypt(x: str) -> str:
    """Descrizione:
    Calcola l'hash SHA256 della stringa fornita.

    Input:
    - x (str): testo in chiaro.

    Output:
    - str: esadecimale hash a 64 caratteri.
    """
    from hashlib import sha256
    return sha256(x.encode()).hexdigest()


def generateToken() -> str:
    """Descrizione:
    Genera un token casuale esadecimale (64 char) per autenticazione.

    Input: (none)
    Output: str token.
    """
    from secrets import token_hex
    return token_hex(32)

# -------------------- Generic SQL Helpers -------------------- #
def sql_insert_helper(table: str, cols: list):
    """Descrizione:
    Costruisce una query INSERT parametrica per la tabella indicata.

    Input:
    - table (str): nome tabella (trusted)
    - cols (list[str]): nomi colonne (non vuota)

    Output:
    - str: SQL 'INSERT INTO <table> (c1,...) VALUES (%s,...)'

    Eccezioni:
    - ValueError se cols è vuota.
    """
    if not cols:
        raise ValueError("sql_insert_helper: 'cols' non può essere vuota")
    placeholders = ', '.join(['%s'] * len(cols))
    cols_part = ', '.join(cols)
    return f"INSERT INTO {table} ({cols_part}) VALUES ({placeholders})"


def sql_select_helper(table: str,
                      columns: list | str | None = None,
                      joins: list | None = None,
                      where_cols: list | None = None,
                      where_exprs: list | None = None,
                      order_by: str | None = None,
                      distinct: bool = False):
    """Descrizione:
    Costruisce una query SELECT parametrica con supporto base per alias, JOIN
    e condizioni WHERE opzionali.

    Parametri chiave (semplici e pratici):
    - table: tabella principale, può includere alias (es. 'Progetto p').
    - columns: lista di colonne/espressioni (es. ['p.*', 'd.email AS dip_email']).
    - joins: lista di dict con chiavi: type (es. 'JOIN'|'LEFT JOIN'), table (es. 'TASK'),
             alias (opzionale), on (string ON expr, es. 't.Progetto_id_progetto = p.id_progetto').
             Esempio: [{'type':'JOIN','table':'TASK','alias':'t','on':'t.Progetto_id_progetto = p.id_progetto'}]
    - where_cols: lista di nomi di colonne singole per cui verrà generata la clausola `col=%s`.
                  Usa nomi qualificati quando necessario (es. 'p.id_progetto').
    - where_exprs: lista di espressioni raw da includere in WHERE (es. 't.Dipendente_email=%s').
    - order_by: stringa per ORDER BY.
    - distinct: se True aggiunge DISTINCT.

    Nota: where_cols e where_exprs possono essere combinati; l'ordine delle placeholder
    nei parametri deve corrispondere all'ordine dei valori passati a cursor.execute.
    """
    # Allow columns to be provided as a single string or as a list of expressions
    if isinstance(columns, str):
        cols_part = columns
    else:
        cols_part = ', '.join(columns) if columns else '*'
    distinct_part = 'DISTINCT ' if distinct else ''
    sql = f"SELECT {distinct_part}{cols_part} FROM {table}"

    # joins
    if joins:
        for j in joins:
            jtype = j.get('type', 'JOIN')
            # allow either 'table' + optional 'alias' or a single 'table' string like 'TASK t'
            jtable = j.get('table')
            jalias = j.get('alias')
            jon = j.get('on')
            if jalias:
                sql += f" {jtype} {jtable} {jalias}"
            else:
                sql += f" {jtype} {jtable}"
            if jon:
                sql += f" ON {jon}"

    # where clauses: expressions and simple column placeholders
    conds = []
    if where_exprs:
        conds.extend(where_exprs)
    if where_cols:
        conds.extend([f"{c}=%s" for c in where_cols])
    if conds:
        sql += " WHERE " + ' AND '.join(conds)

    if order_by:
        sql += f" ORDER BY {order_by}"
    return sql

def sql_update_helper(table: str, set_cols: list, where_cols: list):
    """Descrizione:
    Costruisce una query UPDATE parametrica.

    Input:
    - table (str): nome tabella (trusted)
    - set_cols (list[str]): colonne da aggiornare
    - where_cols (list[str]): colonne per condizioni di uguaglianza (AND)

    Output:
    - str: 'UPDATE <table> SET c1=%s,c2=%s WHERE w1=%s AND w2=%s'

    Eccezioni:
    - ValueError se set_cols o where_cols vuoti.
    """
    if not set_cols:
        raise ValueError("sql_update_helper: 'set_cols' non può essere vuota")
    if not where_cols:
        raise ValueError("sql_update_helper: 'where_cols' non può essere vuota")
    set_part = ', '.join(f"{c}=%s" for c in set_cols)
    where_part = ' AND '.join(f"{c}=%s" for c in where_cols)
    return f"UPDATE {table} SET {set_part} WHERE {where_part}"

def sql_delete_helper(table: str, where_cols: list):
    """Descrizione:
    Costruisce una query DELETE parametrica con condizioni di uguaglianza.

    Input:
    - table (str)
    - where_cols (list[str]): colonne per condizioni (AND)

    Output:
    - str: 'DELETE FROM <table> WHERE c1=%s AND c2=%s'

    Eccezioni:
    - ValueError se where_cols è vuota.
    """
    if not where_cols:
        raise ValueError("sql_delete_helper: 'where_cols' non può essere vuota")
    where_part = ' AND '.join(f"{c}=%s" for c in where_cols)
    return f"DELETE FROM {table} WHERE {where_part}"

## validate_params rimosso: la validazione è interamente demandata a Marshmallow

# -------------------- Unified Error Schema Helpers -------------------- #
def api_error(code: str, message: str, status: int = 400, *, details: dict | None = None):
    """Descrizione:
    Crea una risposta JSON d'errore secondo lo schema uniforme.

    Input:
    - code (str): codice macchina UPPER_SNAKE_CASE
    - message (str): messaggio human readable
    - status (int): HTTP status (default 400)
    - details (dict|None): extra campi da unire in error

    Output:
    - (Flask Response, status)
    """
    from flask import jsonify
    payload = {"error": {"code": code, "message": message}}
    if details:
        payload["error"].update(details)
    return jsonify(payload), status

def is_manager(token):
    """Descrizione:
    Verifica se il token appartiene a un Manager e ritorna la riga Manager.

    Input:
    - token (str|None)

    Output:
    - dict|None: riga Manager oppure None se non valido/non Manager.
    """
    if not token:
        return None
    try:
        from .repository import get_user_by_token
        result = get_user_by_token(token)
        if not result or result.get('type') != 'Manager':
            return None
        return result.get('user')
    except Exception:
        return None

# -------------------- Decorators (Wrappers) -------------------- #
def manager_required(fn):
    """Descrizione:
    Decorator alias di role_required('Manager') che aggiunge 'manager' nei kwargs.

    Input:
    - fn (callable)

    Output:
    - callable decorato.
    """
    wrapped = role_required('Manager')(fn)
    from functools import wraps
    @wraps(wrapped)
    def adapter(*args, **kwargs):
        # current_user/current_type populated by role_required
        if 'current_user' in kwargs:
            kwargs.setdefault('manager', kwargs['current_user'])
        return wrapped(*args, **kwargs)
    # Mark to allow other decorators (e.g., manager_of_department) to detect prior wrapping
    adapter._manager_required = True  # type: ignore[attr-defined]
    return adapter

def manager_of_department(dept_field='id_dipartimento'):
    """Descrizione:
    Decorator che richiede token Manager valido e matching del dipartimento
    rispetto al campo nel payload.

    Input:
    - dept_field (str): nome chiave nel JSON (default 'id_dipartimento').

    Output:
    - callable decorator.

        Note:
        - Se il campo dipartimentale è assente o None -> solleva ValidationException
            con codice MISSING_PARAMS (400) invece di un generico errore di ruolo.
    """
    def outer(fn):
        from functools import wraps
        from flask import request
        from .repository import get_user_by_token
        from .exceptions import AuthException

        @wraps(fn)
        def wrapper(*args, **kwargs):
            data = request.get_json() or {}
            token = data.get('token')
            if not token:
                raise AuthException("AUTH_TOKEN_MISSING", "Token mancante", 401)

            result = get_user_by_token(token)
            if not result:
                raise AuthException("AUTH_TOKEN_INVALID", "Token non valido", 403)
            if result.get('type') != 'Manager':
                raise AuthException("AUTH_FORBIDDEN_ROLE", "Permesso negato: richiede ruolo Manager", 403)

            manager = result.get('user') or {}
            if dept_field not in data or data.get(dept_field) is None:
                # Uniformare: se manca il campo richiesto, usiamo lo stesso formato di marshmallow
                from .exceptions import ValidationException
                raise ValidationException(
                    "MISSING_PARAMS",
                    "Validazione fallita",
                    400,
                    {"fields": {dept_field: ["Missing data for required field."]}}
                )

            dept_req = data.get(dept_field)
            dept_mgr = manager.get('Dipartimento_id_dipartimento')
            try:
                same = int(dept_req) == int(dept_mgr)
            except Exception:
                same = str(dept_req) == str(dept_mgr)
            if not same:
                raise AuthException("AUTH_FORBIDDEN_DEPARTMENT", "Permesso negato: manager di altro dipartimento", 403)

            # Inject for downstream route logic (and compatibility with previous wrappers)
            kwargs['manager'] = manager
            kwargs['current_user'] = manager
            kwargs['current_type'] = 'Manager'

            return fn(*args, **kwargs)
        return wrapper
    return outer

def role_required(*roles):
    """Descrizione:
    Decorator generico che verifica che il token appartenga a un utente con tipo ammesso.

    Input:
    - roles (*str): ruoli ammessi.

    Output:
    - callable decorator (inietta current_user, current_type nei kwargs).
    """
    def decorator(fn):
        from functools import wraps
        from flask import request
        from .repository import get_user_by_token
        from .exceptions import AuthException

        role_set = set(r.lower() for r in roles)

        @wraps(fn)
        def wrapper(*args, **kwargs):
            data = request.get_json() or {}
            token = data.get('token')
            if not token:
                raise AuthException("AUTH_TOKEN_MISSING", "Token mancante", 401)
            result = get_user_by_token(token)
            if not result:
                raise AuthException("AUTH_TOKEN_INVALID", "Token non valido", 403)
            tipo = (result.get('type') or '').lower()
            if role_set and tipo not in role_set:
                raise AuthException("AUTH_FORBIDDEN_ROLE", "Permesso negato: ruolo non autorizzato", 403)
            kwargs['current_user'] = result.get('user')
            kwargs['current_type'] = result.get('type')
            return fn(*args, **kwargs)
        return wrapper
    return decorator

def admin_required(fn):
    """Descrizione:
    Shortcut alias di role_required('Admin').

    Input: fn (callable)
    Output: callable decorato.
    """
    return role_required('Admin')(fn)

def integrity_errors_project(exc):
    """Descrizione:
    Traduce errori di integrità (progetto) in eccezioni API strutturate.

    Input: exc (Exception) presumibilmente MySQL error.
    Output: (raise) ConflictException | ValidationException | ServerException.
    """
    from .exceptions import ConflictException, ValidationException, ServerException
    try:
        mysql_code = exc.args[0] if exc.args else None
    except Exception:
        raise ServerException("Errore di integrità database", details={"orig": str(exc)})

    if mysql_code == 1062:  # duplicate key
        raise ConflictException("DUPLICATE_PROJECT", "Il progetto è già registrato")
    if mysql_code in (1452, 1451):  # foreign key
        raise ValidationException("DB_INTEGRITY_ERROR", "Violazione di integrità referenziale", 400)

    raise ServerException("Errore database", details={"db_code": mysql_code})

def integrity_errors(exc):
    """Descrizione:
    Traduce errori di integrità (utente) in eccezioni API.

    Input: exc (Exception)
    Output: (raise) ConflictException | ValidationException | ServerException.
    """
    from .exceptions import ConflictException, ValidationException, ServerException
    try:
        mysql_code = exc.args[0] if exc.args else None
    except Exception:
        raise ServerException("Errore di integrità database", details={"orig": str(exc)})

    if mysql_code == 1062:
        raise ConflictException("DUPLICATE_USER", "L'utente è già registrato")
    if mysql_code in (1452, 1451):
        raise ValidationException("DB_INTEGRITY_ERROR", "Violazione di integrità referenziale", 400)

    raise ServerException("Errore database", details={"db_code": mysql_code})


def integrity_errors_department(exc):
    """Descrizione:
    Traduce errori di integrità specifici per la tabella Dipartimento.

    Mappa il duplicate key (1062) su DUPLICATE_DEPARTMENT per chiarezza API.
    """
    from .exceptions import ConflictException, ValidationException, ServerException
    try:
        mysql_code = exc.args[0] if exc.args else None
    except Exception:
        raise ServerException("Errore di integrità database", details={"orig": str(exc)})

    if mysql_code == 1062:
        raise ConflictException("DUPLICATE_DEPARTMENT", "Il dipartimento è già registrato")
    if mysql_code in (1452, 1451):
        raise ValidationException("DB_INTEGRITY_ERROR", "Violazione di integrità referenziale", 400)

    raise ServerException("Errore database", details={"db_code": mysql_code})


def decide_login(data, get_user_by_token_fn, get_manager_fn, get_dipendente_fn, crypt_fn):
    """Descrizione:
    Incapsula la logica di branching del login (token vs email/password).

    Input:
    - data (dict)
    - get_user_by_token_fn, get_manager_fn, get_dipendente_fn (callable): accesso dati.
    - crypt_fn (callable): funzione hashing password.

    Output:
    - tuple status: ('ok_token', token, type) | ('invalid_token',) | ('missing_credentials',)
      | ('ok', token, type) | ('invalid_credentials',)
    """
    token = data.get('token')
    if token:
        result = get_user_by_token_fn(token)
        if result:
            return ('ok_token', token, result['type'])
        return ('invalid_token',)

    email = data.get('email')
    password = data.get('password')
    if not email or not password:
        return ('missing_credentials',)

    # manager first
    manager = get_manager_fn(email)
    if manager and manager.get('password') == crypt_fn(password):
        return ('ok', manager.get('token'), 'Manager')

    # then dipendente
    user = get_dipendente_fn(email)
    if user and user.get('password') == crypt_fn(password):
        return ('ok', user.get('token'), 'Dipendente')

    return ('invalid_credentials',)
