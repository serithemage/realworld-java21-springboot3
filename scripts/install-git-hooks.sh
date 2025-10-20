#!/bin/bash
#
# Script to install Git hooks for the project
# Run this script after cloning the repository: ./scripts/install-git-hooks.sh
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
HOOK_DIR="$PROJECT_ROOT/.git/hooks"
PRE_COMMIT_HOOK="$HOOK_DIR/pre-commit"

echo "🔧 Installing Git hooks..."
echo ""

# Check if .git directory exists
if [ ! -d "$PROJECT_ROOT/.git" ]; then
  echo "❌ Error: .git directory not found. Are you in a Git repository?"
  exit 1
fi

# Create hooks directory if it doesn't exist
mkdir -p "$HOOK_DIR"

# Copy pre-commit hook
echo "📋 Installing pre-commit hook..."
cp "$SCRIPT_DIR/pre-commit" "$PRE_COMMIT_HOOK"
chmod +x "$PRE_COMMIT_HOOK"

echo ""
echo "✅ Git hooks installed successfully!"
echo ""
echo "ℹ️  The pre-commit hook will run all unit tests before each commit."
echo "   To bypass the hook in emergency situations, use: git commit --no-verify"
echo ""
