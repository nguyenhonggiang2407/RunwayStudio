# RS01 — Reproduce the application baseline

Application revision [`2a4d31b`](https://github.com/nguyenhonggiang2407/RunwayStudio/tree/2a4d31bd566d77e16f1edde3c043182ed3816997). The Windows run used Temurin 17.0.21+9 and Node 24.19.0. [Suite results](evidence/RS01-local-baseline.json).

| Command | Result |
| --- | --- |
| `./mvnw.cmd -B clean verify` | Exit 0. 94 tests passed. The Spring Boot JAR was built. |
| `npm ci --ignore-scripts` | Exit 0. Installed the locked UI test dependencies. |
| `npm run check` | Exit 0. Checked 14 modules, 140 original-photo hashes and two licensed editorial variants. |
| `npm run test:e2e` | Exit 0. 18 tests passed against the local JAR on desktop and two mobile widths. |
| `npm audit --audit-level=high` | Exit 0. npm reported zero findings for the UI test dependency tree. |
| `python tools/dependency_audit.py --fail-on-high` | Exit 0. OSV scanned 81 Maven runtime packages. Two findings were retained with existing triage records and zero high, critical or unknown findings remained unreviewed. [Raw evidence](evidence/dependency-audit.json). |

Set `JAVA_HOME` to a JDK 17 installation and put its `bin` first on `PATH` before running the Maven wrapper and browser suite. Install Chromium using `npx playwright install chromium` if the test browser is missing. The browser runner starts its own temporary H2-backed Java process on loopback and uses fictional accounts and orders. The test suite exercises the cart, checkout with a voucher, outfit persistence, scene editing and role boundaries.

| Environment | Storage and startup | Evidence boundary |
| --- | --- | --- |
| Local `demo` | H2 file at `data/runway`, loopback binding and local fixtures | The automated tests use isolated H2. |
| Render portfolio demo | Docker, H2 in memory, HTTPS cookies and ephemeral state | T02 checked public navigation and readiness on 6 November. [Smoke report](https://github.com/nguyenhonggiang2407/nguyenhonggiang2407/blob/codex/T02-live-demo-smoke/docs/evidence/demo-smoke-2026-11-06/report.md#runwaystudio). |
| `mysql,prod` | MySQL 8.4 and Flyway, with seed disabled and operator-provided credentials | Use the MySQL CI job and a disposable schema for acceptance. Docker and MySQL were unavailable in the local run. |

PowerShell requires quoting the entire Maven property containing the output path. Without quotes, this environment passed `.txt` as a lifecycle phase and Maven exited 1. The following form passes each property as one argument:

```powershell
.\mvnw.cmd -B dependency:list '-DincludeScope=runtime' '-DoutputFile=target/runtime-dependencies.txt'
```

The existing [deployment instructions](DEPLOYMENT.md) describe schema and migration checks for MySQL. Run `./mvnw -B -Pmysql-verify verify` only with the two disposable schemas that the acceptance tests expect. [Existing MySQL CI on the inspected application revision](https://github.com/nguyenhonggiang2407/RunwayStudio/actions/runs/42038940707).
