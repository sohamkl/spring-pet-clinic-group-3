# Task 2 — Sustainability (report scaffold)

**Course:** SOFTENG-754 (same document as Task 1; label this section **Task 2**).  
**Constraint:** Each criterion write-up **≤ 500 words**.  
**Appendix:** SonarQube/SonarCloud **screenshots or exported reports** (before/after where useful).

**For each criterion in the final report:** list **every refactored file** as a **repo-relative path** (e.g. `src/main/java/.../Foo.java`, `src/main/resources/db/postgres/data.sql`). Copy the same list into the **Changelog** when you merge.

---

##  Submission checklist


| Requirement                                                                       | Done |
| --------------------------------------------------------------------------------- | ---- |
| Code changes in **separate PRs** (one per criterion below)                        | ☐    |
| Each PR **reviewed/approved by teammate** (not author); merged to fork `**main`** | ☐    |
| **No PRs** to upstream `spring-projects/spring-petclinic`                         | ☐    |
| Tool evaluation evidence in **Appendix**                                          | ☐    |
| **Task 2** discussion in **same report as Task 1** (copy from here or merge)      | ☐    |
| **Student contribution table** updated for Task 2 (same table as Task 1)          | ☐    |


---

## Sonar / tooling (brief)

- **Tool:** SonarCloud (or SonarQube) — project: *[fill]*  
- **Baseline:** *[branch/commit or date]*  
- **Note:** Quality gate optional for narrative; criteria below drive fixes.

---

## Criterion 1 — Reduce complexity (cyclomatic / cognitive)

**Sonar / rules (examples):** cognitive complexity, nested branches, collapsible `if`, etc.  
**PR:** *[link / branch name]*

Refactored `Owner`, `OwnerController`, and `PetController` methods to reduce conditional nesting and remove duplicated controller logic/literals.


| Item                      | Notes                                                                      |
| ------------------------- | -------------------------------------------------------------------------- |
| **Refactored files**      | `src/main/java/org/springframework/samples/petclinic/owner/Owner.java`, `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java`, `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` |
| **Issue**                 | Sonar-style maintainability issues: collapsible nested `if` conditions, duplicated owner lookup/validation logic, and repeated hard-coded redirect/error literals in controller flows. |
| **Sustainability impact** | Harder to change, higher defect risk, slower onboarding.                   |
| **Change**                | Consolidated conditional checks in `Owner#getPet` overloads; extracted shared owner lookup in `PetController` (`findRequiredOwner`); merged nested name-duplicate check in `processUpdateForm`; extracted repeated future birth-date validation into a helper method reused by create/update paths; extracted repeated redirect paths and duplicated error-message literals into top-level controller constants. |
| **Evidence**              | Appendix fig. *[n]*; PR checks green.                                      |


*≤ 500 words when pasted into final report.*

---

## Criterion 2 — Resource usage (try-with-resources)

**Sonar / rules (examples):** resources should be closed, try-with-resources.  
**PR:** *[link / branch name]*


| Item                      | Notes                                                      |
| ------------------------- | ---------------------------------------------------------- |
| **Refactored files**      | *[paths]*                                                  |
| **Issue**                 | Closeable not closed / leak risk.                          |
| **Sustainability impact** | FD leaks, connection pressure, flaky behaviour under load. |
| **Change**                | try-with-resources (or equivalent) on which types/paths.   |
| **Evidence**              | Appendix fig. *[n]*.                                       |


*≤ 500 words.*

---

## Criterion 3 — Redundant / unused code

**Sonar / rules (examples):** dead code, unused private members, unreachable code.  
**PR:** *[link / branch name]*


| Item                      | Notes                                                  |
| ------------------------- | ------------------------------------------------------ |
| **Refactored files**      | *[paths]*                                              |
| **Issue**                 | What was redundant / unused.                           |
| **Sustainability impact** | Noise for readers, false assumptions, merge conflicts. |
| **Change**                | Removed or inlined what; API impact if any.            |
| **Evidence**              | Appendix fig. *[n]*.                                   |


