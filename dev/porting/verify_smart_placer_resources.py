"""Check the migrated smart placer models, textures, UI resources and handbook."""

import json
import subprocess
from pathlib import Path

from verify_redstone_resources import ROOT, SOURCE, source


def main():
    paths = subprocess.check_output(
        ["git", "ls-tree", "-r", "--name-only", SOURCE, "src/main/resources/assets/anvilcraft"], cwd=ROOT
    ).decode().splitlines()
    checked = 0
    for path in paths:
        if "smart_block_placer" not in path or "/pulse_generator/" in path or path.endswith("/layer_sigle.png"):
            continue
        target = ROOT / path
        assert target.exists(), f"Missing resource: {path}"
        original = source(path)
        actual = target.read_bytes()
        if path.endswith(".json"):
            assert json.loads(original) == json.loads(actual), path
        elif path.endswith(".md"):
            assert original.replace(b"\r\n", b"\n") == actual.replace(b"\r\n", b"\n"), path
        else:
            assert original == actual, path
        checked += 1
    print(f"PASS: {checked} smart placer resource files match source")


if __name__ == "__main__":
    main()
