# Changelog

### v2.5.0 - **unreleased**

* feat: sets up better unix signal handler, so that when JVM exit is triggered by SIGTERM or SIGKILL and system exits cleanly, exit code 0 is used. 

### v2.4.0

* feat: simplify development & test system management code by using `c.t.namespace` better

### v2.3.0 (2025-07-11)

*   feat: Add simple Jetty component and tests
*   feat: Add recurring scheduling modes for scheduled thread pool executor
*   fix: Make tests pass in JDK11
*   chore: Update dependencies and project upkeep

### v2.2.2 (2025-05-12)

*   fix: Simplified lifecycle implementation to ensure shutdown hooks are executed
*   chore: Brought back reporting of failed hook executions
*   chore: Updated dependencies and improved Makefile

### v2.2.1 (2025-05-01)

*   feat: Added support for virtual threads in the concurrent component
*   fix: Corrected CI configuration
*   chore: Added a Makefile and addressed linter issues

### v2.2.0 (2025-04-24)

*   feat: Introduced a scheduler component and wrappers for `java.util.concurrent`
*   feat: Replaced `print*` with `clojure.tools.logging` for better logging
*   test: Added tests for the scheduler and fixed the CI workflow
*   docs: Added more documentation

### v2.1.0 (2025-01-10)

*   Initial public release under the new Maven coordinates.

---

## Pre-v2 Era (as `nomnom/utility-belt`)

### v1.3.3 (2023-03-02)

*   Added `utility-belt.base64` for base64 encoding/decoding.
*   Added `utility-belt.sanitization` for data sanitization (email, spaces).
*   Deprecated functions in `utility-belt.id` that are now in `clojure.core`.

### v1.3.2 (2021-11-30)

*   Patch release with minor fixes.

### v1.3.1 (2020-11-18)

*   Fixed an email validation pattern.

### v1.3.0 (2020-06-22)

*   Updated dependencies and added logging setup.

### v1.2.3 (2020-05-19)

*   Snapshot release with minor changes.

### v1.2.2 (2020-01-31)

*   Bumped dependencies.

### v1.2.1 (2019-10-24)

*   CI setup and dependency updates.

### v1.2.0 (2019-10-20)

*   Initial import from a private repository.
