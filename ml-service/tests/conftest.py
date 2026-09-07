import os

# ml_service.main reads ML_SERVICE_TOKEN at import time and refuses to start
# without it (ADR-0008) — this has to run before any test module imports
# ml_service.main, which is exactly when pytest loads conftest.py. Not a
# secret: nothing here reaches a real deployment.
os.environ.setdefault("ML_SERVICE_TOKEN", "test-only-token")
