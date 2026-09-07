# ML service image: FastAPI on Python 3.13.
#
# Single stage. The dependencies here are wheels (numpy, scikit-learn, pandas)
# with nothing to compile, so a build stage would save nothing worth the
# indirection.

FROM python:3.13-slim

ENV TZ=Asia/Ho_Chi_Minh \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

# Runtime dependencies only. requirements-dev.txt (pytest, httpx test client)
# has no place in a shipped image.
WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Includes artifacts/: the trained model and its metrics are committed, so the
# image needs neither a training run nor database access to serve (ADR-0003).
COPY ml_service/ ml_service/

RUN useradd --system --uid 10001 app && chown -R app:app /app
USER app

EXPOSE 8000

# Bound to every interface because the container's own address is not known
# here; reachability is the network's job, and this service is deliberately
# not published outside it.
CMD ["python", "-m", "uvicorn", "ml_service.main:app", "--host", "0.0.0.0", "--port", "8000"]
