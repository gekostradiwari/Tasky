from .repository import (
    handle_project, handle_task,
    handle_numero_dipendenti,
    handle_dipendenti_by_department,
    handle_dipendenti_data_by_department,
    insertProject, insertTask
)
from .db import get_db_connection
from .repository import handle_dipendenti_by_project, get_user_by_token

def add_project(data):
    """Wrapper manager: costruisce args e delega a repository.handle_project.

    Restituisce la Response (jsonify, status) prodotta da handle_project.
    """
    def dip_args(d):
        return (
            d.get("id_progetto"),
            d.get("descrizione"),
            d.get("budget"),
            d.get("nome"),
            str(d.get("data_inizio")),
            str(d.get("data_fine")),
            d.get("id_dipartimento")
        )
    return handle_project(
        data,
        insert_func=insertProject,
        args_builder=dip_args,
        success_message="Progetto inserito correttamente"
    )

def add_task(data):
    """Wrapper manager: costruisce args e delega a repository.handle_task."""
    def dip_args(d):
        return (
            d.get("id"),
            d.get("stato"),
            d.get("descrizione"),
            str(d.get("data_inizio")),
            str(d.get("data_fine")),
            d.get("id_progetto"),
            d.get("email_dipendente"),
            d.get("email_manager")
        )
    return handle_task(
        data,
        insert_func=insertTask,
        args_builder=dip_args,
        success_message="Task inserita correttamente"
    )

def get_numero_dipendenti(data):
    return handle_numero_dipendenti(data, get_db_connection_fn=get_db_connection)

def get_dipendenti_summary(data):
    return handle_dipendenti_by_department(data, get_db_connection_fn=get_db_connection)

def get_dipendenti_data(data):
    return handle_dipendenti_data_by_department(data, get_db_connection_fn=get_db_connection)

def get_dipendenti_by_project(data):
    """Manager wrapper for fetching employees assigned to a project.

    This function delegates to repository.handle_dipendenti_by_project and uses
    repository.get_user_by_token and the module-level get_db_connection.
    It's intended to be called only after request-level manager checks (e.g. the
    `manager_of_department` decorator) but performs the final delegation here to
    keep routing thin.
    """
    return handle_dipendenti_by_project(
        data,
        get_user_by_token_fn=get_user_by_token,
        get_db_connection_fn=get_db_connection
    )


def get_managers_by_project(data):
    """Wrapper manager per ottenere i manager associati a un progetto."""
    from .repository import handle_managers_by_project, get_user_by_token
    return handle_managers_by_project(
        data,
        get_user_by_token_fn=get_user_by_token,
        get_db_connection_fn=get_db_connection
    )

def register_manager(data):
    """Manager wrapper for registering a Manager.

    Builds args expected by repository.insertManager and delegates to handle_register
    by reusing the repository-level helper. This keeps routes thin and centralizes
    registration logic for Managers in the manager layer.
    """
    from .repository import handle_register, insertManager

    def _args_builder(d):
        return (
            d.get('email'),
            # store hashed password consistently with dipendente flow
            __import__('builtins') and __import__('importlib').import_module('.utils', package='app').crypt(d.get('password')),
            d.get('nome'),
            d.get('cognome'),
            d.get('data_nascita'),
            d.get('anni_lavorativi'),
            d.get('Dipartimento_id_dipartimento'),
            d.get('sesso'),
            d.get('numero_telefono'),
        )

    # delegate to repository.handle_register which returns (jsonify, status)
    return handle_register(
        data,
        insert_func=insertManager,
        args_builder=_args_builder,
        success_message='Manager registered successfully'
    )


def add_department(data):
    """Wrapper manager per la creazione di un Dipartimento.

    Si occupa di costruire la tupla di argomenti attesa dall'insertDipartimento e di
    delegare la creazione al repository tramite handle_insert_dipartimento.
    """
    from .repository import handle_insert_dipartimento, insertDipartimento

    def args_builder(d):
        return (
            d.get('id_dipartimento'),
            d.get('nome'),
            d.get('numero_dipendenti', 0),
        )

    return handle_insert_dipartimento(
        data,
        insert_func=insertDipartimento,
        args_builder=args_builder,
        success_message='Dipartimento creato correttamente'
    )
