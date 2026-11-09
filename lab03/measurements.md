# Task 4: UI Responsiveness and Load Measurements

## Environment & Hardware

- **CPU**: AMD Ryzen 9 8945HS w/ Radeon 780M Graphics (8 cores / 16 threads, up to 5.2 GHz)
- **RAM**: 32 GB (31.3 GB usable, ~0.7 GB reserved for iGPU)
- **OS**: Windows 11 Home (64-bit, build 26300)
- **JDK**: Eclipse Temurin OpenJDK 25.0 (build 25+36-LTS)
- **JavaFX**: 25.0.4

## Measurement Method

Measurements were captured using `LoadBenchmarkApp` with the command:

```bash
mvn -q javafx:run "-Dapp.main=edu.itc.salesreport.ui.LoadBenchmarkApp" "-Djavafx.args=--data=data --month=2026-08 --warmup=2 --runs=5"
```

The test data contains the complete month of August 2026 (93 files, 465,000 rows across PNH, REP, and BTB, generated with seed 42). Frame intervals were recorded with `FrameGapMonitor` attached to the JavaFX `AnimationTimer` pulse cycle (baseline ~16.7 ms at 60 Hz). Each mode was executed for 2 warm-up iterations followed by 5 recorded runs.

## Benchmark Results

| Mode                    |          Run          |    Load (ms)    | Max frame gap (ms) |
| :---------------------- | :--------------------: | :--------------: | :----------------: |
| caller-thread           |           1           |       378       |        457        |
| caller-thread           |           2           |       393       |        456        |
| caller-thread           |           3           |       447       |        515        |
| caller-thread           |           4           |       427       |        472        |
| caller-thread           |           5           |       366       |        405        |
| **caller-thread** | **Mean / Worst** | **402 ms** |  **515 ms**  |
| fx-task                 |           1           |       355       |         17         |
| fx-task                 |           2           |       379       |         21         |
| fx-task                 |           3           |       365       |         18         |
| fx-task                 |           4           |       365       |         19         |
| fx-task                 |           5           |       381       |         17         |
| **fx-task**       | **Mean / Worst** | **369 ms** |  **21 ms**  |

## Manual Responsiveness Observation

When testing by hand with a heavy dataset (20,000 rows per day), the difference between the two modes was immediately apparent. Under `CallerThreadJobs`, the UI froze solid for over a second during generation: window dragging was unresponsive, text entered into the product filter search box appeared only after the data finished parsing, and clicking Cancel had no effect while the thread was blocked. Under `FxBackgroundJobs`, the JavaFX Application Thread remained completely fluid at ~60 frames per second throughout loading. We were able to freely type queries into the product filter, smoothly drag and reposition the window, and pressing Cancel immediately halted further branch processing and left the previously loaded summaries intact.
