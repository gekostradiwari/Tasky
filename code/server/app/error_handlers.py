from werkzeug.exceptions import HTTPException
from marshmallow import ValidationError as MarshmallowValidationError
from .utils import api_error
from .exceptions import ApiException, NotFoundException, ServerException, ValidationException
import logging
import traceback

logger = logging.getLogger(__name__)


def register_error_handlers(app):
    """Register global error handlers producing unified error schema.

    Codes:
      NOT_FOUND, METHOD_NOT_ALLOWED, HTTP_ERROR, UNHANDLED_EXCEPTION
    
    All errors are logged with appropriate context for debugging and monitoring.
    """

    @app.errorhandler(404)
    def _not_found(e):  # pragma: no cover
        logger.warning(f"404 Not Found: {e}")
        return api_error("NOT_FOUND", "Risorsa non trovata", 404)

    @app.errorhandler(405)
    def _method_not_allowed(e):  # pragma: no cover
        logger.warning(f"405 Method Not Allowed: {e}")
        return api_error("METHOD_NOT_ALLOWED", "Metodo non consentito", 405)

    @app.errorhandler(HTTPException)
    def _http_exception(e: HTTPException):  # pragma: no cover
        logger.warning(f"HTTP Exception {e.code}: {e.description}")
        if e.code == 404:
            return _not_found(e)
        if e.code == 405:
            return _method_not_allowed(e)
        return api_error("HTTP_ERROR", getattr(e, 'description', None) or "Errore HTTP", e.code or 400)

    @app.errorhandler(ApiException)
    def _api_exception(e: ApiException):  # pragma: no cover
        # Log level depends on status code
        if e.status >= 500:
            logger.error(f"ApiException [{e.code}]: {e.message} | Details: {e.details}")
        elif e.status >= 400:
            logger.warning(f"ApiException [{e.code}]: {e.message} | Details: {e.details}")
        else:
            logger.info(f"ApiException [{e.code}]: {e.message}")
        return api_error(e.code, e.message, e.status, details=e.details)

    @app.errorhandler(MarshmallowValidationError)
    def _marshmallow_validation(e: MarshmallowValidationError):  # pragma: no cover
        # Uniforma i messaggi di marshmallow al codice MISSING_PARAMS
        logger.info(f"Validation error: {e.messages}")
        return api_error("MISSING_PARAMS", "Validazione fallita", 400, details={"fields": e.messages})

    @app.errorhandler(Exception)
    def _unhandled(e):  # pragma: no cover
        # Log full traceback for unhandled exceptions
        logger.error(
            f"UNHANDLED EXCEPTION: {type(e).__name__}: {str(e)}\n"
            f"Traceback:\n{traceback.format_exc()}"
        )
        return api_error("UNHANDLED_EXCEPTION", "Errore interno del server", 500)
