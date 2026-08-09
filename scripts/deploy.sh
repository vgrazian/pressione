#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"
COMMIT_MSG="${1:-deploy: v$(node -p "require('./package.json').version")}"
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; CYAN='\033[0;36m'; NC='\033[0m'

# Source .env so GITHUB_TOKEN (and other vars) are available
if [ -f .env ]; then set -a; source .env; set +a; fi

OWNER="vgrazian"
REPO="pressione"
PAGES_URL="https://${OWNER}.github.io/${REPO}/"
GITHUB_TOKEN="${GITHUB_TOKEN:-}"

# ── helpers ──────────────────────────────────────────────────────────

# Fetch a JSON key via python3 (available on macOS + most CI runners)
_json_val() {
  local json="$1" key="$2" default="${3:-}"
  echo "$json" | python3 -c "
import json,sys
try:
    d=json.load(sys.stdin)
    parts='${key}'.split('.')
    for p in parts:
        d=d.get(p,{})
    if isinstance(d,(dict,list)): print('${default}')
    else: print(d if d is not None else '${default}')
except: print('${default}')
" 2>/dev/null
}

# GitHub API call (authenticated if GITHUB_TOKEN is set).
# Usage: _gh_api "/endpoint"           → GET
#        _gh_api -X POST "/endpoint"   → POST
_gh_api() {
  local method="GET"
  local endpoint=""
  if [ "$1" = "-X" ]; then
    method="$2"
    endpoint="$3"
  else
    endpoint="$1"
  fi
  local auth_args=()
  [ -n "$GITHUB_TOKEN" ] && auth_args=(-H "Authorization: Bearer $GITHUB_TOKEN")
  curl -s --max-time 15 -X "$method" \
    "${auth_args[@]}" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "https://api.github.com${endpoint}" 2>/dev/null || echo '{}'
}

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║        Pressione — Fast Deploy              ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""

# ── Step 0: GitHub Status pre-flight ─────────────────────────────────
echo -e "${CYAN}🌐 [0/5] GitHub Status...${NC}"

# Query components.json for per-service status (Pages is a component)
COMPONENTS_JSON=$(curl -s --max-time 10 \
  "https://www.githubstatus.com/api/v2/components.json" 2>/dev/null || echo '{}')
PAGES_STATUS=$(echo "$COMPONENTS_JSON" | python3 -c "
import json,sys
try:
    d=json.load(sys.stdin)
    for c in d.get('components',[]):
        if c.get('name','') == 'Pages':
            print(c.get('status','unknown'))
            break
    else:
        print('unknown')
except: print('unknown')
" 2>/dev/null || echo "unknown")

case "$PAGES_STATUS" in
  "operational")
    echo -e "${GREEN}   ✅ GitHub Pages: operational${NC}"
    ;;
  "degraded_performance"|"partial_outage")
    echo -e "${YELLOW}   ⚠️  GitHub Pages: ${PAGES_STATUS} — deployment may be delayed${NC}"
    echo -e "${YELLOW}   See: https://www.githubstatus.com/${NC}"
    ;;
  "major_outage")
    echo -e "${RED}   ❌ GitHub Pages: major outage — deployment will likely fail${NC}"
    echo -e "${RED}   See: https://www.githubstatus.com/${NC}"
    read -p "   Continue anyway? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      exit 1
    fi
    ;;
  *)
    echo -e "${YELLOW}   ⚠️  Could not determine GitHub Pages status (${PAGES_INDICATOR:-none})${NC}"
    ;;
esac
echo ""

# ── Step 1: Local checks ─────────────────────────────────────────────
echo -e "${CYAN}🔍 [1/5] Verifica...${NC}"
[ -f .env ] || { echo -e "${RED}❌ .env non trovato${NC}"; exit 1; }
echo -e "${GREEN}   ✅ OK${NC}"
echo ""

# ── Step 2: Build ────────────────────────────────────────────────────
echo -e "${CYAN}📦 [2/5] Build...${NC}"
npm run build --silent 2>&1 | tail -3
echo ""