*≤ 500 words.*

---

## Criterion 4 — Imports (unused + no wildcard imports)

**Sonar / rules (examples):** java:S1128 unused imports; wildcard / star imports if enabled.  
**PR:** *[link / branch name]*


| Item                      | Notes                                                       |
| ------------------------- | ----------------------------------------------------------- |
| **Refactored files**      | *[paths]*                                                   |
| **Issue**                 | Wildcards / unused imports (files touched).                 |
| **Sustainability impact** | Unclear dependencies, merge noise, accidental name clashes. |
| **Change**                | Explicit static imports; removed unused lines.              |
| **Evidence**              | Appendix fig. *[n]*.                                        |


*≤ 500 words.*

---

## Criterion 5 — Documentation (three **service** classes)

**PR:** *[link / branch name]*  
**Classes documented (exact FQCN or paths):**

1. *[package.ClassName]* — *[1 sentence: what doc adds]*
2. *[package.ClassName]* — *[…]*
3. *[package.ClassName]* — *[…]*


| Item                      | Notes                                                               |
| ------------------------- | ------------------------------------------------------------------- |
| **Refactored files**      | *[paths to the 3 (+ any package-info) classes]*                     |
| **Issue**                 | Missing or thin Javadoc / module intent.                            |
| **Sustainability impact** | Contract and usage unclear for maintainers.                         |
| **Change**                | Class/method summaries, pre/post conditions, non-obvious behaviour. |
| **Evidence**              | Appendix (optional diff excerpt or Sonar doc rule if any).          |


*≤ 500 words.*

---

## Changelog (add a row after each merge)


| Date       | Criterion       | PR  | **Refactored files**                                                                       | Summary (one line)                                |
| ---------- | --------------- | --- | ------------------------------------------------------------------------------------------ | ------------------------------------------------- |
| 2026-05-16 | Reduce complexity | #…  | `src/main/java/org/springframework/samples/petclinic/owner/Owner.java`                     | Refactor Owner class methods for improved readability by consolidating conditional checks in getPet methods. |
| 2026-05-16 | Reduce complexity | #…  | `src/main/java/org/springframework/samples/petclinic/owner/PetController.java`             | Extract helper method for owner lookup and reuse it in `findOwner` and `findPet` to remove duplicated logic. |
| 2026-05-16 | Reduce complexity | #…  | `src/main/java/org/springframework/samples/petclinic/owner/PetController.java`             | Merge nested conditional in `processUpdateForm` and extract shared birth-date validation helper for create/update flows. |
| 2026-05-16 | Reduce complexity | #…  | `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java`, `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` | Extract repeated redirect/error literals into controller constants to reduce duplication and improve maintainability. |
| YYYY-MM-DD | Imports         | #…  | `src/test/java/.../OwnerControllerTests.java`, `src/test/java/.../VetControllerTests.java` | e.g. explicit `MockMvcResultMatchers` imports     |
| YYYY-MM-DD | SQL / Sonar SQL | #…  | `src/main/resources/db/postgres/data.sql`                                                  | e.g. `SELECT 1` in `NOT EXISTS` (SelectStarCheck) |
|            |                 |     |                                                                                            |                                                   |


---

## Appendix — Sonar evidence (for PDF/report)

- Fig. A1: Issues filtered *[rule/criterion]* — **before**  
- Fig. A2: Same filter — **after**  
- *(Add exports or PR decoration screenshots as needed.)*

---

## Contribution table (Task 2 row — copy into Task 1 table)


| Task   | Member   | Role (e.g. author / reviewer) | PR(s) | Notes |
| ------ | -------- | ----------------------------- | ----- | ----- |
| Task 2 | *[name]* |                               |       |       |
| Task 2 | *[name]* |                               |       |       |


