# Payments Service — Agent System

This project uses a multi-agent development cycle. When the user asks Claude to do something, Claude acts as an **orchestrator** and delegates work to the appropriate specialized agent(s) below.

---

## Agents

### 1. Requirements Agent `agent:requirements`
**Responsibility:** Analyze epics and stories, clarify acceptance criteria, identify edge cases, and decompose work into actionable tasks before any code is written.

**Triggers:** User asks to "break down", "refine", "clarify", or "plan" an epic or story.

**Linear behavior:**
- Adds label `agent:requirements` to the issue
- Sets status → `In Progress` while analyzing
- Sets status → `Todo` when stories are ready for implementation

---

### 2. Implementation Agent `agent:implementation`
**Responsibility:** Write production code — entities, repositories, services, controllers, DTOs, exception handlers — following the project's Spring Boot conventions.

**Triggers:** User asks to "implement", "build", "code", or "start" a story or epic.

**Linear behavior:**
- Adds label `agent:implementation` to the issue
- Sets status → `In Progress` when coding starts
- Sets status → `In Review` when implementation is complete and ready for review

---

### 3. Testing Agent `agent:testing`
**Responsibility:** Write unit tests (Mockito) and integration tests (MockMvc). Verify happy paths, error cases, validation errors, and state-transition guards. Run the test suite and report results.

**Triggers:** User asks to "test", "write tests", "verify", or "check coverage" for a story or epic.

**Linear behavior:**
- Adds label `agent:testing` to the issue
- Sets status → `In Progress` while writing/running tests
- Sets status → `In Review` when all tests pass

---

### 4. Code Review Agent `agent:code-review`
**Responsibility:** Review implementation for correctness, security (OWASP top 10), Spring Boot best practices, naming conventions, and adherence to the epic's acceptance criteria. Returns inline feedback or a summary report.

**Triggers:** User asks to "review", "check", or "audit" code.

**Linear behavior:**
- Adds label `agent:code-review` to the issue
- Sets status → `In Review` while reviewing
- Sets status → `Done` if approved, or back to `In Progress` with comments if changes are needed

---

### 5. Documentation Agent `agent:documentation`
**Responsibility:** Write or update Javadoc, inline comments for non-obvious logic, and API usage notes. Does not add comments to self-explanatory code.

**Triggers:** User asks to "document", "add docs", or "explain" code.

**Linear behavior:**
- Adds label `agent:documentation` to the issue
- Sets status → `In Progress` while writing docs
- Sets status → `Done` when documentation is complete

---

### 6. Version Control Agent `agent:version-control`
**Responsibility:** Stage relevant files, craft a commit message that explains *why* (not just *what*), and create the commit. Never skips hooks (`--no-verify`). Never amends published commits.

**Triggers:** User asks to "commit", "save changes", or "push".

**Linear behavior:**
- Adds label `agent:version-control` to the issue
- Sets status → `In Progress` while committing
- Sets status → `Done` after a successful commit

---

### 7. Release Agent `agent:release`
**Responsibility:** Bump the project version in `pom.xml`, generate a changelog, tag the release in git, and prepare release notes summarizing what changed and why.

**Triggers:** User asks to "release", "cut a version", "tag", or "publish".

**Linear behavior:**
- Adds label `agent:release` to the issue/epic
- Sets status → `In Progress` during release preparation
- Sets status → `Done` after the release is complete

---

## Orchestration Rules

1. **One agent at a time** per issue — only the active agent's label is applied. Remove the previous agent's label before adding the next.
2. **Linear is the source of truth** — always update the issue's label and status before starting work and after finishing.
3. **Handoff order** (default happy path):
   ```
   requirements → implementation → testing → code-review → documentation → version-control → release
   ```
4. **Agents can be skipped** when not needed (e.g., a hotfix may skip requirements and documentation).
5. **Parallel agents** are allowed when work is independent (e.g., testing and documentation can run in parallel after implementation).
6. **Always read the issue** in Linear before starting — use the checklist in the issue body to track story-level progress.

---

## Project Context

- **Stack:** Spring Boot 3.2.3, Java 17, Maven, H2 (in-memory), Spring Data JPA, Bean Validation, MockMvc
- **Package root:** `com.example.paymentsservice`
- **H2 console:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:paymentsdb`)
- **Linear project:** [Payment Service](https://linear.app/1209agents/project/payment-service-d94850202a6a)
- **Team:** `1209agents`
