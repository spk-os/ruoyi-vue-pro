#!/usr/bin/env bash
# SPK-OS: 修复 npm #4828 bug 导致的 napi-rs native binding 漏装
# 适用：yudao-ui-admin-vue3 用 npm install 后，rolldown/oxc-parser 的 optional 平台包装到错误位置
# 用法：bash fix-native-bindings.sh （在 yudao-ui-admin-vue3 目录，npm install 之后执行）
set -e
cd "$(dirname "$0")/yudao-ui/yudao-ui-admin-vue3"
NPM="/usr/local/bin/npm"
REG="--registry=https://registry.npmmirror.com --legacy-peer-deps --no-save"

# 1. rolldown binding
$NPM install @rolldown/binding-linux-x64-gnu@1.0.0-rc.17 $REG >/dev/null 2>&1 || true
cp -f node_modules/@rolldown/binding-linux-x64-gnu/rolldown-binding.linux-x64-gnu.node node_modules/rolldown/dist/rolldown-binding.linux-x64-gnu.node
mkdir -p node_modules/rolldown/node_modules/@rolldown && cp -r node_modules/@rolldown/binding-linux-x64-gnu node_modules/rolldown/node_modules/@rolldown/

# 2. oxc-parser binding
$NPM install @oxc-parser/binding-linux-x64-gnu@0.131.0 $REG >/dev/null 2>&1 || true
cp -f node_modules/@oxc-parser/binding-linux-x64-gnu/parser.linux-x64-gnu.node node_modules/oxc-parser/src-js/parser.linux-x64-gnu.node
mkdir -p node_modules/oxc-parser/node_modules/@oxc-parser && cp -r node_modules/@oxc-parser/binding-linux-x64-gnu node_modules/oxc-parser/node_modules/@oxc-parser/

echo "✅ native bindings 已修复 (rolldown + oxc-parser)"
