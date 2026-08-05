#!/usr/bin/env bash
set -euo pipefail

# ═══════════════════════════════════════════════════════════════
# Pressione — Fast Deploy (git plumbing)
# ═══════════════════════════════════════════════════════════════
# Uses git write-tree + commit-tree + push-ref to deploy dist/
# directly to gh-pages. No branch switching, no worktrees,
# no clones — sub-second git operations only.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

COMMIT_MSG="${1:-deploy: v$(node -p "require('./package.json').version")}"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; CYAN='\033[0;36m'; NC='\033[0m'

echo ""
echo -e "${CYAN}╔══════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Pressione — Fast Deploy           ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════╝${NC}"
echo ""

# ── 1. Pre-flight ─────────────────────────────────────────────
echo -e "${CYAN}🔍 [1/4] Verifica...${NC}"
[ -f .env ] || { echo -e "${RED}❌ .env non trovato${NC}"; exit 1; }
grep -q "sb_publishable_" .env 2>/dev/null || { echo -e "${RED}❌ Chiave Supabase non valida${NC}"; exit 1; }
[ -f .gitignore ] && ! grep -q '^\.env$' .gitignore 2>/dev/null && echo '.env' >> .gitignore
echo -e "${GREEN}   ✅ OK${NC}"

# ── 2. Build ───────────────────────────────────────────────────
echo ""
echo -e "${CYAN}📦 [2/4] Build...${NC}"
npm install --silent 2>&1 | tail -1
npm run build

# ── 3. Verify ──────────────────────────────────────────────────
echo ""
echo -e "${CYAN}🔬 [3/4] Verifica...${NC}"
[ -f dist/index.html ] || { echo -e "${RED}❌ dist/index.html non trovato${NC}"; exit 1; }

VERSION=$(node -p "require('./package.json').version")
BUILD=$(git rev-parse --short HEAD 2>/dev/null || echo "dev")
echo -e "   📦 v${VERSION} (${BUILD})"

# ── 4. Deploy via git plumbing ─────────────────────────────────
echo ""
echo -e "${CYAN}🚀 [4/4] Deploy (git plumbing)...${NC}"

# Fetch current gh-pages HEAD (for parent)
git fetch origin gh-pages --quiet 2>/dev/null || true
PARENT=$(git rev-parse origin/gh-pages 2>/dev/null || echo "")

# Build tree object from dist/ using a temporary index.
# This never touches the main index or working tree.
TMP_INDEX=$(mktemp /tmp/pressione-deploy-index.XXXXXX)
export GIT_INDEX_FILE="$TMP_INDEX"

# Stage all dist/ files into the temp index
(cd dist && git add -A) 2>/dev/null

# Write tree object from the temp index
TREE=$(git write-tree 2>/dev/null)
rm -f "$TMP_INDEX"
unset GIT_INDEX_FILE

if [ -z "$TREE" ]; then
    echo -e "${RED}❌ git write-tree fallito${NC}"
    exit 1
fi

# Safety: verify .env wasn't included in the tree
if git ls-tree -r "$TREE" | grep -q '\.env'; then
    echo -e "${RED}❌ CRITICO: .env presente nel tree! Deploy annullato.${NC}"
    exit 1
fi

# Create commit
if [ -n "$PARENT" ] && [ "$PARENT" != "" ]; then
    COMMIT=$(echo "$COMMIT_MSG" | git commit-tree "$TREE" -p "$PARENT")
else
    COMMIT=$(echo "$COMMIT_MSG" | git commit-tree "$TREE")
fi

echo -e "   📝 Commit: ${COMMIT:0:7}"
echo "   🚀 Push su origin/gh-pages..."

# Push directly: commit → refs/heads/gh-pages
git push origin "$COMMIT:refs/heads/gh-pages" --quiet

echo ""
echo -e "${GREEN}╔══════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ Deploy completato!             ║${NC}"
printf "${GREEN}║   v%s (%s)%*s║${NC}\n" "$VERSION" "$BUILD" $(( 20 - ${#VERSION} - ${#BUILD} )) ""
echo -e "${GREEN}║   https://vgrazian.github.io/pressione/ ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════╝${NC}"
echo ""
echo "Attendi ~1-2 min per propagazione CDN, poi Cmd+Shift+R."
