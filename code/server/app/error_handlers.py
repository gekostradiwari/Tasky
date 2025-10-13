from werkzeug.exceptions import HTTPException
from marshmallow import ValidationError as MarshmallowValidationError
from .utils import api_error
from .exceptions import ApiException, NotFoundException, ServerException, ValidationException


def register_error_handlers(app):
    """Register global error handlers producing unified error schema.

    Codes:
      NOT_FOUND, METHOD_NOT_ALLOWED, HTTP_ERROR, UNHANDLED_EXCEPTION
    """

    @app.errorhandler(404)
    def _not_found(e):  # pragma: no cover
        return api_error("NOT_FOUND", "Risorsa non trovata", 404)

    @app.errorhandler(405)
    def _method_not_allowed(e):  # pragma: no cover
        return api_error("METHOD_NOT_ALLOWED", "Metodo non consentito", 405)

    @app.errorhandler(HTTPException)
    def _http_exception(e: HTTPException):  # pragma: no cover
        if e.code == 404:
            return _not_found(e)
        if e.code == 405:
            return _method_not_allowed(e)
        return api_error("HTTP_ERROR", getattr(e, 'description', None) or "Errore HTTP", e.code or 400)

    @app.errorhandler(ApiException)
    def _api_exception(e: ApiException):  # pragma: no cover
        return api_error(e.code, e.message, e.status, details=e.details)

    @app.errorhandler(MarshmallowValidationError)
    def _marshmallow_validation(e: MarshmallowValidationError):  # pragma: no cover
        # Uniforma i messaggi di marshmallow al codice MISSING_PARAMS
        return api_error("MISSING_PARAMS", "Validazione fallita", 400, details={"fields": e.messages})

    @app.errorhandler(Exception)
    def _unhandled(e):  # pragma: no cover
        # TODO: add logging / sentry integration here
        return api_error("UNHANDLED_EXCEPTION", "Errore interno del server", 500)
