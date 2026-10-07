# SyncCore benchmark report

Runtime: OpenJDK 64-Bit Server VM 21.0.8 (Eclipse Adoptium). OS: Mac OS X 26.4.1 aarch64. Available processors: 10.

Warm-ups per stage: 2. Measured samples: 5. Fixture generation excluded. Times are monotonic elapsed milliseconds.

| Identities | Conflicts | Operations | Stage | Median ms | Min ms | Empirical p95 ms |
| ---: | ---: | ---: | --- | ---: | ---: | ---: |
| 100 | 20 | 80 | change detection | 0.092625 | 0.080083 | 0.230709 |
| 100 | 20 | 80 | conflict detection | 0.061959 | 0.058417 | 0.131291 |
| 100 | 20 | 80 | planning (full pipeline) | 0.315708 | 0.279542 | 0.713833 |
| 100 | 20 | 80 | guarded application | 1.116292 | 0.89575 | 1.234 |
| 1000 | 200 | 800 | change detection | 0.545916 | 0.452084 | 1.514542 |
| 1000 | 200 | 800 | conflict detection | 0.593917 | 0.567709 | 0.636 |
| 1000 | 200 | 800 | planning (full pipeline) | 1.432792 | 1.4095 | 2.560875 |
| 1000 | 200 | 800 | guarded application | 3.850208 | 2.965375 | 7.302625 |
| 10000 | 2000 | 8000 | change detection | 6.243166 | 5.583584 | 7.518333 |
| 10000 | 2000 | 8000 | conflict detection | 4.112084 | 1.959625 | 5.595459 |
| 10000 | 2000 | 8000 | planning (full pipeline) | 16.597459 | 7.993541 | 23.272667 |
| 10000 | 2000 | 8000 | guarded application | 31.812208 | 25.183416 | 40.504542 |
| 100000 | 20000 | 80000 | change detection | 73.663416 | 65.349708 | 75.407166 |
| 100000 | 20000 | 80000 | conflict detection | 22.480333 | 8.884333 | 45.568042 |
| 100000 | 20000 | 80000 | planning (full pipeline) | 149.753459 | 138.410958 | 197.691167 |
| 100000 | 20000 | 80000 | guarded application | 347.186292 | 331.440167 | 423.623959 |

Planning includes change/conflict detection and resolution. Guarded application includes canonical replanning. Samples are not independent process forks; GC and runtime warm-up can affect values. With five samples empirical p95 equals the maximum. Portable memory/allocation metrics are not available.

JVM used heap sampled before suite: 24150680 bytes; after suite: 335446888 bytes. These are not peak-memory or allocation metrics, and no forced GC was used.
