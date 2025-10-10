class ApiException(Exception):
    """Base custom exception carrying an API error payload.

    Attributes:
        code (str): Stable machine-readable error code (UPPER_SNAKE)
        message (str): Human readable localized message
        status (int): HTTP status code
        details (dict|None): Optional extra context
    """
    def __init__(self, code: str, message: str, status: int = 400, details: dict | None = None):
        super().__init__(message)
        self.code = code
        self.message = message
        self.status = status
        self.details = details or {}

    def to_dict(self):
        data = {"code": self.code, "message": self.message}
        if self.details:
            data.update(self.details)
        return data


class AuthException(ApiException):
    pass


class ValidationException(ApiException):
    pass


class NotFoundException(ApiException):
    def __init__(self, message: str = "Risorsa non trovata", details: dict | None = None):
        super().__init__("NOT_FOUND", message, 404, details)


class ConflictException(ApiException):
    def __init__(self, code: str, message: str, details: dict | None = None):
        super().__init__(code, message, 409, details)


class IntegrityException(ApiException):
    pass


class ServerException(ApiException):
    def __init__(self, message: str = "Errore interno del server", details: dict | None = None):
        super().__init__("SERVER_ERROR", message, 500, details)


def raise_if(condition: bool, exc: ApiException):
    if condition:
        raise exc