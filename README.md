# HNBLUE

English · [简体中文](README.zh-CN.md)

HNBLUE is a full-stack blue-carbon research prototype that brings together environmental data exploration, model inference and explanation, interactive visualization, application APIs, and knowledge-assisted analysis.

The repository demonstrates system integration. It is not an operational carbon-accounting platform, and the included experiments do not establish production accuracy, scientific generalizability, or high-availability performance.

## Components

| Component | Technology | Role |
|---|---|---|
| `backend/` | Java 17, Spring Boot, JPA, MySQL, Redis, JWT, MQTT | APIs, persistence, authentication, and integration |
| `frontend/` | Vue 3, ECharts | Maps, time-series views, comparisons, and reports |
| `flask_model/` | Flask, CatBoost, scikit-learn | Model inference, response curves, and analysis scripts |
| `redis_cluster/` | Redis configuration and shell scripts | Local cluster experiments for rate limiting and locking |
| `docs/` | Markdown reports | Architecture, APIs, features, tests, and experiment notes |
| `figures/` | Generated images | Model and interface illustrations |

Knowledge-assisted exploration can be connected to an external OpenAI-compatible model or a local knowledge tool, but that integration is environment-dependent and optional.

## Architecture

```text
Vue / ECharts
      |
      v
Spring Boot API ---- MySQL
      |       \
      |        -> Redis experiment cluster
      |        -> MQTT integration points
      v
Flask model service -> CatBoost inference / explanation artifacts

Optional external knowledge assistant
```

## Local setup

The repository contains several independently started components rather than a single turnkey installer.

Backend:

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
npm ci
npm run serve
```

Model service:

```powershell
python -m pip install -r flask_model/carbon_model_api/requirements.txt
python flask_model/carbon_model_api/app.py
```

MySQL, Redis, MQTT, and optional model-provider settings must be supplied locally. Review the module documentation and configuration files before starting an integrated environment.

## Evidence and limitations

- Stored model outputs and figures document experiments; they are not a substitute for independent validation.
- No model metric in this repository should be read as guaranteed performance on new ecosystems or operational data.
- Redis scripts represent a cluster experiment, not proof of sustained production concurrency or availability.
- Sample datasets may carry their own provenance and reuse conditions.
- External AI responses require source checking and should not be treated as scientific authority.

## Security

Do not commit database passwords, JWT signing material, mail credentials, MQTT credentials, model API keys, internal addresses, or private datasets. Use local environment-specific configuration and rotate any credential that has ever been exposed.

## License

The repository software is available under the [Apache License 2.0](LICENSE). Third-party datasets, model artifacts, images, and dependencies may have separate terms.