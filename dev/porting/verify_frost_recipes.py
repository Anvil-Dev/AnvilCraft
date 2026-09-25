"""Compare Frost recipe content with the local 1.21 source branch."""

import json
from pathlib import Path
import subprocess


ROOT = Path(__file__).resolve().parents[2]
SOURCE = "dev/1.21/1.6"


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT)


def normalize(value):
    if isinstance(value, list):
        return [normalize(entry) for entry in value]
    if isinstance(value, dict):
        # RecipeResult uses ItemStackTemplate in 26.1 instead of a bare Item id.
        if set(value) == {"id"}:
            return value["id"]
        return {key: normalize(entry) for key, entry in value.items()}
    return value


def verify():
    checked = 0
    for folder in ("deformation", "permutation"):
        directory = f"src/generated/resources/data/anvilcraft/recipe/{folder}"
        paths = git("ls-tree", "-r", "--name-only", SOURCE, directory).decode().splitlines()
        actual = {path.relative_to(ROOT).as_posix() for path in (ROOT / directory).glob("*.json")}
        assert set(paths) == actual, (folder, set(paths) ^ actual)
        for name in paths:
            expected = normalize(json.loads(git("show", f"{SOURCE}:{name}")))
            current = normalize(json.loads((ROOT / name).read_bytes()))
            assert current == expected, name
            checked += 1
    directory = "src/main/resources/data/anvilcraft/recipe/twilight_forest"
    for name in git("ls-tree", "-r", "--name-only", SOURCE, directory).decode().splitlines():
        source = json.loads(git("show", f"{SOURCE}:{name}"))
        if source.get("type") not in ("anvilcraft:deformation", "anvilcraft:permutation"):
            continue
        assert normalize(json.loads((ROOT / name).read_bytes())) == normalize(source), name
        checked += 1
    print(f"Verified {checked} Frost recipes against {SOURCE} ({git('rev-parse', '--short', SOURCE).decode().strip()})")


if __name__ == "__main__":
    verify()
