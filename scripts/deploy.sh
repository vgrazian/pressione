#!/usr/bin/env bash
set -euo pipefail

# ═══════════════════════════════════════════════════════════════
# Pressione — Fast Deploy (local clone)
# ═══════════════════════════════════════════════════════════════
# Clones gh-pages locally (--local for speed, hardlinks), cleans
# old build, copies dist/, commits, force-pushes.

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
echo -e "${GREEN}   ✅ OK${NC}"

# ── 2. Build ───────────────────────────────────────────────────
echo ""
echo -e "${CYAN}📦 [2/4] Build...${NC}"
npm run build --silent 2>&1 | tail -3

# ── 3. Verify ──────────────────────────────────────────────────
echo ""
echo -e "${CYAN}🔬 [3/4] Verifica...${NC}"
[ -f dist/index.html ] || { echo -e "${RED}❌ dist/index.html non trovato${NC}"; exit 1; }
VERSION=$(node -p "require('./package.json').version")
BUILD=$(git rev-parse --short HEAD 2>/dev/null || echo "dev")
echo -e "   📦 v${VERSION} (${BUILD})"

# ── 4. Deploy via local clone ──────────────────────────────────
echo ""
echo -e "${CYAN}🚀 [4/4] Deploy (local clone)...${NC}"

TMP_CLONE=$(mktemp -d /tmp/pressione-gh-pages.XXXXXX)
trap "rm -rf '$TMP_CLONE'" EXIT

# Clone gh-pages locally (--local uses hardlinks, nearly instant)
if git show-ref --verify --quiet refs/remotes/origin/gh-pages; then
    git fetch origin gh-pages --quiet 2>/dev/null || true
fi

if git show-ref --verify --quiet refs/heads/gh-pages; then
    # Use existing local branch
    git clone --local --branch gh-pages --single-branch . "$TMP_CLONE" --quiet 2>/dev/null
elif git show-ref --verify --quiet refs/remotes/origin/gh-pages; then
    git clone --local --branch gh-pages --single-branch . "$TMP_CLONE" --quiet 2>/dev/null || {
        git clone --branch gh-pages --single-branch --depth 1 "$(git remote get-url origin)" "$TMP_CLONE" --quiet
    }
else
    mkdir -p "$TMP_CLONE" && git -C "$TMP_CLONE" init --quiet && git -C "$TMP_CLONE" checkout -b gh-pages --quiet
fi

# Clean old build
git -C "$TMP_CLONE" rm -rf --quiet . 2>/dev/null || true

# Copy new build
cp -R dist/* "$TMP_CLONE"/
touch "$TMP_CLONE/.nojekyll"

# Commit + force push (gh-pages is always fully regenerated)
git -C "$TMP_CLONE" add -A
if git -C "$TMP_CLONE" diff --cached --quiet 2>/dev/null; then
    echo -e "${YELLOW}   ⚠️  Build identica — skip${NC}"
else
    git -C "$TMP_CLONE" commit -m "$COMMIT_MSG" --quiet
    git -C "$TMP_CLONE" push origin gh-pages --force --quiet
    echo -e "${GREEN}   ✅ Push completato${NC}"
fi

echo ""
echo -e "${GREEN}╔══════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ Deploy completato!             ║${NC}"
printf "${GREEN}║   v%s (%s)%*s║${NC}\n" "$VERSION" "$BUILD" $(( 20 - ${#VERSION} - ${#BUILD} )) ""
echo -e "${GREEN}║   https://vgrazian.github.io/pressione/ ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════╝${NC}"
echo ""
