"""
Centralized logging configuration for TaskyAPI.

Implements:
- JSON structured logging for machine parsing
- Rotating file handlers (app.log, error.log, scheduler.log)
- Automatic redaction of sensitive data (passwords, tokens)
- Configurable log levels via environment
"""

import logging
import logging.handlers
import json
import os
import re
from datetime import datetime
from pathlib import Path


class SensitiveDataFilter(logging.Filter):
    """Filter to redact sensitive information from log records."""
    
    # Patterns to redact
    SENSITIVE_PATTERNS = [
        (re.compile(r'"password"\s*:\s*"[^"]*"'), '"password":"[REDACTED]"'),
        (re.compile(r'"token"\s*:\s*"[^"]*"'), '"token":"[REDACTED]"'),
        (re.compile(r'"fcm_token"\s*:\s*"[^"]*"'), '"fcm_token":"[REDACTED]"'),
        (re.compile(r'password=\'[^\']*\''), 'password=\'[REDACTED]\''),
        (re.compile(r'password="[^"]*"'), 'password="[REDACTED]"'),
        (re.compile(r'token=\'[^\']*\''), 'token=\'[REDACTED]\''),
        (re.compile(r'Bearer\s+[A-Za-z0-9\-_\.]+'), 'Bearer [REDACTED]'),
    ]
    
    def filter(self, record):
        """Redact sensitive data from log message."""
        if hasattr(record, 'msg') and isinstance(record.msg, str):
            for pattern, replacement in self.SENSITIVE_PATTERNS:
                record.msg = pattern.sub(replacement, record.msg)
        return True


class JSONFormatter(logging.Formatter):
    """Format log records as JSON for structured logging."""
    
    def format(self, record):
        log_data = {
            'timestamp': datetime.utcnow().isoformat() + 'Z',
            'level': record.levelname,
            'logger': record.name,
            'message': record.getMessage(),
            'module': record.module,
            'function': record.funcName,
            'line': record.lineno,
        }
        
        # Add exception info if present
        if record.exc_info:
            log_data['exception'] = self.formatException(record.exc_info)
        
        # Add extra fields if present
        if hasattr(record, 'extra_data'):
            log_data['extra'] = record.extra_data
        
        return json.dumps(log_data, ensure_ascii=False)


class SimpleFormatter(logging.Formatter):
    """Human-readable log format for console/simple viewing."""
    
    def format(self, record):
        timestamp = datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S')
        return f"{timestamp} [{record.levelname:8}] {record.name:25} - {record.getMessage()}"


def setup_logging(app=None, log_level=None):
    """
    Configure application-wide logging.
    
    Args:
        app: Flask app instance (optional, for app.logger)
        log_level: Override log level (default: from env or INFO)
    
    Creates:
        - logs/app.log: All application logs (JSON, rotating)
        - logs/error.log: ERROR and above only (JSON, rotating)
        - logs/scheduler.log: Scheduler-specific logs (JSON, rotating)
        - Console: Human-readable output for development
    """
    # Determine log level
    level_name = log_level or os.environ.get('LOG_LEVEL', 'INFO').upper()
    level = getattr(logging, level_name, logging.INFO)
    
    # Create logs directory
    log_dir = Path(__file__).parent.parent / 'logs'
    log_dir.mkdir(exist_ok=True)
    
    # Root logger configuration
    root_logger = logging.getLogger()
    root_logger.setLevel(level)
    
    # Remove existing handlers to avoid duplicates
    root_logger.handlers.clear()
    
    # Sensitive data filter (applied to all handlers)
    sensitive_filter = SensitiveDataFilter()
    
    # 1. Console Handler (human-readable, for development)
    console_handler = logging.StreamHandler()
    console_handler.setLevel(level)
    console_handler.setFormatter(SimpleFormatter())
    console_handler.addFilter(sensitive_filter)
    root_logger.addHandler(console_handler)
    
    # 2. App Log Handler (all logs, JSON, rotating)
    app_log_path = log_dir / 'app.log'
    app_handler = logging.handlers.RotatingFileHandler(
        app_log_path,
        maxBytes=10 * 1024 * 1024,  # 10 MB
        backupCount=5,
        encoding='utf-8'
    )
    app_handler.setLevel(level)
    app_handler.setFormatter(JSONFormatter())
    app_handler.addFilter(sensitive_filter)
    root_logger.addHandler(app_handler)
    
    # 3. Error Log Handler (ERROR and above, JSON, rotating)
    error_log_path = log_dir / 'error.log'
    error_handler = logging.handlers.RotatingFileHandler(
        error_log_path,
        maxBytes=10 * 1024 * 1024,  # 10 MB
        backupCount=5,
        encoding='utf-8'
    )
    error_handler.setLevel(logging.ERROR)
    error_handler.setFormatter(JSONFormatter())
    error_handler.addFilter(sensitive_filter)
    root_logger.addHandler(error_handler)
    
    # 4. Scheduler Log Handler (scheduler module only, JSON, rotating)
    scheduler_log_path = log_dir / 'scheduler.log'
    scheduler_handler = logging.handlers.RotatingFileHandler(
        scheduler_log_path,
        maxBytes=5 * 1024 * 1024,  # 5 MB
        backupCount=3,
        encoding='utf-8'
    )
    scheduler_handler.setLevel(logging.INFO)
    scheduler_handler.setFormatter(JSONFormatter())
    scheduler_handler.addFilter(sensitive_filter)
    
    # Only add scheduler logs to scheduler.log
    scheduler_logger = logging.getLogger('app.scheduler')
    scheduler_logger.addHandler(scheduler_handler)
    
    # Suppress noisy third-party loggers
    logging.getLogger('werkzeug').setLevel(logging.WARNING)
    logging.getLogger('apscheduler').setLevel(logging.WARNING)
    
    # Log startup
    logger = logging.getLogger(__name__)
    logger.info(f"Logging initialized - Level: {level_name}, Logs dir: {log_dir}")
    
    if app:
        app.logger.info(f"Flask app logging configured - Level: {level_name}")
    
    return root_logger


def get_recent_logs(log_file='app.log', lines=100, level_filter=None):
    """
    Retrieve recent log entries from a log file.
    
    Args:
        log_file: Name of log file (app.log, error.log, scheduler.log)
        lines: Number of recent lines to retrieve
        level_filter: Optional level filter (ERROR, WARNING, INFO, etc.)
    
    Returns:
        List of log entries (parsed JSON if applicable)
    """
    log_dir = Path(__file__).parent.parent / 'logs'
    log_path = log_dir / log_file
    
    if not log_path.exists():
        return []
    
    try:
        with open(log_path, 'r', encoding='utf-8') as f:
            all_lines = f.readlines()
        
        # Get last N lines
        recent_lines = all_lines[-lines:]
        
        # Parse JSON and filter by level if specified
        parsed_logs = []
        for line in recent_lines:
            try:
                log_entry = json.loads(line.strip())
                if level_filter is None or log_entry.get('level') == level_filter:
                    parsed_logs.append(log_entry)
            except json.JSONDecodeError:
                # Non-JSON line (shouldn't happen with JSONFormatter, but handle it)
                parsed_logs.append({'message': line.strip(), 'level': 'UNKNOWN'})
        
        return parsed_logs
    except Exception as e:
        logging.getLogger(__name__).error(f"Error reading log file {log_file}: {e}")
        return []
