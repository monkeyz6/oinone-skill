#!/usr/bin/env bash
#
# Regenerate Codex CLI prompt files from the canonical SKILL.md sources.
#
# 对 plugins/oinone-pamirs/skills/<name>/SKILL.md 重新派生
# plugins/oinone-pamirs/codex/prompts/<name>.md，并在末尾附上
# 同目录辅助资源（reference.md / examples.md / references/ / assets/）
# 的相对路径列表，供模型按需用 Read 工具加载。
#
# 用法：
#   ./scripts/regen-codex-prompts.sh
#

set -euo pipefail

REPO_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
SKILLS_DIR="$REPO_ROOT/plugins/oinone-pamirs/skills"
PROMPTS_DIR="$REPO_ROOT/plugins/oinone-pamirs/codex/prompts"

if [ ! -d "$SKILLS_DIR" ]; then
  echo "错误：找不到 $SKILLS_DIR" >&2
  exit 1
fi

mkdir -p "$PROMPTS_DIR"

count=0
for d in "$SKILLS_DIR"/*/; do
  name=$(basename "$d")
  src="$d/SKILL.md"
  dst="$PROMPTS_DIR/$name.md"
  [ -f "$src" ] || continue

  awk '
    BEGIN { fm = 0 }
    NR == 1 && $0 == "---" { fm = 1; next }
    fm == 1 && $0 == "---" { fm = 2; next }
    fm == 1 { next }
    { print }
  ' "$src" > "$dst"

  sibs=()
  for entry in "$d"*; do
    [ -e "$entry" ] || continue
    base=$(basename "$entry")
    case "$base" in
      SKILL.md|evals) continue ;;
    esac
    sibs+=("$base")
  done

  if [ ${#sibs[@]} -gt 0 ]; then
    {
      echo ""
      echo "---"
      echo ""
      echo "## 补充参考资料"
      echo ""
      echo "如需更详细的 API / 示例 / 模板资源，请使用 Read 工具读取以下文件（路径相对于 plugin 仓库根目录）："
      echo ""
      for base in "${sibs[@]}"; do
        if [ -d "$d/$base" ]; then
          echo "- 目录: \`plugins/oinone-pamirs/skills/$name/$base/\`"
        else
          echo "- 文件: \`plugins/oinone-pamirs/skills/$name/$base\`"
        fi
      done
      echo ""
      echo "> 在 Codex CLI 中，请在本 marketplace 仓库根目录启动 codex，相对路径方可解析；或将仓库路径作为前缀传入 Read 工具。"
    } >> "$dst"
  fi

  echo "  regen  codex/prompts/$name.md"
  count=$((count + 1))
done

echo ""
echo "已重新生成 $count 个 Codex prompt 文件。"
