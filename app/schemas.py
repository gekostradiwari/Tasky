from marshmallow import Schema, fields, validate, ValidationError

# Common validators
NonEmptyStr = fields.Str(required=True, validate=validate.Length(min=1))
DateStr = fields.Date(required=True, format="%Y-%m-%d")

class RegisterDipendenteSchema(Schema):
    email = fields.Email(required=True)
    password = fields.Str(required=True, validate=validate.Length(min=3))
    nome = NonEmptyStr
    cognome = NonEmptyStr
    data_nascita = DateStr
    Dipartimento_id_dipartimento = fields.Integer(required=True, strict=True)

class RegisterManagerSchema(RegisterDipendenteSchema):
    anni_lavorativi = fields.Integer(required=True, strict=True, validate=validate.Range(min=0))

class LoginSchema(Schema):
    token = fields.Str(load_default=None)
    email = fields.Email(load_default=None)
    password = fields.Str(load_default=None)

    def validate_input(self, data):
        # At least token OR (email & password)
        if not data.get('token') and not (data.get('email') and data.get('password')):
            raise ValidationError("Devi fornire token oppure coppia email/password", field_name="credentials")
        return data

    def load(self, data, *args, **kwargs):  # override to inject custom cross-field validation
        loaded = super().load(data, *args, **kwargs)
        return self.validate_input(loaded)

class ProjectCreateSchema(Schema):
    token = fields.Str(required=True)
    id_progetto = fields.Integer(load_default=None)
    descrizione = NonEmptyStr
    budget = fields.Decimal(required=True, as_string=True)
    nome = NonEmptyStr
    data_inizio = DateStr
    data_fine = DateStr
    id_dipartimento = fields.Integer(required=True, strict=True)


class DepartmentCreateSchema(Schema):
    """Schema per la creazione di un Dipartimento.

    Campi:
    - token: token del Manager che richiede l'operazione (verificato a monte)
    - id_dipartimento: opzionale, inserimento esplicito
    - nome: nome del dipartimento (required)
    - numero_dipendenti: opzionale, default 0
    """
    token = fields.Str(required=True)
    id_dipartimento = fields.Integer(load_default=None)
    nome = NonEmptyStr
    numero_dipendenti = fields.Integer(load_default=0)

class TaskCreateSchema(Schema):
    token = fields.Str(required=True)
    id = fields.Integer(load_default=None)
    stato = NonEmptyStr
    descrizione = NonEmptyStr
    data_inizio = DateStr
    data_fine = DateStr
    id_progetto = fields.Integer(required=True, strict=True)
    id_dipartimento = fields.Integer(required=True, strict=True)
    email_dipendente = fields.Email(required=True)
    email_manager = fields.Email(required=True)

class ProjectVisibilitySchema(Schema):
    token = fields.Str(required=True)
    id_dipartimento = fields.Integer(required=True, strict=True)

class TaskVisibilitySchema(Schema):
    token = fields.Str(required=True)
    id_progetto = fields.Integer(required=True, strict=True)
    id_dipartimento = fields.Integer(load_default=None)  # richiesto solo se manager


class EmployeeProjectsSchema(Schema):
    email_dipendente = fields.Email(required=True)


class ProjectBudgetSchema(Schema):
    id_progetto = fields.Integer(required=True, strict=True)


class ProjectUpdateSchema(Schema):
    token = fields.Str(required=True)
    id_progetto = fields.Integer(required=True, strict=True)
    # Campi aggiornabili (tutti opzionali, almeno uno richiesto in route)
    descrizione = fields.Str(load_default=None)
    budget = fields.Decimal(load_default=None, as_string=True)
    nome = fields.Str(load_default=None)
    data_inizio = fields.Date(load_default=None, format="%Y-%m-%d")
    data_fine = fields.Date(load_default=None, format="%Y-%m-%d")
    id_dipartimento = fields.Integer(load_default=None, strict=True)


class ProjectDeleteSchema(Schema):
    token = fields.Str(required=True)
    id_progetto = fields.Integer(required=True, strict=True)
    id_dipartimento = fields.Integer(load_default=None, strict=True)  # per controllo dipartimento


class TaskUpdateSchema(Schema):
    token = fields.Str(required=True)
    id = fields.Integer(required=True, strict=True)
    id_progetto = fields.Integer(required=True, strict=True)
    id_dipartimento = fields.Integer(load_default=None, strict=True)
    stato = fields.Str(load_default=None)
    descrizione = fields.Str(load_default=None)
    data_inizio = fields.Date(load_default=None, format="%Y-%m-%d")
    data_fine = fields.Date(load_default=None, format="%Y-%m-%d")
    email_dipendente = fields.Email(load_default=None)
    email_manager = fields.Email(load_default=None)


class TaskDeleteSchema(Schema):
    token = fields.Str(required=True)
    id = fields.Integer(required=True, strict=True)
    id_progetto = fields.Integer(required=True, strict=True)
    id_dipartimento = fields.Integer(load_default=None, strict=True)
