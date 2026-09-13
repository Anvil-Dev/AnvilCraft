"""在提交节点前只读核对本地源分支，列出新提交与已发现但未移植的增量。"""
import json
import subprocess
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[2]
sys.stdout.reconfigure(encoding="utf-8")
tracking = json.loads((root / "docs/porting/source-updates.json").read_text(encoding="utf-8"))
current = subprocess.check_output(["git", "rev-parse", tracking["branch"]], cwd=root, text=True).strip()
observed = tracking["latest_observed"]
ancestor = subprocess.run(["git", "merge-base", "--is-ancestor", observed, current], cwd=root, check=False).returncode == 0
new = subprocess.check_output(["git", "log", "--reverse", "--format=%H%x09%s", f"{observed}..{current}"], cwd=root).decode("utf-8")
changed = subprocess.check_output(["git", "diff", "--name-only", observed, current], cwd=root).decode("utf-8").splitlines()
print(json.dumps({"branch": tracking["branch"], "current": current, "observed": observed, "fast_forward": ancestor,
                  "new_commits": new.splitlines(), "changed_files": changed,
                  "pending": [entry for entry in tracking["updates"] if entry["status"] != "ported"]},
                 ensure_ascii=False, indent=2))
