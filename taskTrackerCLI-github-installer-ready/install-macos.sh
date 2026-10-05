#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v java >/dev/null || { echo "Java 17+ is required."; exit 1; }
mkdir -p "$HOME/Applications/TaskTracker" "$HOME/.local/bin"
cp TaskTracker.jar "$HOME/Applications/TaskTracker/TaskTracker.jar"
cat > "$HOME/.local/bin/tasktracker" <<LAUNCH
#!/usr/bin/env bash
exec java -jar "$HOME/Applications/TaskTracker/TaskTracker.jar"
LAUNCH
chmod +x "$HOME/.local/bin/tasktracker"
echo "TaskTracker installed. Run ~/.local/bin/tasktracker"
