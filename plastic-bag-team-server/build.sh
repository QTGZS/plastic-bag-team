#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
OUT=out
rm -rf "$OUT"
mkdir -p "$OUT"
echo ">>> 编译 Java 源码 ..."
javac -d "$OUT" src/org/yhteam/server/*.java
echo ">>> 复制资源文件 ..."
cp resources/index.html "$OUT"/
cp resources/style.css "$OUT"/
cp resources/app.js "$OUT"/
cp resources/api-docs.html "$OUT"/
cp resources/API文档.md "$OUT"/
echo ">>> 打包 jar ..."
jar cfe plastic-bag-team-server.jar org.yhteam.server.Server -C "$OUT" .
echo ">>> 完成: plastic-bag-team-server.jar"
ls -lh plastic-bag-team-server.jar
