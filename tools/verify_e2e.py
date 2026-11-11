#!/usr/bin/env python3
"""Verifikasi hasil e2e: test benar-benar berjalan dan tanpa kegagalan.

Latar belakang: AGP/UTP pernah menelan kegagalan instalasi APK
(INSTALL_FAILED_UPDATE_INCOMPATIBLE akibat instalasi manual di workflow)
sebagai BUILD SUCCESSFUL tanpa menjalankan satu pun test instrumented app —
E2E "hijau" padahal kosong (run #4–#8). Skrip ini memfail build bila:
  - tidak ada hasil XML connectedAndroidTest sama sekali,
  - jumlah testcase < minimum, atau
  - ada failure/error.

Dipanggil dari .github/workflows/e2e.yml setelah ./gradlew
connectedDebugAndroidTest. ScreenshotTest DIKECUALIKAN dari e2e lewat argumen
instrumentation `notClass` (milik workflow Screenshots).
"""
from __future__ import annotations

import glob
import sys
import xml.etree.ElementTree as ET

# 9 test e2e (BrowserSmokeTest + ExplorerFlowTest); ScreenshotTest dikecualikan
# lewat notClass di e2e.yml karena milik workflow Screenshots.
MIN_TESTS = 9
PATTERN = "app/build/outputs/androidTest-results/**/*.xml"


def main() -> None:
    files = sorted(glob.glob(PATTERN, recursive=True))
    if not files:
        sys.exit("GAGAL: tidak ada hasil XML connectedAndroidTest — test tidak benar-benar berjalan")

    total = failed = errors = skipped = 0
    for path in files:
        root = ET.parse(path).getroot()
        total += int(root.get("tests", 0))
        failed += int(root.get("failures", 0))
        errors += int(root.get("errors", 0))
        skipped += int(root.get("skipped", 0))
        print(f"  {path}: tests={root.get('tests')} failures={root.get('failures')} "
              f"errors={root.get('errors')} skipped={root.get('skipped')}")

    print(f"e2e total: {total} test, {failed} failure, {errors} error, {skipped} skipped "
          f"({len(files)} file XML)")

    if total < MIN_TESTS:
        sys.exit(f"GAGAL: jumlah test {total} < minimum {MIN_TESTS} — kemungkinan test tidak lengkap berjalan")
    if failed or errors:
        sys.exit(f"GAGAL: ada {failed} failure + {errors} error")
    print("e2e: OK — semua test benar-benar berjalan dan lulus")


if __name__ == "__main__":
    main()
