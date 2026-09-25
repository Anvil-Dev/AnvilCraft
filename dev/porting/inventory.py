"""Generate a reproducible branch inventory; file equality is not gameplay parity."""

import argparse
import collections
import csv
import json
from pathlib import Path
import subprocess


ROOT = Path(__file__).resolve().parents[2]


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT).decode("utf-8")


def tree(ref):
    result = {}
    for entry in git("ls-tree", "-r", ref, "src").splitlines():
        metadata, path = entry.split("\t", 1)
        result[path] = metadata.split()[2]
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", default="eb2dadbc4d7fb3a0476765f6313e1667a758ed66")
    parser.add_argument("--target", default="e4bf6c786eb4b103df8473e11d00c7667e0674b3")
    parser.add_argument("--output", type=Path, default=ROOT / "build/porting/inventory")
    args = parser.parse_args()
    source, target = tree(args.source), tree(args.target)
    names = collections.defaultdict(list)
    source_names = collections.Counter(Path(p).name for p in source if p.endswith(".java"))
    for path in target:
        if path.endswith(".java"):
            names[Path(path).name].append(path)
    rows, matched = [], set()
    for path, blob in sorted(source.items()):
        other = path if path in target else None
        candidates = names[Path(path).name] if path.endswith(".java") else []
        if other is None and len(candidates) == 1 and source_names[Path(path).name] == 1:
            other = candidates[0]
        if other is None:
            status = "source-only"
        else:
            matched.add(other)
            status = "identical" if blob == target[other] else "review"
            if path != other:
                status += "-relocated"
        rows.append((status, path, other or "", blob, target.get(other, "")))
    for path in sorted(target.keys() - matched):
        rows.append(("target-only", "", path, "", target[path]))
    args.output.mkdir(parents=True, exist_ok=True)
    with (args.output / "files.tsv").open("w", encoding="utf-8", newline="") as output:
        writer = csv.writer(output, delimiter="\t", lineterminator="\n")
        writer.writerow(("status", "source", "target", "source_blob", "target_blob"))
        writer.writerows(rows)
    base = git("merge-base", args.source, args.target).strip()
    (args.output / "source-commits.tsv").write_text(
        git("log", "--first-parent", "--reverse", "--format=%H%x09%s", f"{base}..{args.source}"),
        encoding="utf-8",
    )
    summary = {
        "source": git("rev-parse", args.source).strip(),
        "target": git("rev-parse", args.target).strip(),
        "merge_base": base,
        "counts": dict(collections.Counter(row[0] for row in rows)),
        "java_counts": dict(collections.Counter(row[0] for row in rows if (row[1] or row[2]).endswith(".java"))),
        "note": "Review is required for API changes, renamed assets and feature parity. Identical files alone do not prove parity.",
    }
    (args.output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
