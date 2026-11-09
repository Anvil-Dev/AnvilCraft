"""分发原版构件，分别保留 CurseForge 主文件和源码附件的回执。"""

import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from common import (commit_status, digest, gh, github, record, release_type, repo, require, summary,
                    tag_manifest)
from maven import request as maven_request

MODRINTH = "https://api.modrinth.com/v2"
CURSEFORGE = "https://minecraft.curseforge.com/api"
AGENT = "Anvil-Dev/AnvilCraft release (github.com/Anvil-Dev/AnvilCraft)"


def files_for(plan, line, directory):
    target = plan["targets"][line]
    artifact_name = "build-" + line
    listing = github(f"actions/runs/{plan['run']}/artifacts?name={artifact_name}", missing=True)
    artifacts = listing["artifacts"] if listing else []
    if artifacts and not artifacts[0]["expired"]:
        gh("run", "download", plan["run"], "-n", artifact_name, "-D", str(directory))
        folder = directory / "dist"
    else:
        # Actions 构件过期后，使用 GitHub 主包和内部 Maven 的原版源码包。
        folder = directory
        gh("release", "download", plan["tag"], "-p", target["jar"], "-D", str(folder))
        prefix = f"{target['maven_group'].replace('.', '/')}/{target['maven_artifact']}/{plan['version']}"
        name = target["jar"][:-4] + "-sources.jar"
        data = maven_request(prefix + "/" + name)
        require(data is not None, f"Missing original Maven attachment: {name}")
        (folder / name).write_bytes(data)
    for name, checksum in target["files"].items():
        require(digest(folder / name) == checksum, f"Checksum mismatch: {name}")
    return folder


class Rejected(RuntimeError):
    """远端已明确拒绝本次上传。"""


def multipart(url, auth, fields):
    args = ["curl", "--silent", "--show-error", "--max-time", "240", "--user-agent", AGENT,
            "--header", "@-", "--write-out", "\n%{http_code}"]
    for name, value in fields.items():
        if isinstance(value, Path):
            args.extend(["--form", f"{name}=@{value}"])
        else:
            args.extend(["--form-string", f"{name}={json.dumps(value)}"])
    result = subprocess.run([*args, url], input=auth + "\n", text=True, capture_output=True, check=True)
    body, code = result.stdout.rsplit("\n", 1)
    if int(code) in {400, 401, 403, 404, 413, 415, 422, 429}:
        raise Rejected(f"HTTP {code}: {body[:500]}")
    require(200 <= int(code) < 300, f"Upload result uncertain (HTTP {code}): {body[:500]}")
    return json.loads(body)


def send(sha, context, upload, reconcile=None):
    prior = commit_status(sha, context)
    if prior and prior["state"] == "success":
        return prior["description"]
    receipt = reconcile() if reconcile else None
    if not receipt:
        require(not prior, f"Upload already attempted; no retry for {context}: {prior}")
        record(sha, context, "pending", "upload in progress")
        try:
            receipt = str(upload())
        except Rejected as error:
            record(sha, context, "failure", f"upload rejected: {str(error)[:100]}")
            raise
    record(sha, context, "success", receipt)
    return receipt


def publish_modrinth(plan, line, folder, context):
    t = plan["targets"][line]
    kind = release_type(plan["version"])
    token = os.environ["MODRINTH_TOKEN"]
    require(token, "Missing MODRINTH_TOKEN")
    files = {name: folder / name for name in t["files"]}
    hashes = {name: digest(path, "sha512") for name, path in files.items()}

    def reconcile():
        request = Request(f"{MODRINTH}/version_file/{hashes[t['jar']]}?algorithm=sha512",
                          headers={"Authorization": token, "User-Agent": AGENT})
        try:
            with urlopen(request, timeout=45) as response:
                remote = json.load(response)
        except HTTPError as error:
            if error.code == 404:
                return None
            raise
        require(remote["project_id"] == t["modrinth"] and remote["version_number"] == plan["version"]
                and remote["version_type"] == kind and t["game"] in remote["game_versions"]
                and "neoforge" in remote["loaders"]
                and {f["filename"]: f["hashes"]["sha512"] for f in remote["files"]} == hashes,
                "Modrinth has the JAR with different release metadata")
        return remote["id"]

    metadata = {"project_id": t["modrinth"], "version_number": plan["version"],
                "name": f"AnvilCraft {plan['version']} · NeoForge {t['game']}", "version_type": kind,
                "changelog": changelog(plan),
                "dependencies": [{"project_id": "nKAcAL9b", "dependency_type": "embedded"}],
                "game_versions": [t["game"]],
                "loaders": ["neoforge"], "featured": True, "primary_file": t["jar"], "file_parts": list(files),
                "file_types": {n: "sources-jar"
                               for n in files if n != t["jar"]}}
    return send(t["sha"], context, lambda: multipart(MODRINTH + "/version", "Authorization: " + token,
                {"data": metadata, **files})["id"], reconcile)


def changelog(plan):
    url = f"https://github.com/{repo()}/releases/tag/{plan['tag']}"
    return plan.get("release_notes", "") + "\n\n" + url


def publish_curseforge(plan, line, folder, context):
    t = plan["targets"][line]
    token = os.environ["CURSEFORGE_TOKEN"]
    require(token, "Missing CURSEFORGE_TOKEN")
    url = f"{CURSEFORGE}/projects/{t['curseforge']}/upload-file"
    base = {"changelog": changelog(plan), "changelogType": "markdown", "releaseType": release_type(plan["version"])}
    primary = dict(base, displayName=f"AnvilCraft {plan['version']} · NeoForge {t['game']}",
                   gameVersionNames=[t["game"], "NeoForge", "Client", "Server"],
                   relations={"projects": [{"slug": "anvil-lib", "type": "embeddedLibrary"}]})

    def upload(name, metadata):
        return multipart(url, "X-Api-Token: " + token, {"metadata": metadata, "file": folder / name})["id"]

    parent = send(t["sha"], context + "/jar", lambda: upload(t["jar"], primary))
    receipts = [parent]
    for classifier in ("sources",):
        name = t["jar"][:-4] + f"-{classifier}.jar"
        metadata = dict(base, parentFileID=int(parent))
        receipts.append(send(t["sha"], context + "/" + classifier, lambda: upload(name, metadata)))
    return ", ".join(receipts)


def run(tag, line, platform):
    plan = tag_manifest(tag)
    require(plan and line in plan["targets"], "Release tag or Minecraft line not found")
    context = f"release/{tag}/{line}/{platform}"
    with tempfile.TemporaryDirectory() as directory:
        folder = files_for(plan, line, Path(directory))
        publish = {"modrinth": publish_modrinth, "curseforge": publish_curseforge}[platform]
        receipt = publish(plan, line, folder, context)
    summary(f"**{platform} · Minecraft {plan['targets'][line]['game']}** — accepted: {receipt}")


if __name__ == "__main__":
    try:
        run(*sys.argv[1:])
    except Exception as error:
        summary(f"**Distribution incomplete (not retried)** — {' / '.join(sys.argv[1:])}\n\n{error}")
        print("::warning::Mod-site upload incomplete. See the job summary for the receipt and failure reason. No automatic retry.")
        raise
