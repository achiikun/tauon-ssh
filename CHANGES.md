# Tauon 3.3.0-SNAPSHOT

## Features

- Download files
- Fixed export and import zip backups
- Change logo color, blue is too confusing when using Teams.
- Updated Jediterm to version 3.53
- Mac support x11 (solution copied blindly from @devlinx9): https://github.com/achiikun/tauon-ssh/commit/020b542819f21ecd6a4d5ac95bb083200233457c
- Local terminal (just as Muon @devlinx9 ;)

## Fixes

- Fix a deadlock while transferring files
- Forwarded ports don't remain open after closing sessions anymore
- Remove Language option from configuration
- Change name dialog (@devlinx9): https://github.com/devlinx9/muon-ssh/commit/daf53003696eaf3c35591992f7eb6e7a2d68ba42
- Use root site manager tree (@devlinx9): https://github.com/devlinx9/muon-ssh/commit/02fc2c4510f76e86ac76f83900a635477426a60a & https://github.com/devlinx9/muon-ssh/commit/13260808cf687a94e9154f6ce63c6d1ffa44e266
- Export & import everything from zip: https://github.com/devlinx9/muon-ssh/commit/868f89f2d2e194449fd06cb5eac7c9d9e53d2e32
- Rewritten site import (not overwriting the existing ones)
- Ask before deleting (in local file system): https://github.com/achiikun/tauon-ssh/commit/e30940472cb0ab0901027281d5bfb9cd8c45a106#diff-0f86d11b223f406bafaea4cf3ccf2f3a73e44c2db04c4553533a565709625e3d

### TODO

- Doc: https://github.com/achiikun/tauon-ssh/commit/d98d3945fb7bb0540c3ba11c550d7c3fbf11ec46

# Tauon 3.2.1

- New fonts were included.
- More refactorings and stability.

# Tauon 3.2.0

- Jediterm is in an external repository.
- Dropped support for all languages except English.
- Simplified update checker.
- New collapsed session panel. ([PR #3](https://github.com/achiikun/tauon-ssh/pull/3))
- More refactorings and stability.

# Tauon 3.1.0

- Added SUDO capabilities in disk space analyzer
- Fixed unoptimized loop & execute with environment variables in SudoUtils

