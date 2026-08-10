# ML Service

Python + FastAPI service. Stateless ML/data-science computation for the
platform.

**Owns**: feature engineering, model training, model evaluation, and
forecast generation logic. Exposes an internal API consumed only by the
Java backend.

**Does not own**: persistence of platform state, authentication, or any
API surface exposed to the frontend. Has no direct PostgreSQL access — see
`docs/adr/0003-ml-service-data-access.md` for why, and the open question
it leaves for revisiting if training-data throughput ever requires it.

Not yet scaffolded. Will be initialized with FastAPI plus
pandas/NumPy/scikit-learn (TensorFlow/PyTorch only if a specific model
justifies it) when ML work actually begins.
