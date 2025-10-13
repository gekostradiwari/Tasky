########## Base runtime stage ##########
FROM python:3.11-slim AS runtime
WORKDIR /app

# install system deps for mysql client
RUN apt-get update \
    && apt-get install -y --no-install-recommends default-libmysqlclient-dev build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY . /app

ENV FLASK_APP=api.py
ENV FLASK_RUN_HOST=0.0.0.0

########## Docs build stage ##########
FROM runtime AS docs

# Install documentation dependencies separately to keep base image slim if not used
COPY requirements-docs.txt ./
RUN pip install --no-cache-dir -r requirements-docs.txt \
    && sphinx-build -b html docs docs/_build/html

########## Final image (can choose to include docs) ##########
FROM runtime AS final

# Optionally copy pre-built docs (uncomment if you want them inside final image)
COPY --from=docs /app/docs/_build/html /app/docs/_build/html

EXPOSE 5000
CMD ["python", "api.py"]
