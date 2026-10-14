"""Plan a release without modifying GitHub or replacing a published APK."""

import json
import os
from pathlib import Path
import re
import subprocess
from dataclasses import dataclass


VERSION_PATTERN = r"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)"
BUILD_FILE = "app/build.gradle.kts"


@dataclass(frozen=True)
class Version:
    name: str
    code: int

    @property
    def numbers(self):
        return tuple(map(int, self.name.split(".")))

    @property
    def tag(self):
        return f"v{self.name}"


def read_version(source):
    names = re.findall(r'^\s*versionName\s*=\s*"([^"\n]+)"\s*$', source, re.MULTILINE)
    codes = re.findall(r"^\s*versionCode\s*=\s*(\d+)\s*$", source, re.MULTILINE)
    if len(names) != 1 or len(codes) != 1:
        raise ValueError("Expected one literal versionName and versionCode in app/build.gradle.kts")
    if not re.fullmatch(VERSION_PATTERN, names[0]) or int(codes[0]) < 1:
        raise ValueError("Use a stable versionName such as 1.0.2 and a positive versionCode")
    return Version(names[0], int(codes[0]))


def latest_stable_release(releases):
    stable = [
        release for release in releases
        if not release["draft"] and not release["prerelease"]
        and re.fullmatch("v" + VERSION_PATTERN, release["tag_name"])
    ]
    return max(
        stable,
        key=lambda release: tuple(map(int, release["tag_name"][1:].split("."))),
        default=None,
    )


def plan_release(version, releases, previous_version, commit):
    existing = next((release for release in releases if release["tag_name"] == version.tag), None)
    if existing and not existing["draft"]:
        return "skip"
    if previous_version:
        if version.numbers <= previous_version.numbers:
            raise ValueError("versionName must be newer than the latest published stable version")
        if version.code <= previous_version.code:
            raise ValueError("Increase versionCode so Android can update existing installations")
    if existing:
        if existing["target_commitish"] != commit:
            raise ValueError("An unfinished draft exists for another commit; remove that draft or bump the version")
        return "resume"
    return "create"


def command(*args):
    return subprocess.check_output(args, text=True).strip()


def main():
    version = read_version(Path(BUILD_FILE).read_text())
    repository = os.environ["GITHUB_REPOSITORY"]
    commit = os.environ.get("GITHUB_SHA") or command("git", "rev-parse", "HEAD")
    pages = json.loads(command("gh", "api", f"repos/{repository}/releases", "--paginate", "--slurp"))
    releases = [release for page in pages for release in page]
    latest = latest_stable_release(releases)
    previous = read_version(command("git", "show", f"{latest['tag_name']}:{BUILD_FILE}")) if latest else None
    action = plan_release(version, releases, previous, commit)

    if action != "skip":
        tag = subprocess.run(
            ["git", "rev-parse", "--verify", "--quiet", f"refs/tags/{version.tag}^{{commit}}"],
            capture_output=True, text=True, check=False,
        )
        if tag.returncode == 0 and tag.stdout.strip() != commit:
            raise ValueError(f"{version.tag} already points to another commit; choose a new version")
        if tag.returncode not in (0, 1):
            raise RuntimeError(f"Could not inspect {version.tag}: {tag.stderr}")

    metadata = {
        "version_name": version.name,
        "version_code": str(version.code),
        "tag": version.tag,
        "action": action,
        "publish": str(action != "skip").lower(),
    }
    if output_file := os.environ.get("GITHUB_OUTPUT"):
        with open(output_file, "a") as output:
            output.writelines(f"{key}={value}\n" for key, value in metadata.items())
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
