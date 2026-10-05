# Upstream Source Notice

## Root Activity Launcher

This project includes activity-launching logic adapted from:

**Root Activity Launcher**
https://github.com/zacharee/RootActivityLauncher

The upstream GitHub repository identifies the project as GPL-3.0.

The adapted portions in this project were used as a starting point for the
privileged/alternate activity-launching implementation, including the
Shizuku/Assistant-related execution approach. The code in this repository has
been modified for the narrower purpose of launching the Samsung fingerprint
activities used by this project.

Upstream source and license:
- Repository: https://github.com/zacharee/RootActivityLauncher
- License: GNU General Public License v3.0

## License handling

Because GPL-covered upstream code is incorporated in modified form, this
repository distributes the resulting project under GPL-3.0-only. The complete
GPLv3 text is provided in `LICENSE`.

## Other dependencies

Other third-party libraries are not claimed as original project code and are
listed separately in `THIRD-PARTY-NOTICES.md`.

## SenseTouch modifications (2026)

SenseTouch is based on FingerprintAccuracyEnhancer:
https://github.com/asdfasdf-asdfasdf/FingerprintAccuracyEnhancer
Copyright (C) 2026 FingerprintAccuracyEnhancer contributors.

Adam's SenseTouch changes: new branding and Compose interface, device checks,
explicit Shizuku setup/status, narrowed hidden API exemptions, durable assistant
recovery journal, verified setting writes, and GitHub build workflows.
Original copyright and license notices remain applicable.

Base commit: `392f84a631f931efed3511214bfd28c8e0c16cb2`.

The notice formerly in `LaunchEngine.kt` is preserved here:

Copyright (C) 2026 FingerprintAccuracyEnhancer contributors.
Modified for SenseTouch by Adam, 2026. GPL-3.0-only.
Assistant/Shizuku approach adapted from Root Activity Launcher.
See this notice and LICENSE.
