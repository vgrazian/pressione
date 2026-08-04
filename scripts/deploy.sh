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

# Safety: ensure .env is in .gitignore (prevents tracking)
if [ -f .gitignore ] && ! grep -q '^\.env$' .gitignore 2>/dev/null; then
    echo "⚠️  .env NON è in .gitignore! Aggiungilo prima di deployare."
    echo '.env' >> .gitignore
    echo "   ✅ .env aggiunto a .gitignore"
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
# Ensure temp dir is always cleaned up, even on error
trap "rm -rf '$TMPDIR' .temp .tmp 2>/dev/null; git checkout main --quiet 2>/dev/null" EXIT
cp -r dist/* "$TMPDIR"/

# Stash local changes before switching branch
STASHED=false
if ! git diff --quiet || ! git diff --cached --quiet; then
    git stash push -m "deploy-script-auto-stash" --quiet
    STASHED=true
fi

git checkout gh-pages --quiet 2>/dev/null || git checkout -b gh-pages origin/gh-pages --quiet

# Remove ONLY tracked files — list them explicitly and remove one by one.
# This is safe because it only touches files known to git, never gitignored files.
echo "   🧹 Pulizia file tracciati su gh-pages..."
git ls-files -z | xargs -0 git rm --quiet -- 2>/dev/null || true

# Copy new build
cp -r "$TMPDIR"/* .
touch .nojekyll

# Restore .gitignore from main (it was tracked, so git rm removed it from index)
git show main:.gitignore > .gitignore 2>/dev/null || true

# Safety: if .gitignore is missing, create a minimal one that at least protects .env
if [ ! -f .gitignore ]; then
    echo "⚠️  .gitignore assente, creo protezione minima per .env"
    printf '.env\n.env.local\nnode_modules/\n' > .gitignore
fi

git add -A

# FINAL SAFETY: verify .env was NOT staged (would mean .gitignore failed)
if git diff --cached --name-only | grep -q '^.env$'; then
    echo "❌ ERRORE CRITICO: .env sta per essere committato!"
    echo "   Qualcosa non va con .gitignore. Deploy ANNULLATO."
    git reset --quiet HEAD .env 2>/dev/null || true
    exit 1
fi
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

# Clean up any accidental .temp or .tmp directories
rm -rf .temp .tmp 2>/dev/null || true

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
