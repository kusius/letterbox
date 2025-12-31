#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

DEBUG=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    -d|--debug)
      DEBUG=true
      shift
      ;;
    android|ios|desktop)
      PLATFORM="$1"
      shift
      ;;
    *)
      error_exit "Invalid argument: $1. Usage: ./release.sh [-d] [android|ios|desktop]"
      ;;
  esac
done

# Validate platform argument
if [[ -z "$PLATFORM" ]]; then
  error_exit "Platform argument required. Usage: ./release.sh [-d] [android|ios|desktop]"
fi

case "$PLATFORM" in
  android|ios|desktop)
    ;;
  *)
    error_exit "Invalid platform: $PLATFORM. Must be: android, ios, or desktop"
    ;;
esac

info "Starting release for platform: $PLATFORM"

# Check working tree is clean (skip in debug mode)
if [[ "$DEBUG" == true ]]; then
  info "Debug mode enabled — skipping working tree cleanliness checks"
else
  if ! git diff-index --quiet HEAD --; then
    error_exit "Working tree has staged changes. Please commit or discard them."
  fi

  if ! git diff-files --quiet; then
    error_exit "Working tree has unstaged changes. Please commit or discard them."
  fi

  success "Working tree is clean"
fi

# Check out dev branch
info "Checking out dev branch..."
git checkout dev || error_exit "Failed to checkout dev branch"
success "On dev branch"

# Verify dev is in sync with origin
info "Verifying dev is in sync with origin..."
git fetch origin || error_exit "Failed to fetch from origin"

LOCAL=$(git rev-parse dev)
REMOTE=$(git rev-parse origin/dev)

if [[ "$LOCAL" != "$REMOTE" ]]; then
  error_exit "Main branch is out of sync with origin. Please pull/push changes."
fi

success "Main branch is in sync with origin"

# Read current version from libs.versions.toml
VERSION_KEY="app-version-$PLATFORM"
CURRENT_VERSION=$(grep "^[[:space:]]*$VERSION_KEY" gradle/libs.versions.toml | sed 's/.*= "\([^"]*\)".*/\1/')

if [[ -z "$CURRENT_VERSION" ]]; then
  error_exit "Could not find $VERSION_KEY in gradle/libs.versions.toml"
fi

success "Current version: $CURRENT_VERSION"

# Parse version
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

if [[ -z "$MAJOR" ]] || [[ -z "$MINOR" ]] || [[ -z "$PATCH" ]]; then
  error_exit "Invalid version format in libs.versions.toml: $CURRENT_VERSION (expected X.Y.Z)"
fi

# Ask user for update type
echo ""
echo "Current version: $CURRENT_VERSION"
echo "Select update type:"
echo "  1) Major (increment $MAJOR, reset minor and patch)"
echo "  2) Minor (increment $MINOR, reset patch)"
echo "  3) Patch (increment $PATCH)"
echo ""
read -p "Enter choice (1-3): " CHOICE

case "$CHOICE" in
  1)
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
    ;;
  2)
    MINOR=$((MINOR + 1))
    PATCH=0
    ;;
  3)
    PATCH=$((PATCH + 1))
    ;;
  *)
    error_exit "Invalid choice: $CHOICE"
    ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
NEW_VERSION_CODE=$((MAJOR * 10000 + MINOR * 100 + PATCH))
success "New version: $NEW_VERSION ($NEW_VERSION_CODE)"

# Update libs.versions.toml
info "Updating gradle/libs.versions.toml..."
sed -i.bak "s/^\([[:space:]]*\)$VERSION_KEY = \"[^\"]*\"/\1$VERSION_KEY = \"$NEW_VERSION\"/" gradle/libs.versions.toml

if ! grep -q "$VERSION_KEY = \"$NEW_VERSION\"" gradle/libs.versions.toml; then
  error_exit "Failed to update version in gradle/libs.versions.toml"
fi

rm -f gradle/libs.versions.toml.bak
success "Updated gradle/libs.versions.toml"

if [[ "$PLATFORM" == "ios" ]]; then
  info "Updating iOS Info.plist version..."
  
  # Path to your Info.plist
  IOS_PLIST_PATH="iosApp/iosApp/Info.plist"
  
  # Update CFBundleShortVersionString (semantic version)
  /usr/libexec/PlistBuddy -c "Set :CFBundleShortVersionString $NEW_VERSION" "$IOS_PLIST_PATH" || error_exit "Failed to update CFBundleShortVersionString"

  # Update CFBundleVersion (build number)
  # For simplicity, we increment patch as build number
  /usr/libexec/PlistBuddy -c "Set :CFBundleVersion $NEW_VERSION_CODE" "$IOS_PLIST_PATH" || error_exit "Failed to update CFBundleVersion"

  success "Updated iOS Info.plist"
fi

# Commit changes
COMMIT_MSG="Release version v$NEW_VERSION for platform $PLATFORM"
info "Committing with message: $COMMIT_MSG"

git add gradle/libs.versions.toml || error_exit "Failed to stage gradle/libs.versions.toml"


if [[ "$PLATFORM" == "ios" ]]; then
  git add "$IOS_PLIST_PATH" || error_exit "Failed to stage Info.plist"
fi

if [[ "$DEBUG" == false ]]; then
  git commit -m "$COMMIT_MSG" || error_exit "Failed to commit"
fi
success "Committed"

# Push to origin
info "Pushing dev to origin..."

if [[ "$DEBUG" == false ]]; then
   git push origin dev || error_exit "Failed to push dev to origin"
fi
success "Pushed to origin"

# Create and push tag
TAG="v$NEW_VERSION-$PLATFORM"
info "Creating tag: $TAG"

if [[ "$DEBUG" == false ]]; then
   git tag "$TAG" || error_exit "Failed to create tag"
fi

success "Tag created"

info "Pushing tag to origin..."

if [[ "$DEBUG" == false ]]; then
   git push origin "$TAG" || error_exit "Failed to push tag to origin"
fi

success "Tag pushed to origin"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Release complete!${NC}"
echo -e "${GREEN}Platform: $PLATFORM${NC}"
echo -e "${GREEN}Version: v$NEW_VERSION${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "The GitHub Actions workflow should trigger automatically."
echo "Monitor progress at: https://github.com/your-username/letterbox/actions"
