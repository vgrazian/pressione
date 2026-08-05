#!/usr/bin/env bash
set -euo pipefail

# ═══════════════════════════════════════════════════════════════
# Pressione — Safe Release Deploy
# ═══════════════════════════════════════════════════════════════
# Builds the app and deploys dist/ to the gh-pages branch using
# an isolated git worktree. Never touches the main working directory,
# never uses destructive cross-branch cleanup.
#
# Usage:  bash scripts/deploy.sh [commit-message]

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

COMMIT_MSG="${1:-deploy: v$(node -p "require('./package.json').version")}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m'

WORKTREE_DIR=""
CLEANUP_DONE=false

cleanup() {
    if [ "$CLEANUP_DONE" = true ]; then return; fi
    CLEANUP_DONE=true

    if [ -n "$WORKTREE_DIR" ] && [ -d "$WORKTREE_DIR" ]; then
        echo -e "   🧹 Rimozione worktree temporaneo..."
        git worktree remove "$WORKTREE_DIR" --force 2>/dev/null || rm -rf "$WORKTREE_DIR"
    fi
    # Return to original branch if we're not on it
    git checkout --quiet - 2>/dev/null || true
}
trap cleanup EXIT

echo ""
echo -e "${CYAN}╔══════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Pressione — Safe Release Deploy   ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════╝${NC}"
echo ""

# ── 1. Pre-flight checks ──────────────────────────────────────
echo -e "${CYAN}🔍 [1/5] Verifica prerequisiti...${NC}"

# .env check
if [ ! -f .env ]; then
    echo -e "${RED}❌ ERRORE: .env non trovato!${NC}"
    echo "   Crealo con VITE_SUPABASE_URL e VITE_SUPABASE_PUBLISHABLE_KEY"
    exit 1
fi
if ! grep -q "sb_publishable_" .env 2>/dev/null; then
    echo -e "${RED}❌ ERRORE: .env non contiene una chiave Supabase valida${NC}"
    exit 1
fi

# Ensure .env is gitignored
if [ -f .gitignore ] && ! grep -q '^\.env$' .gitignore 2>/dev/null; then
    echo -e "${YELLOW}   ⚠️  .env non in .gitignore — aggiunto${NC}"
    echo '.env' >> .gitignore
fi

# Build doesn't need clean working tree (Vite reads from disk, not git)
# but warn if there are unstaged changes (they won't be in the deploy)
if ! git diff --quiet 2>/dev/null; then
    echo -e "${YELLOW}   ⚠️  Working tree con modifiche non committate (non incluse nel deploy)${NC}"
fi

echo -e "${GREEN}   ✅ Prerequisiti OK${NC}"

# ── 2. Build ───────────────────────────────────────────────────
echo ""
echo -e "${CYAN}📦 [2/5] Build...${NC}"
npm install --silent 2>&1 | tail -1
npm run build

# ── 3. Verify build artifacts ──────────────────────────────────
echo ""
echo -e "${CYAN}🔬 [3/5] Verifica build...${NC}"

if [ ! -f dist/index.html ]; then
    echo -e "${RED}❌ ERRORE: dist/index.html non trovato — build fallita${NC}"
    exit 1
fi

# Safety: verify the anon key (NOT secret key) is in the bundle
# We only check for the Supabase URL, never for secret keys
if grep -q "pvmlphhzqevmktrknipo" dist/assets/index*.js 2>/dev/null; then
    echo -e "${GREEN}   ✅ Supabase URL presente nel bundle${NC}"
else
    echo -e "${YELLOW}   ⚠️  Supabase URL non trovato — verifica .env${NC}"
fi

