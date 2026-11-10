"""从发布分支的历史中选择日志起点，并使用 GitHub 原生发布说明。"""

from urllib.parse import quote

from common import github, release_type, summary


def generate(plan, anchor):
    releases = []
    page = 1
    while True:
        batch = github(f"releases?per_page=100&page={page}")
        releases.extend(item for item in batch if not item["draft"] and item.get("published_at"))
        if len(batch) < 100:
            break
        page += 1

    prerelease = release_type(plan["version"]) != "release"
    sha = plan["targets"][anchor]["sha"]
    previous = None
    for item in sorted(releases, key=lambda r: (r["published_at"], r["id"]), reverse=True):
        tag = item["tag_name"]
        if tag == plan["tag"] or (item["prerelease"] and not prerelease):
            continue
        comparison = github(f"compare/{quote(tag, safe='')}...{sha}?per_page=1", missing=True)
        if comparison and comparison["status"] in {"ahead", "identical"}:
            previous = tag
            break

    if previous is None and releases:
        # 省略 previous_tag_name 会让 GitHub 自动选到其他分支或不匹配的类型。
        summary("未找到符合类型的祖先 Release tag，跳过增量发布说明。")
        return {"previous_tag_name": None, "release_notes": ""}

    payload = {"tag_name": plan["tag"], "target_commitish": sha}
    if previous:
        payload["previous_tag_name"] = previous
    notes = github("releases/generate-notes", payload)["body"]
    summary(f"Generate release notes: Previous tag = {previous or '(首次发布)'}")
    return {"previous_tag_name": previous, "release_notes": notes}
