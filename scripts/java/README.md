# Manual Java maintenance utilities

These legacy utilities were moved out of `src/test/java`. They are not compiled
or discovered by the unit-test suite.

- `com/landawn/abacus/CodeHelper.java` contains the original parameter-check
  source rewriting helper, without its former JUnit annotation.
- `com/landawn/abacus/Maven.java` contains the original local artifact-copying
  utility.

Their existing paths and behavior are preserved for manual maintenance.
