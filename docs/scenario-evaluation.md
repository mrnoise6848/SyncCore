# Deterministic scenario evaluation

Actual Android/JVM host runs: 395. Passed: 395. Cases containing conflicts: 150.
Converged outcomes: 335. Safely unresolved outcomes: 60.

Includes 75 catalog/policy combinations and 320 exhaustive snapshot/policy combinations.
Each evaluated run checks replay, plan/result JSON round-trip and either replica agreement or explicit unresolved conflicts.
The 320-case grid also checks conflict presence against independent three-way divergence predicates.
These finite-case results do not claim correctness for all possible inputs, device UI behavior or production transport.
