#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
rm -rf build/classes build/fat
mkdir -p build/classes build/fat
javac --release 17 --add-modules jdk.httpserver -cp "dependencies/*" -d build/classes src/*.java
cp -r build/classes/* build/fat/
cp -r web build/fat/web
for jar in dependencies/*.jar; do
  (cd build/fat && jar xf "../../$jar")
done
rm -f build/fat/META-INF/*.SF build/fat/META-INF/*.RSA build/fat/META-INF/*.DSA 2>/dev/null || true
printf 'Main-Class: Main\n' > build/manifest.mf
jar cfm TaskTracker.jar build/manifest.mf -C build/fat .
echo "Built TaskTracker.jar"