# ── Step 3: Verify build output ──────────────────────────────────────
echo -e "${CYAN}🔬 [3/5] Verifica...${NC}"
[ -f dist/index.html ] || { echo -e "${RED}❌ dist/index.html non trovato${NC}"; exit 1; }
VERSION=$(node -p "require('./package.json').version")
BUILD=$(git rev-parse --short HEAD 2>/dev/null || echo "dev")
echo -e "   📦 v${VERSION} (${BUILD})"
echo ""

# ── Step 4: Deploy to gh-pages ───────────────────────────────────────
echo -e "${CYAN}🚀 [4/5] Deploy...${NC}"
TMP_CLONE=$(mktemp -d /tmp/pressione-gh-pages.XXXXXX)
trap "rm -rf '$TMP_CLONE'" EXIT
git clone --branch gh-pages --single-branch --depth 2 \
  "https://github.com/${OWNER}/${REPO}.git" "$TMP_CLONE" --quiet
git -C "$TMP_CLONE" rm -rf --quiet . 2>/dev/null || true
cp -R dist/* "$TMP_CLONE"/
touch "$TMP_CLONE/.nojekyll"
# Ensure no .github directory is deployed (Pages serves from branch, not Actions)
rm -rf "$TMP_CLONE/.github"
git -C "$TMP_CLONE" add -A
git -C "$TMP_CLONE" commit -m "$COMMIT_MSG" --quiet
DEPLOYED_SHA=$(git -C "$TMP_CLONE" rev-parse --short HEAD)
git -C "$TMP_CLONE" push origin gh-pages --force --quiet
echo -e "${GREEN}   ✅ Push completato (${DEPLOYED_SHA})${NC}"
echo ""

# ── Step 5: Verify Pages deployment ──────────────────────────────────
echo -e "${CYAN}🔎 [5/5] Pages Build Verification...${NC}"

if [ -z "$GITHUB_TOKEN" ]; then
  echo -e "${YELLOW}   ⚠️  GITHUB_TOKEN not set — skipping API verification${NC}"
  echo -e "${YELLOW}   Set GITHUB_TOKEN (repo + Pages:read scope) for build checks.${NC}"
  echo -e "${YELLOW}   Falling back to HTTP check of ${PAGES_URL}${NC}"

  HTTP_ATTEMPT=0; HTTP_MAX=30
  while [ "$HTTP_ATTEMPT" -lt "$HTTP_MAX" ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
      "${PAGES_URL}?t=$(date +%s)" 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" = "200" ]; then
      echo -e "${GREEN}   ✅ Live page responding (HTTP ${HTTP_CODE})${NC}"
      break
    fi
    echo -ne "   ⏳ HTTP ${HTTP_CODE} — waiting... (${HTTP_ATTEMPT}/${HTTP_MAX})\r"
    sleep 10
    HTTP_ATTEMPT=$((HTTP_ATTEMPT + 1))
  done
  if [ "$HTTP_ATTEMPT" -ge "$HTTP_MAX" ]; then
    echo ""
    echo -e "${YELLOW}   ⚠️  Page not yet responding after 5 min${NC}"
    echo -e "${YELLOW}   Check: ${PAGES_URL}${NC}"
  fi
else
  # Authenticated: poll Pages build API
  MAX_ATTEMPTS=30      # up to 5 minutes
  ATTEMPT=1
  BUILD_STATUS="unknown"
  TRIED_REBUILD=false

  while [ "$ATTEMPT" -le "$MAX_ATTEMPTS" ]; do
    BUILD_JSON=$(_gh_api "/repos/${OWNER}/${REPO}/pages/builds/latest")
    BUILD_STATUS=$(_json_val "$BUILD_JSON" "status" "unknown")
    BUILD_SHA=$(_json_val "$BUILD_JSON" "commit" "")
    BUILD_SHA="${BUILD_SHA:0:7}"
    BUILD_CREATED=$(_json_val "$BUILD_JSON" "created_at" "")

    # Detect stuck build: building but SHA doesn't match what we just pushed
    if [ "$BUILD_STATUS" = "building" ] && [ -n "$BUILD_SHA" ] && [ "$BUILD_SHA" != "$DEPLOYED_SHA" ]; then
      if [ "$TRIED_REBUILD" = false ] && [ "$ATTEMPT" -ge 5 ]; then
        echo ""
        echo -e "${YELLOW}   ⚠️  Stale build (${BUILD_SHA}) stuck since ${BUILD_CREATED} — requesting rebuild...${NC}"
        REBUILD_RESULT=$(_gh_api -X POST "/repos/${OWNER}/${REPO}/pages/builds")
        REBUILD_STATUS=$(_json_val "$REBUILD_RESULT" "status" "")
        if [ "$REBUILD_STATUS" = "queued" ] || [ "$REBUILD_STATUS" = "building" ]; then
          echo -e "${GREEN}   ✅ Rebuild queued (${REBUILD_STATUS}) — waiting for new build...${NC}"
        else
          REBUILD_ERROR=$(_json_val "$REBUILD_RESULT" "message" "check token scope")
          echo -e "${YELLOW}   ⚠️  Rebuild failed: ${REBUILD_ERROR}${NC}"
          echo -e "${YELLOW}   Token needs Pages:read-write scope to trigger rebuilds.${NC}"
        fi
        TRIED_REBUILD=true
      fi
    fi

    case "$BUILD_STATUS" in
      "built")
        if [ -n "$BUILD_SHA" ] && [ "$BUILD_SHA" = "$DEPLOYED_SHA" ]; then
          echo -e "${GREEN}   ✅ Pages build verified — commit ${BUILD_SHA} is live${NC}"
        elif [ -n "$BUILD_SHA" ]; then
          echo -e "${YELLOW}   ⚠️  Build complete but SHA mismatch${NC}"
          echo -e "${YELLOW}      Pushed: ${DEPLOYED_SHA}  |  Built: ${BUILD_SHA}${NC}"
        else
          echo -e "${GREEN}   ✅ Pages build complete${NC}"
        fi
        break
        ;;
      "building"|"queued")
        echo -ne "   ⏳ Build: ${BUILD_STATUS} | sha: ${BUILD_SHA:-?} (attempt ${ATTEMPT}/${MAX_ATTEMPTS})...\r"
        sleep 10
        ATTEMPT=$((ATTEMPT + 1))
        ;;
      "errored")
        echo ""
        echo -e "${RED}   ❌ Pages build errored!${NC}"
        ERROR_MSG=$(_json_val "$BUILD_JSON" "error.message" "Unknown error")
        echo -e "${RED}      ${ERROR_MSG}${NC}"
        if [ "$TRIED_REBUILD" = false ]; then
          echo -e "${YELLOW}   Attempting rebuild...${NC}"
          _gh_api -X POST "/repos/${OWNER}/${REPO}/pages/builds" > /dev/null 2>&1 || true
          TRIED_REBUILD=true
          ATTEMPT=1
          continue
        fi
        break
        ;;
      *)
        echo -ne "   ⏳ Status: ${BUILD_STATUS} (attempt ${ATTEMPT}/${MAX_ATTEMPTS})...\r"
        sleep 5
        ATTEMPT=$((ATTEMPT + 1))
        ;;
    esac
  done

  if [ "$ATTEMPT" -gt "$MAX_ATTEMPTS" ] && [ "$BUILD_STATUS" != "built" ]; then
    echo ""
    echo -e "${YELLOW}   ⚠️  Timed out waiting for Pages build (last status: ${BUILD_STATUS})${NC}"
    echo -e "${YELLOW}   Check: https://github.com/${OWNER}/${REPO}/settings/pages${NC}"
  fi
fi
echo ""

echo -e "${GREEN}╔══════════════════════════════════════════════╗${NC}"
printf "${GREEN}║   %-41s║${NC}\n" "✅ Deploy completato!"
printf "${GREEN}║   %-41s║${NC}\n" "v${VERSION} (${BUILD})"
printf "${GREEN}║   %-41s║${NC}\n" "${PAGES_URL}"
echo -e "${GREEN}╚══════════════════════════════════════════════╝${NC}"
echo ""
