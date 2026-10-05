#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v java >/dev/null || { echo "Java 17+ is required."; exit 1; }
major=$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
[ "$major" -ge 17 ] || { echo "Java 17+ is required."; exit 1; }
mkdir -p "$HOME/.local/share/tasktracker" "$HOME/.local/bin" "$HOME/.local/share/applications"
cp TaskTracker.jar "$HOME/.local/share/tasktracker/TaskTracker.jar"
cat > "$HOME/.local/bin/tasktracker" <<LAUNCH
#!/usr/bin/env bash
exec java -jar "$HOME/.local/share/tasktracker/TaskTracker.jar"
LAUNCH
chmod +x "$HOME/.local/bin/tasktracker"
cat > "$HOME/.local/share/applications/tasktracker.desktop" <<DESKTOP
[Desktop Entry]
Type=Application
Name=TaskTracker
Exec=$HOME/.local/bin/tasktracker
Terminal=false
Categories=Utility;
DESKTOP
echo "TaskTracker installed. Run 'tasktracker' or open it from your app menu."
