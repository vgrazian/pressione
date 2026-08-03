#!/usr/bin/env bash
set -euo pipefail

# ─── Pressione Release Script ───────────────────────────────────
# Builds the app and deploys dist/ to the gh-pages branch.
# Run from the project root.
#
# Usage:  bash scripts/deploy.sh [commit-message]

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

COMMIT_MSG="${1:-deploy: $(node -p "require('./package.json').version")}"

echo ""
echo "╔══════════════════════════════════════╗"
echo "║   Pressione — Release Deploy        ║"
echo "╚══════════════════════════════════════╝"
echo ""

# ── 1. Check .env ──────────────────────────────────────────────
echo "🔍 [1/5] Verifica .env..."
if [ ! -f .env ]; then
    echo "❌ ERRORE: .env non trovato! Creane uno con:"
    echo "   VITE_SUPABASE_URL=https://xxx.supabase.co"
    echo "   VITE_SUPABASE_PUBLISHABLE_KEY=sb_publishable_..."
    exit 1
fi

if ! grep -q "sb_publishable_" .env 2>/dev/null; then
    echo "❌ ERRORE: .env non contiene una chiave Supabase valida"
    exit 1
fi
echo "   ✅ .env presente con credenziali Supabase"

# ── 2. Install & Build ─────────────────────────────────────────
echo ""
echo "📦 [2/5] Installazione dipendenze + build..."
npm install --silent 2>&1 | tail -1
npm run build

# ── 3. Verify Supabase baked in ────────────────────────────────
echo ""
echo "🔬 [3/5] Verifica build..."
if grep -q "pvmlphhzqevmktrknipo" dist/assets/index*.js 2>/dev/null; then
    echo "   ✅ Supabase URL presente nel bundle"
else
    echo "   ⚠️  Supabase URL NON trovato nel bundle — verifica .env"
fi

VERSION=$(node -p "require('./package.json').version")
BUILD=$(git rev-parse --short HEAD 2>/dev/null || echo "dev")
echo "   📦 v${VERSION} (build ${BUILD})"
echo "   📁 $(ls dist/assets/*.js 2>/dev/null | wc -l | tr -d ' ') asset JS"

# ── 4. Deploy to gh-pages ──────────────────────────────────────
echo ""
echo "🚀 [4/5] Deploy su gh-pages..."

TMPDIR=$(mktemp -d /tmp/pressione-gh-pages.XXXXXX)
cp -r dist/* "$TMPDIR"/

# Stash local changes before switching branch
STASHED=false
if ! git diff --quiet || ! git diff --cached --quiet; then
    git stash push -m "deploy-script-auto-stash" --quiet
    STASHED=true
fi

git checkout gh-pages --quiet 2>/dev/null || git checkout -b gh-pages origin/gh-pages --quiet

# Clean everything except .git and node_modules
find . -maxdepth 1 -not -name '.git' -not -name '.' -not -name '..' -not -name 'node_modules' -exec rm -rf {} \; 2>/dev/null || true

# Copy new build
cp -r "$TMPDIR"/* .
touch .nojekyll

git add -A
git commit -m "$COMMIT_MSG" --quiet
git push origin gh-pages --quiet

# ── 5. Cleanup ─────────────────────────────────────────────────
echo ""
echo "🧹 [5/5] Pulizia..."

git checkout main --quiet
if [ "$STASHED" = true ]; then
    git stash pop --quiet 2>/dev/null || true
fi
rm -rf "$TMPDIR"

# ── Done ───────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════╗"
echo "║   ✅ Deploy completato!             ║"
echo "║   v${VERSION} (${BUILD})                  ║"
echo "║   https://vgrazian.github.io/pressione/ ║"
echo "╚══════════════════════════════════════╝"
echo ""
echo "Attendi ~1-2 min per propagazione CDN, poi hard refresh (Cmd+Shift+R)."
echo ""
