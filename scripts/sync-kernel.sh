#!/usr/bin/env sh
#
# This file is part of YumeBox.
#
# YumeBox is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program. If not, see <https://www.gnu.org/licenses/>.
#
# Copyright (c)  YumeYucca 2025 - Present
#
#

set -eu

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KERNEL_PROPERTIES="$PROJECT_ROOT/kernel.properties"
GOLANG_ROOT="$PROJECT_ROOT/lib/mihomo"
GOLANG_MAIN="$PROJECT_ROOT/lib/native/go"
MIHOMO_DIR="$GOLANG_ROOT/mihomo"

usage() {
  echo "Usage: $(basename "$0") <alpha|meta|smart>"
  exit 1
}

CHOICE="${1:-}"
case "$CHOICE" in
  alpha|Alpha)
    REPO_URL="https://github.com/MetaCubeX/mihomo.git"
    BRANCH_NAME="Alpha"
    VERSION_SUFFIX=""
    ;;
  meta|Meta)
    REPO_URL="https://github.com/MetaCubeX/mihomo.git"
    BRANCH_NAME="Meta"
    VERSION_SUFFIX=""
    ;;
  smart|Smart)
    REPO_URL="https://github.com/vernesong/mihomo.git"
    BRANCH_NAME="Alpha"
    VERSION_SUFFIX="-Smart"
    ;;
  *)
    usage
    ;;
esac

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd git
require_cmd go

update_kernel_properties() {
  tmp_file="$KERNEL_PROPERTIES.tmp.$$"
  : > "$tmp_file"
  seen_repo=0
  seen_branch=0
  seen_suffix=0

  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      external.mihomo.repo=*)
        echo "external.mihomo.repo=$REPO_URL" >> "$tmp_file"
        seen_repo=1
        ;;
      external.mihomo.branch=*)
        echo "external.mihomo.branch=$BRANCH_NAME" >> "$tmp_file"
        seen_branch=1
        ;;
      external.mihomo.suffix=*)
        echo "external.mihomo.suffix=$VERSION_SUFFIX" >> "$tmp_file"
        seen_suffix=1
        ;;
      *)
        echo "$line" >> "$tmp_file"
        ;;
    esac
  done < "$KERNEL_PROPERTIES"

  if [ "$seen_repo" -eq 0 ]; then
    echo "external.mihomo.repo=$REPO_URL" >> "$tmp_file"
  fi
  if [ "$seen_branch" -eq 0 ]; then
    echo "external.mihomo.branch=$BRANCH_NAME" >> "$tmp_file"
  fi
  if [ "$seen_suffix" -eq 0 ]; then
    echo "external.mihomo.suffix=$VERSION_SUFFIX" >> "$tmp_file"
  fi

  mv "$tmp_file" "$KERNEL_PROPERTIES"
  echo "Updated kernel.properties -> repo=$REPO_URL branch=$BRANCH_NAME suffix=$VERSION_SUFFIX"
}

sync_repo() {
  if [ -d "$MIHOMO_DIR" ]; then
    echo "Removing existing directory $MIHOMO_DIR"
    rm -rf "$MIHOMO_DIR"
  fi
  echo "Cloning $REPO_URL (branch $BRANCH_NAME) -> $MIHOMO_DIR"
  git clone --branch "$BRANCH_NAME" --single-branch "$REPO_URL" "$MIHOMO_DIR"
}

run_tidy() {
  if [ ! -f "$1/go.mod" ]; then
    echo "Skipping tidy for $1 (no go.mod found)"
    return
  fi
  echo "Running go mod tidy in $1"
  (
    cd "$1"
    go mod tidy
  )
}

update_kernel_properties
sync_repo
run_tidy "$GOLANG_ROOT"
run_tidy "$GOLANG_MAIN"

echo "Done: selected $CHOICE"
