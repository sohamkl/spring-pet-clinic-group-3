# Task 2 PR Plan (Sustainability Improvements)

This plan structures the required sustainability improvements into **separate, low-conflict PRs** that can be reviewed/approved by a peer (not the author) and merged into your fork’s `main`.

## Goals / Constraints (from assignment)

- **One PR per improvement criterion**:
  - Complexity (cyclomatic/cognitive)
  - Resource usage (try-with-resources)
  - Redundant / unused code
  - Imports (unused + avoid wildcard)
  - Documentation (3 service classes)
- **Use SonarQube/SonarCloud** to support evaluation and capture **screenshots / exports** for the Appendix.
- PRs must be **reviewed + approved by team members (not the author)**, then merged into **your fork’s `main`**.
- The Task 2 report section must explain each criterion (≤ **500 words each**) in the same report as Task 1, plus update the contribution table.

## Working Agreement (to avoid merge conflicts)

- **No “mega-formatting” PRs.** Avoid broad reformatting across many files.
- **Lock scope per PR**:
  - Each PR touches only files necessary for that criterion.
  - Prefer **small, targeted changes** over sweeping refactors.
- **Allocate ownership by file area**:
  - If a PR needs changes in a file owned by another PR, either (a) move that change to the correct PR, or (b) re-scope so only one PR edits that file.
- **Merge order is important**:
  - Merge “mechanical / safe” PRs (imports, unused code) before “behavioral” PRs (complexity refactors, try-with-resources), then documentation last.

## Branching / Naming Convention

Create one branch per criterion (and one optional “Sonar setup” branch if needed):

- `task2/sonar-setup` (optional, only if you add CI integration or project properties)
- `task2/imports-cleanup`
- `task2/unused-code-removal`
- `task2/try-with-resources`
- `task2/complexity-reduction`
- `task2/service-docs`

## PR Checklist Template (apply to every PR)

- **Sonar evidence**:
  - Before/after screenshot(s) of the specific issue type(s) addressed.
  - If possible, include a short “before/after issue count” note in the PR description.
- **Test**:
  - `./mvnw -B verify` passes locally (or in CI).
- **Scope**:
  - PR title clearly maps to **exactly one** criterion.
  - No unrelated cleanup.
- **Review**:
  - Author does **not** approve their own PR.
  - At least one peer approval before merge.

## Proposed PR Sequence (low conflict + fast merges)

### PR 0 (optional): Sonar integration / baseline

**Branch**: `task2/sonar-setup`  
**Owner**: Person A (you)  
**Purpose**: Ensure Sonar runs on PRs and `main`, so every subsequent PR has consistent evidence.

**Files likely touched**:
- GitHub Actions workflow (`.github/workflows/...`) and/or `sonar-project.properties` (only if needed).

**Why first**: Prevents rework and makes every PR’s evidence easy to capture.

---

### PR 1: Remove unnecessary imports (and avoid wildcard imports)

**Branch**: `task2/imports-cleanup`  
**Owner**: Person B (peer)  
**Scope rules**:
- Fix **unused imports** and **wildcard imports** only.
- Do not refactor logic.

**Conflict avoidance**:
- Restrict to the smallest set of files reported by Sonar for imports.
- If a file is likely to be edited later for complexity refactor, prefer to leave it for that PR unless Sonar flags imports as a blocker.

---

### PR 2: Avoid redundant coding (remove unused code)

**Branch**: `task2/unused-code-removal`  
**Owner**: Person A (you)  
**Scope rules**:
- Remove dead/unused private methods, unreachable branches, unused fields, unused classes if truly unused.
- Keep public API stable (avoid breaking external references).

**Conflict avoidance**:
- Avoid deleting or rewriting methods that will be refactored for complexity later; instead, coordinate which PR owns which file.

---

### PR 3: Minimizing resource usage (try-with-resources)

**Branch**: `task2/try-with-resources`  
**Owner**: Person B (peer)  
**Scope rules**:
- Apply try-with-resources for closeable resources (streams/readers/etc.) where Sonar flags potential leaks.
- Do not alter behavior beyond safe resource handling.

**Conflict avoidance**:
- Prefer focusing on a small number of files (e.g., utility classes, CSV/file handling, any custom IO).

---

### PR 4: Reduce complexity (cyclomatic / cognitive complexity)

**Branch**: `task2/complexity-reduction`  
**Owner**: Person A (you)  
**Scope rules**:
- Target top Sonar issues (highest cognitive complexity).
- Refactor by extracting methods, simplifying conditionals, using early returns, and reducing nesting.
- Avoid changing functionality; add/adjust tests if needed.

**Conflict avoidance**:
- Do not edit the same files used heavily in PR 3 unless required; if needed, merge PR 3 first.

---

### PR 5: Documentation (3 service-module classes)

**Branch**: `task2/service-docs`  
**Owner**: Person B (peer)  
**Scope rules**:
- Pick **three classes in the service layer/module** (e.g., service classes in the app’s “service” package).
- Add/strengthen documentation focusing on intent, contracts, constraints, and usage (not narration).

**Conflict avoidance**:
- Only touch those 3 classes (and possibly their package-info if appropriate).
- Do this last to avoid doc conflicts during refactors.

## Suggested Ownership Split (2 people)

- **Person A (you)**: PR 0 (optional), PR 2, PR 4  
- **Person B (peer)**: PR 1, PR 3, PR 5  

This split minimizes overlap: Person B handles “mechanical” fixes + docs, Person A handles “behavioral refactor” (complexity) and targeted dead code removal.

## Merge Strategy

- Merge in this order (unless Sonar evidence dictates otherwise):
  1. PR 0 (optional)
  2. PR 1 (imports)
  3. PR 2 (unused code)
  4. PR 3 (try-with-resources)
  5. PR 4 (complexity)
  6. PR 5 (docs)
- Always **rebase/merge `main` into the branch** before final review if `main` moved.

## Report Writing Plan (Task 2 section)

For each criterion (≤ 500 words):

- **Issue**: What Sonar reported (include rule name/ID if available).
- **Impact**: Why it hurts sustainability (maintainability, defects, performance/resource risk).
- **Fix**: What changed (briefly) and why it is safe.
- **Evidence**: Reference PR + Sonar screenshot(s) in Appendix.

Also update the **student contribution table** with PR ownership, review actions, and merges.

