#!/usr/bin/env python3

import os
import re
import subprocess
import sys
import argparse


def run_cmd(cmd):
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error running command: {cmd}\n{result.stderr}")
        sys.exit(1)
    return result.stdout.strip()


def main():
    parser = argparse.ArgumentParser(
        description="Bump the patch version in app/build.gradle and commit."
    )
    parser.add_argument(
        "version",
        nargs="?",
        help="Optional: Specific version tag to use (e.g., v1.6.8). "
        "If omitted, increments patch version.",
    )
    args = parser.parse_args()

    gradle_file = "app/build.gradle"

    if not os.path.exists(gradle_file):
        print(f"Error: {gradle_file} not found. Please run this from the project root.")
        sys.exit(1)

    # Get current short commit hash (7 characters)
    commit_hash = run_cmd("git rev-parse --short=7 HEAD")

    # Read build.gradle
    with open(gradle_file, "r") as f:
        content = f.read()

    # Find current version pattern
    pattern = r"versionName '([a-f0-9]+)-python-audio-autotest-(v\d+\.\d+\.\d+)'"
    match = re.search(pattern, content)

    if not match:
        print("Error: Could not find expected versionName pattern in app/build.gradle")
        sys.exit(1)

    old_hash, old_version_tag = match.groups()

    if args.version:
        new_version_tag = args.version
        if not new_version_tag.startswith("v"):
            new_version_tag = "v" + new_version_tag
    else:
        # Increment patch version automatically
        major, minor, patch = old_version_tag[1:].split(".")
        new_patch = int(patch) + 1
        new_version_tag = f"v{major}.{minor}.{new_patch}"

    new_version_string = f"{commit_hash}-python-audio-autotest-{new_version_tag}"
    old_version_string = f"{old_hash}-python-audio-autotest-{old_version_tag}"

    if new_version_string == old_version_string:
        print("Version is already up to date.")
        return

    print(f"Current version : {old_version_string}")
    print(f"New version     : {new_version_string}")

    # Replace version in file
    old_line = f"versionName '{old_version_string}'"
    new_line = f"versionName '{new_version_string}'"
    new_content = content.replace(old_line, new_line)

    with open(gradle_file, "w") as f:
        f.write(new_content)

    print(f"Updated {gradle_file}")

    # Check for release notes
    release_notes_file = f"release_notes/{new_version_tag}.md"
    desc = ""
    if os.path.exists(release_notes_file):
        with open(release_notes_file, "r") as rn:
            desc = rn.read().strip()

    # Git add and commit
    run_cmd(f"git add {gradle_file}")
    if os.path.exists(release_notes_file):
        run_cmd(f"git add {release_notes_file}")

    commit_msg = f"audioworker: update {new_version_string}"
    if desc:
        commit_msg += f"\n\n{desc}"

    with open(".commit_msg.tmp", "w") as f:
        f.write(commit_msg)

    run_cmd("git commit -F .commit_msg.tmp")
    os.remove(".commit_msg.tmp")

    print(f"Committed version bump to {new_version_string}")


if __name__ == "__main__":
    main()
