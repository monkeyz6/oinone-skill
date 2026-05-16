#!/usr/bin/env bash
#
# 一键安装 Oinone/Pamirs slash commands 到 Codex CLI（无需 git clone）。
#
# 安装：
#   curl -fsSL https://raw.githubusercontent.com/monkeyz6/oinone-skill/main/bootstrap-codex.sh | bash
#
# 卸载：
#   curl -fsSL https://raw.githubusercontent.com/monkeyz6/oinone-skill/main/bootstrap-codex.sh | bash -s -- --uninstall
#
# 可选环境变量：
#   OINONE_REPO=<owner/repo>   GitHub 仓库，默认 monkeyz6/oinone-skill
#   OINONE_REF=<ref>           分支或 tag，默认 main
#   CODEX_HOME=<dir>           Codex 配置目录，默认 $HOME/.codex
#

set -euo pipefail

REPO="${OINONE_REPO:-monkeyz6/oinone-skill}"
REF="${OINONE_REF:-main}"
DST="${CODEX_HOME:-$HOME/.codex}/prompts"

MODE="install"
case "${1:-}" in
  "") ;;
  --uninstall) MODE="uninstall" ;;
  -h|--help)
    cat <<'EOF'
用法：
  curl -fsSL https://raw.githubusercontent.com/<owner>/<repo>/main/bootstrap-codex.sh | bash
  curl -fsSL https://... | bash -s -- --uninstall

环境变量：
  OINONE_REPO   GitHub owner/repo（默认 monkeyz6/oinone-skill）
  OINONE_REF    分支或 tag（默认 main）
  CODEX_HOME    Codex 配置目录（默认 ~/.codex）
EOF
    exit 0
    ;;
  *)
    echo "未知参数: $1" >&2
    echo "用法: bootstrap-codex.sh [--uninstall]" >&2
    exit 2
    ;;
esac

if [ "$MODE" = "uninstall" ]; then
  count=0
  for f in "$DST"/oinone-*.md; do
    if [ -e "$f" ] || [ -L "$f" ]; then
      rm -f "$f"
      echo "  remove $(basename "$f")"
      count=$((count + 1))
    fi
  done
  echo ""
  echo "已卸载 $count 项 Oinone/Pamirs slash command。"
  exit 0
fi

mkdir -p "$DST"

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

echo "下载 github.com/$REPO@$REF ..."
curl -fsSL "https://codeload.github.com/$REPO/tar.gz/refs/heads/$REF" \
  | tar -xz -C "$tmp" --strip-components=1

src="$tmp/plugins/oinone-pamirs/codex/prompts"
if [ ! -d "$src" ]; then
  echo "错误：tarball 中找不到 $src（仓库结构可能已变更）" >&2
  exit 1
fi

count=0
for f in "$src"/oinone-*.md; do
  [ -e "$f" ] || continue
  cp -f "$f" "$DST/$(basename "$f")"
  echo "  install $(basename "$f")"
  count=$((count + 1))
done

echo ""
echo "已安装 $count 项 Oinone/Pamirs slash command 到 $DST"
echo "在 Codex CLI 中输入 /oinone- 即可看到补全。"
