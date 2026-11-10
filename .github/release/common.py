"""共享发布元数据和 GitHub API 访问。"""

import base64
import hashlib
import json
import os
import re
import subprocess
from pathlib import Path
from urllib.error import HTTPError
from urllib.parse import quote
from urllib.request import Request, urlopen

LINES = ("1.21", "26.1")
ANCHOR = "1.21"
BRANCHES = {line: f"dev/{line}/1.6" for line in LINES}
VERSION_CORE = r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)"
VERSION = rf"{VERSION_CORE}(?:\+(?:alpha|beta|pre-release|rc|build|hotfix)\.(?:0|[1-9][0-9]*))?"
VERSION_LINE = re.compile(rb"(?m)^(mod_version[ \t]*=[ \t]*)([^\s#]+)([ \t]*)(?=\r?$)")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def repo():
    return os.environ["GITHUB_REPOSITORY"]


def github(path, data=None, *, method=None, missing=False):
    url = f"https://api.github.com/repos/{repo()}/{path}"
    request = Request(url, method=method, headers={
        "Authorization": "Bearer " + os.environ["GH_TOKEN"],
        "Accept": "application/vnd.github+json", "Content-Type": "application/json",
        "X-GitHub-Api-Version": "2022-11-28",
    }, data=None if data is None else json.dumps(data).encode())
    try:
        with urlopen(request, timeout=60) as response:
            return json.load(response) if response.status != 204 else None
    except HTTPError as error:
        if missing and error.code == 404:
            return None
        raise RuntimeError(f"GitHub {request.get_method()} {path}: HTTP {error.code}: "
                           + error.read().decode()[:500]) from error


def gh(*args):
    return subprocess.check_output(["gh", *args, "--repo", repo()], text=True).strip()


def contents(path, ref):
    data = github(f"contents/{path}?ref={quote(ref, safe='')}")
    return base64.b64decode(data["content"])


def release_type(version):
    require(re.fullmatch(VERSION, version), f"Invalid release version: {version}")
    qualifier = version.partition("+")[2].partition(".")[0]
    if qualifier == "alpha":
        return "alpha"
    return "beta" if qualifier in {"beta", "pre-release", "rc"} else "release"


def mod_version(properties):
    matches = VERSION_LINE.findall(properties)
    require(len(matches) == 1, "Expected one mod_version property")
    return matches[0][1].decode()


def digest(path, algorithm="sha256"):
    with Path(path).open("rb") as file:
        return hashlib.file_digest(file, algorithm).hexdigest()


def staging_digest(directory):
    directory = Path(directory)
    manifest = hashlib.sha256()
    for path in sorted(directory.rglob("*"), key=lambda p: p.relative_to(directory).as_posix()):
        if path.is_file():
            manifest.update(path.relative_to(directory).as_posix().encode() + b"\0")
            manifest.update(bytes.fromhex(digest(path)))
    return manifest.hexdigest()


def output(key, value):
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as file:
        print(f"{key}={value}", file=file)


def summary(message):
    print(message, flush=True)
    if os.getenv("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as file:
            print(message, file=file)


def artifact(name):
    run = os.environ["GITHUB_RUN_ID"]
    items = github(f"actions/runs/{run}/artifacts?name={quote(name)}")["artifacts"]
    if items:
        require(not items[0]["expired"], f"The original artifact {name} has expired")
        return items[0]
    return None


def tag_manifest(tag):
    require(re.fullmatch(VERSION, tag), f"Invalid release tag: {tag}")
    ref = github(f"git/ref/tags/{tag}", missing=True)
    if ref is None:
        return None
    require(ref["object"]["type"] == "tag", f"{tag} already exists as a lightweight tag")
    obj = github("git/tags/" + ref["object"]["sha"])
    manifest = json.loads(obj["message"].strip().split("\n\n", 1)[1])
    require(manifest["tag"] == tag and manifest["repo"] == repo()
            and manifest["version"] == tag and re.fullmatch(r"[0-9]+", manifest["run"])
            and set(manifest["targets"]) == set(LINES)
            and obj["object"]["sha"] == manifest["targets"][ANCHOR]["sha"], "Conflicting release tag")
    for target in manifest["targets"].values():
        require(re.fullmatch(r"[0-9]+(?:\.[0-9]+)+", target["game"])
                and re.fullmatch(r"[0-9a-f]{40}", target["sha"]), "Invalid Minecraft version or source SHA")
        stem = f"anvilcraft-neoforge-{target['game']}-{tag}"
        require(target["jar"] == stem + ".jar" and set(target["files"]) ==
                {stem + suffix for suffix in (".jar", "-sources.jar")}
                and all(re.fullmatch(r"[0-9a-f]{64}", value) for value in target["files"].values()),
                "Invalid filename or checksum in the release tag")
        require(re.fullmatch(r"[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+", target["maven_group"])
                and target["maven_artifact"] == f"anvilcraft-neoforge-{target['game']}"
                and re.fullmatch(r"[0-9a-f]{64}", target["staging_sha256"]),
                "Invalid Maven coordinates or staging checksum in the release tag")
    return manifest


def commit_status(sha, context):
    page = 1
    while True:
        statuses = github(f"commits/{sha}/statuses?per_page=100&page={page}")
        for status in statuses:
            if status["context"] == context:
                return status
        if len(statuses) < 100:
            return None
        page += 1


def record(sha, context, state, description):
    run_url = f"https://github.com/{repo()}/actions/runs/{os.environ['GITHUB_RUN_ID']}"
    github(f"statuses/{sha}", {"context": context, "state": state, "description": description,
                              "target_url": run_url})
