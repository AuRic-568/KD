# TaskTracker

TaskTracker is a local task manager with a browser-based UI plus the original Java CLI commands.

## For normal Windows users

Download **`TaskTracker-Windows-Installer.exe`** from the repository's GitHub Release, run it, and launch TaskTracker from the Start Menu or desktop shortcut.

The Windows installer is **self-contained**. Users do not need to install Java, JDK, Node.js, or any dependencies.

The app opens its UI in the default browser and runs only on `127.0.0.1` on the user's own computer. Task data stays locally at:

`%USERPROFILE%\.tasktracker\tasks.jsonl`

## Features

- Add, edit and delete tasks
- Todo / In Progress / Done states
- Search, filtering and sorting
- Persistent local storage
- Browser UI served locally only
- Original CLI support
- Self-contained Windows installer
- Self-contained Linux and macOS builds via GitHub Actions

## Publishing on GitHub

The repository contains `.github/workflows/build.yml`.

When the repo is uploaded to GitHub, the workflow can be run manually from **Actions → Build TaskTracker → Run workflow** to verify all builds.

To publish downloadable installers, create and push a version tag, for example:

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions will then create a GitHub Release containing:

- `TaskTracker-Windows-Installer.exe` — Windows installer with bundled runtime
- `TaskTracker-Linux.tar.gz`
- `TaskTracker-macOS.tar.gz`
- `TaskTracker.jar`

The Windows build runs on GitHub's Windows runner and uses `jpackage` to make the actual `.exe` installer.

## Developers / source build

The repository already includes the Jackson JAR dependencies.

### Windows

```powershell
.\build.bat
java -jar TaskTracker.jar
```

### Linux / macOS

```bash
./build.sh
java -jar TaskTracker.jar
```

Running the JAR with no arguments launches the UI.

## CLI mode

```bash
java -jar TaskTracker.jar add "Buy groceries"
java -jar TaskTracker.jar update 0 "Buy groceries and cook dinner"
java -jar TaskTracker.jar mark-in-progress 0
java -jar TaskTracker.jar mark-done 0
java -jar TaskTracker.jar list
java -jar TaskTracker.jar list done
java -jar TaskTracker.jar delete 0
```
