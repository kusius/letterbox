#!/usr/bin/env bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Error handler
error_exit() {
  echo -e "${RED}Error: $1${NC}" >&2
  exit 1
}

success() {
  echo -e "${GREEN}✓ $1${NC}"
}

info() {
  echo -e "${YELLOW}→ $1${NC}"
}

# Ensure we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
  error_exit "Not in a git repository"
fi

# Fetch latest changes
info "Fetching latest changes from origin..."
git fetch origin || error_exit "Failed to fetch from origin"
success "Fetched latest changes"

# Get the merge base (last common commit between dev and main)
info "Finding last merge point between dev and main..."
MERGE_BASE=$(git merge-base origin/dev origin/main 2>/dev/null)

if [[ -z "$MERGE_BASE" ]]; then
  error_exit "Could not find merge base between origin/dev and origin/main"
fi

success "Found merge base: ${MERGE_BASE:0:8}"

# Get commits from dev that are not in main
info "Collecting commits from dev since last merge to main..."
COMMITS=$(git log --no-merges --pretty=format:"%H|%s|%an|%ad" --date=short "$MERGE_BASE..origin/dev")

if [[ -z "$COMMITS" ]]; then
  error_exit "No new commits found on dev branch since last merge to main"
fi

# Count commits
COMMIT_COUNT=$(echo "$COMMITS" | wc -l | tr -d ' ')
success "Found $COMMIT_COUNT new commit(s)"

# Generate markdown file
OUTPUT_FILE="PR_NOTES.md"
info "Generating $OUTPUT_FILE..."

cat > "$OUTPUT_FILE" << 'EOF'
# Pull Request: dev → main

## Summary

This PR merges the latest changes from `dev` into `main`.

EOF

# Add statistics
echo "## Changes Overview" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "- **Total commits**: $COMMIT_COUNT" >> "$OUTPUT_FILE"
echo "- **Base commit**: \`${MERGE_BASE:0:8}\`" >> "$OUTPUT_FILE"
echo "- **Date range**: $(git log -1 --format=%ad --date=short "$MERGE_BASE") → $(git log -1 --format=%ad --date=short origin/dev)" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Categorize commits by type (feat, fix, chore, etc.)
echo "## Changes by Type" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Function to get category title
get_category_title() {
  case "$1" in
    feat) echo "### ✨ Features" ;;
    fix) echo "### 🐛 Bug Fixes" ;;
    docs) echo "### 📝 Documentation" ;;
    style) echo "### 💄 Styling" ;;
    refactor) echo "### ♻️ Refactoring" ;;
    perf) echo "### ⚡ Performance" ;;
    test) echo "### ✅ Tests" ;;
    chore) echo "### 🔧 Chores" ;;
    build) echo "### 🏗️ Build" ;;
    ci) echo "### 👷 CI/CD" ;;
    other) echo "### 📦 Other Changes" ;;
  esac
}

# Temporary files for each category
for cat in feat fix docs style refactor perf test chore build ci other; do
  echo -n "" > "/tmp/pr_notes_$cat.txt"
done

# Parse commits and categorize
while IFS='|' read -r hash subject author date; do
  # Extract conventional commit type (if present)
  if [[ "$subject" =~ ^(feat|fix|docs|style|refactor|perf|test|chore|build|ci)(\(.+\))?:\ (.+)$ ]]; then
    type="${BASH_REMATCH[1]}"
    scope="${BASH_REMATCH[2]}"
    message="${BASH_REMATCH[3]}"
    
    # Format with scope if present
    if [[ -n "$scope" ]]; then
      # Remove parentheses from scope
      scope_clean="${scope#(}"
      scope_clean="${scope_clean%)}"
      formatted="- **${scope_clean}**: ${message} (\`${hash:0:8}\`)"
    else
      formatted="- ${message} (\`${hash:0:8}\`)"
    fi
    
    echo "$formatted" >> "/tmp/pr_notes_$type.txt"
  else
    # Non-conventional commit goes to "other"
    echo "- $subject (\`${hash:0:8}\`)" >> "/tmp/pr_notes_other.txt"
  fi
done <<< "$COMMITS"

# Write categorized commits to output file
for cat in feat fix docs style refactor perf test chore build ci other; do
  if [[ -s "/tmp/pr_notes_$cat.txt" ]]; then
    get_category_title "$cat" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    cat "/tmp/pr_notes_$cat.txt" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
  fi
  rm -f "/tmp/pr_notes_$cat.txt"
done

# Add full commit list
echo "---" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "## Full Commit Log" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"

git log --no-merges --pretty=format:"%h - %s (%an, %ad)" --date=short "$MERGE_BASE..origin/dev" >> "$OUTPUT_FILE"

echo "" >> "$OUTPUT_FILE"
echo '```' >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Add footer
echo "---" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "_Generated on $(date '+%Y-%m-%d %H:%M:%S')_" >> "$OUTPUT_FILE"

success "Generated $OUTPUT_FILE"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}PR Notes Generated Successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}File: $OUTPUT_FILE${NC}"
echo -e "${BLUE}Commits: $COMMIT_COUNT${NC}"
echo ""
echo "Next steps:"
echo "  1. Review the generated $OUTPUT_FILE"
echo "  2. Edit if needed to add context or group related changes"
echo "  3. Use the content when creating your PR from dev → main"
echo ""
