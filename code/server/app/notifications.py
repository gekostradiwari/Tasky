import os
from typing import Iterable, Dict, Any, List

_firebase_app = None

def _ensure_initialized():
    global _firebase_app
    if _firebase_app is not None:
        return _firebase_app
    cred_path = os.environ.get('FCM_CREDENTIALS_PATH')
    if not cred_path or not os.path.exists(cred_path):
        # Lazy: allow running without FCM; raise when sending
        _firebase_app = False
        return _firebase_app
    try:
        import firebase_admin
        from firebase_admin import credentials
        cred = credentials.Certificate(cred_path)
        _firebase_app = firebase_admin.initialize_app(cred)
        return _firebase_app
    except Exception:
        _firebase_app = False
        return _firebase_app


def is_ready() -> bool:
    return bool(_ensure_initialized())


def send_to_tokens(tokens: Iterable[str], title: str, body: str, data: Dict[str, str] | None = None) -> Dict[str, Any]:
    """Send a notification to a list of FCM tokens. Returns summary with invalid tokens.

    data must be a dict[str,str] per FCM constraints.
    """
    app = _ensure_initialized()
    if not app:
        raise RuntimeError("FCM not initialized or credentials missing")
    from firebase_admin import messaging
    tokens = [t for t in (tokens or []) if t]
    if not tokens:
        return {"success": 0, "failure": 0, "invalid": []}

    # Normalize data to strings as required by FCM
    data_str = {k: str(v) for k, v in (data or {}).items()}

    # Try modern API first
    if hasattr(messaging, 'send_multicast') and hasattr(messaging, 'MulticastMessage'):
        msg = messaging.MulticastMessage(
            notification=messaging.Notification(title=title, body=body),
            data=data_str,
            tokens=tokens,
        )
        resp = messaging.send_multicast(msg)
        invalid: List[str] = []
        for idx, r in enumerate(resp.responses):
            if not r.success:
                code = getattr(r.exception, 'code', '')
                if code == 'messaging/registration-token-not-registered':
                    invalid.append(tokens[idx])
        return {"success": resp.success_count, "failure": resp.failure_count, "invalid": invalid}

    # Fallback: send_all if available
    if hasattr(messaging, 'send_all'):
        messages = [
            messaging.Message(
                token=t,
                notification=messaging.Notification(title=title, body=body),
                data=data_str,
            ) for t in tokens
        ]
        resp = messaging.send_all(messages)
        # send_all returns BatchResponse similar to send_multicast
        invalid: List[str] = []
        success = 0
        failure = 0
        for idx, r in enumerate(resp.responses):
            if r.success:
                success += 1
            else:
                failure += 1
                code = getattr(r.exception, 'code', '')
                if code == 'messaging/registration-token-not-registered':
                    invalid.append(tokens[idx])
        return {"success": success, "failure": failure, "invalid": invalid}

    # Legacy fallback: per-token send
    success = 0
    failure = 0
    invalid: List[str] = []
    for t in tokens:
        try:
            messaging.send(
                messaging.Message(
                    token=t,
                    notification=messaging.Notification(title=title, body=body),
                    data=data_str,
                )
            )
            success += 1
        except Exception as e:
            failure += 1
            code = getattr(e, 'code', '')
            if code == 'messaging/registration-token-not-registered':
                invalid.append(t)
    return {"success": success, "failure": failure, "invalid": invalid}