VERSION=$(node -p "require('./package.json').version")
BUILD=$(git rev-parse --short HEAD 2>/dev/null || echo "dev")
ASSET_COUNT=$(ls dist/assets/*.js 2>/dev/null | wc -l | tr -d ' ')
echo -e "   📦 v${VERSION} (${BUILD})  ·  ${ASSET_COUNT} asset JS  ·  $(du -sh dist | cut -f1)"

# ── 4. Deploy via isolated worktree ────────────────────────────
echo ""
echo -e "${CYAN}🚀 [4/5] Deploy su gh-pages (worktree isolato)...${NC}"

# Create temp worktree location
WORKTREE_DIR=$(mktemp -d /tmp/pressione-gh-pages.XXXXXX)

# Fetch gh-pages from remote
git fetch origin gh-pages --quiet 2>/dev/null || true

# Always sync local gh-pages with origin to avoid stale worktrees
if git show-ref --verify --quiet refs/remotes/origin/gh-pages; then
    echo "   📂 gh-pages branch da origin"
    if git show-ref --verify --quiet refs/heads/gh-pages; then
        # Local exists — force-sync it with remote to ensure clean state
        git branch -f gh-pages origin/gh-pages --quiet
    fi
    git worktree add "$WORKTREE_DIR" origin/gh-pages --quiet
    git -C "$WORKTREE_DIR" checkout -B gh-pages --quiet
elif git show-ref --verify --quiet refs/heads/gh-pages; then
    echo "   📂 gh-pages branch locale (no remote)"
    git worktree add "$WORKTREE_DIR" gh-pages --quiet
else
    echo "   🆕 gh-pages non esiste — creazione"
    git worktree add "$WORKTREE_DIR" --orphan gh-pages --quiet
fi

# Clean ONLY git-tracked files inside the isolated worktree.
# This is safe because: (a) we're in an isolated worktree,
# (b) git rm only touches files known to git,
# (c) .gitignore rules are respected — untracked files are left alone.
echo "   🧹 Pulizia build precedente..."
git -C "$WORKTREE_DIR" rm -rf --quiet . 2>/dev/null || true

# Copy new build artifacts
echo "   📋 Copia nuovi artefatti..."
cp -R dist/* "$WORKTREE_DIR"/
touch "$WORKTREE_DIR/.nojekyll"

# Stage all changes within the worktree
git -C "$WORKTREE_DIR" add -A

# ── FINAL SAFETY GATES ─────────────────────────────────────────

# Gate 1: .env must NEVER be staged
STAGED_FILES=$(git -C "$WORKTREE_DIR" diff --cached --name-only 2>/dev/null || echo "")
if echo "$STAGED_FILES" | grep -q '^\.env$'; then
    echo -e "${RED}❌ ERRORE CRITICO: .env sta per essere committato!${NC}"
    echo -e "${RED}   Deploy ANNULLATO. Verifica .gitignore.${NC}"
    exit 1
fi

# Gate 2: index.html must exist in the worktree
if [ ! -f "$WORKTREE_DIR/index.html" ]; then
    echo -e "${RED}❌ ERRORE: index.html assente dopo la copia${NC}"
    exit 1
fi

# Commit and push from the isolated worktree
CHANGES=$(git -C "$WORKTREE_DIR" diff --cached --stat 2>/dev/null | tail -1 || echo "")
echo -e "   📝 Commit: ${COMMIT_MSG}"
echo -e "   📊 Changes: ${CHANGES:-nessuna modifica}"

if git -C "$WORKTREE_DIR" diff --cached --quiet 2>/dev/null; then
    echo -e "${YELLOW}   ⚠️  Nessuna modifica — build identica alla precedente, skip commit${NC}"
else
    git -C "$WORKTREE_DIR" commit -m "$COMMIT_MSG" --quiet
fi

echo "   🚀 Push su origin/gh-pages..."
git -C "$WORKTREE_DIR" push origin gh-pages --quiet

# ── 5. Done ────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}╔══════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅ Deploy completato!             ║${NC}"
printf "${GREEN}║   v%s (%s)%*s║${NC}\n" "$VERSION" "$BUILD" $(( 20 - ${#VERSION} - ${#BUILD} )) ""
echo -e "${GREEN}║   https://vgrazian.github.io/pressione/ ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════╝${NC}"
echo ""
echo "Attendi ~1-2 min per propagazione CDN, poi Cmd+Shift+R."
echo ""
