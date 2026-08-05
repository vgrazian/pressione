#!/usr/bin/env bash
set -euo pipefail
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
echo -e "${CYAN}🔍 [1/4] Verifica...${NC}"
[ -f .env ] || { echo -e "${RED}❌ .env non trovato${NC}"; exit 1; }
echo -e "${GREEN}   ✅ OK${NC}"
echo ""
echo -e "${CYAN}📦 [2/4] Build...${NC}"
npm run build --silent 2>&1 | tail -3
echo ""
echo -e "${CYAN}🔬 [3/4] Verifica...${NC}"
[ -f dist/index.html ] || { echo -e "${RED}❌ dist/index.html non trovato${NC}"; exit 1; }
VERSION=$(node -p "require('./package.json').version")
BUILD=$(git rev-parse --short HEAD 2>/dev/null || echo "dev")
echo -e "   📦 v${VERSION} (${BUILD})"
echo ""
echo -e "${CYAN}🚀 [4/4] Deploy...${NC}"
TMP_CLONE=$(mktemp -d /tmp/pressione-gh-pages.XXXXXX)
trap "rm -rf '$TMP_CLONE'" EXIT
git clone --branch gh-pages --single-branch --depth 2 https://github.com/vgrazian/pressione.git "$TMP_CLONE" --quiet
git -C "$TMP_CLONE" rm -rf --quiet . 2>/dev/null || true
cp -R dist/* "$TMP_CLONE"/
touch "$TMP_CLONE/.nojekyll"
git -C "$TMP_CLONE" add -A
git -C "$TMP_CLONE" commit -m "$COMMIT_MSG" --quiet
git -C "$TMP_CLONE" push origin gh-pages --force --quiet
echo -e "${GREEN}   ✅ Push completato${NC}"
echo ""
echo -e "${GREEN}╔══════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ Deploy completato!             ║${NC}"
printf "${GREEN}║   v%s (%s)%*s║${NC}\n" "$VERSION" "$BUILD" $(( 20 - ${#VERSION} - ${#BUILD} )) ""
echo -e "${GREEN}║   https://vgrazian.github.io/pressione/ ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════╝${NC}"
echo ""
