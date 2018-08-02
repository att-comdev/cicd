#!/bin/bash

set -ex

remove_tests_by_pattern() {
  pattern="$1"
  for exclude_dir in $(find ./ -name "$pattern" -type d | grep -Ev "contrib|etc|doc|tempest|meta|compat" | sed 's/.\///') ; do
    if [ -d "$exclude_dir" ]; then
      echo "Deleting tests: '$exclude_dir'"
      rm -rf "$exclude_dir";
      echo "Add 'prune $exclude_dir into MANIFEST.in"
      echo "prune $exclude_dir" >> MANIFEST.in
    fi
  done
}

remove_tests() {
  echo "$1"
  if [ "$PROJECT" != "horizon" ]; then
    pushd "$1"
    remove_tests_by_pattern "tests"
    remove_tests_by_pattern "test"
    popd
  fi
}
remove_tests "/tmp/$PROJECT"
