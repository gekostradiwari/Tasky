from flask import Flask, send_from_directory, redirect
from flask.json.provider import DefaultJSONProvider
from datetime import date, datetime


class _TaskyJSONProvider(DefaultJSONProvider):
    def default(self, o):
        if isinstance(o, (date, datetime)):
            return o.strftime('%d/%m/%Y')
        return super().default(o)

def create_app():
    """Create and configure the Flask application."""
    app = Flask(__name__)

    # Serialize all date/datetime values as dd/MM/yyyy
    app.json_provider_class = _TaskyJSONProvider
    app.json = app.json_provider_class(app)

    # Initialize centralized logging (must be first)
    from .logging_config import setup_logging
    setup_logging(app)
    app.logger.info("Application startup - creating Flask app")

    # Register blueprints
    from .routes import bp as api_bp
    app.register_blueprint(api_bp)
    app.logger.info("API blueprint registered")

    # Initialize time-triggered scheduler (RAD 5.1.B)
    from .scheduler import init_scheduler
    init_scheduler(app)

    # Simple route to serve the testing UI (static files in app/tester)
    # Serve testing UI; ensure trailing slash so relative assets resolve as /tester/<file>
    @app.route('/tester')
    def tester_redirect():
        # Redirect /tester -> /tester/ so that 'tester.css' is resolved to /tester/tester.css
        return redirect('/tester/', code=302)

    @app.route('/tester/')
    def tester_index():
        return send_from_directory('tester', 'tester.html')

    @app.route('/tester/<path:asset>')
    def tester_assets(asset):
        return send_from_directory('tester', asset)

    @app.route('/logs/<filename>')
    def get_logs(filename):
        import os
        # Allow only specific log files for security
        if filename not in ['app.log', 'error.log', 'scheduler.log']:
            return "Access denied", 403
        log_dir = os.path.join(app.root_path, '..', 'logs')
        return send_from_directory(log_dir, filename)

    @app.route('/health')
    def health():
        """Simple health check: 200 if DB reachable, otherwise 403.

        Returns JSON: {"status": "ok"|"unhealthy", "db": "up"|"down"}
        """
        try:
            from .db import get_db_connection
            conn = get_db_connection()
            try:
                with conn.cursor() as cursor:
                    cursor.execute('SELECT 1')
                    cursor.fetchone()
            finally:
                conn.close()
            app.logger.debug("Health check passed - DB connection OK")
            return {"status": "ok", "db": "up"}, 200
        except Exception as e:
            app.logger.error(f"Health check failed - DB connection error: {e}")
            return {"status": "unhealthy", "db": "down", "error": str(e)[:200]}, 403

    # Register global error handlers
    from .error_handlers import register_error_handlers
    register_error_handlers(app)

    app.logger.info("Application startup completed successfully")
    return app
