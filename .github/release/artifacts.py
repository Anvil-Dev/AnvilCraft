"""校验锁定源码产生的主包、内嵌依赖和 Maven 发布文件。"""

import hashlib
import json
import shutil
import subprocess
import sys
import tomllib
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

from common import digest, mod_version, require, staging_digest, summary


def collect(source, line, sha, version, dest):
    actual_sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=source, text=True).strip()
    require(actual_sha == sha, f"Expected {sha} for {line}, found {actual_sha}")
    require(mod_version((source / "gradle.properties").read_bytes()) == version, "Version drift")
    catalog = tomllib.loads((source / "gradle/libs.versions.toml").read_text(encoding="utf-8"))
    game = catalog["versions"]["minecraft"]
    properties = dict(row.split("=", 1) for row in (source / "gradle.properties").read_text(encoding="utf-8").splitlines()
                      if "=" in row and not row.startswith("#"))
    group = properties["maven_group"]
    artifact_id = f"anvilcraft-neoforge-{game}"
    stem = f"{artifact_id}-{version}"
    staging = source / "build/release/staging"
    prefix = staging / group.replace(".", "/") / artifact_id / version / stem
    pom = ET.parse(str(prefix) + ".pom").getroot()
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    coords = [pom.findtext("m:" + key, namespaces=ns) for key in ("groupId", "artifactId", "version")]
    require(coords == [group, artifact_id, version], f"Invalid Maven coordinates: {coords}")
    api = {}
    for dep in pom.findall("m:dependencies/m:dependency", ns):
        identity = tuple(dep.findtext("m:" + key, namespaces=ns) for key in ("groupId", "artifactId"))
        api[identity] = (dep.findtext("m:version", namespaces=ns), dep.findtext("m:scope", "compile", ns))
    for name in ("anvillib", "ageratum"):
        library = catalog["libraries"][name]
        expected = catalog["versions"][library["version"]["ref"]]
        require(api.get((library["group"], library["name"])) == (expected, "compile"),
                f"Invalid API dependency for {name}")
    module = json.loads(Path(str(prefix) + ".module").read_text(encoding="utf-8"))
    require([module["component"][key] for key in ("group", "module", "version")] == [group, artifact_id, version],
            "Invalid Gradle module coordinates")
    jar = source / "build/libs" / (stem + ".jar")
    expected = json.loads((source / "build/release/embedded.json").read_text(encoding="utf-8"))
    bundled = {}
    with zipfile.ZipFile(jar) as archive:
        descriptor = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode())
        require(any(m["modId"] == "anvilcraft" and m["version"] == version for m in descriptor["mods"]),
                "Invalid AnvilCraft mod descriptor")
        for nested in json.loads(archive.read("META-INF/jarjar/metadata.json"))["jars"]:
            identity = nested["identifier"]
            key = f"{identity['group']}:{identity['artifact']}:{nested['version']['artifactVersion']}"
            require(key not in bundled, f"Duplicate embedded dependency: {key}")
            bundled[key] = hashlib.sha256(archive.read(nested["path"])).hexdigest()
    require(bundled == expected, "Embedded dependencies differ from the resolved local build")
    dest.mkdir(parents=True, exist_ok=True)
    hashes = {}
    for suffix in (".jar", "-sources.jar"):
        name = stem + suffix
        original = jar.parent / name
        hashes[name] = digest(original)
        require(hashes[name] == digest(Path(str(prefix) + suffix)), f"Maven and mod-site files differ: {name}")
        shutil.copyfile(original, dest / name)
    entry = {"sha": sha, "game": game, "jar": jar.name, "files": hashes,
             "staging_sha256": staging_digest(staging), "maven_group": group, "maven_artifact": artifact_id,
             "modrinth": "vuISAWPc", "curseforge": "986251"}
    (dest / "entry.json").write_text(json.dumps(entry, indent=2) + "\n", encoding="utf-8")
    summary(f"MC {game}: 主包、源码包、{len(bundled)} 个内嵌依赖及 Maven 坐标校验通过。")


if __name__ == "__main__":
    collect(Path(sys.argv[1]), *sys.argv[2:5], Path(sys.argv[5]))
