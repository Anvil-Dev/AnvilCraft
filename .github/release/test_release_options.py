"""验证版本分类、发布说明和重试，不连接外部服务。"""

import io
import json
import os
import tempfile
import unittest
from contextlib import chdir
from pathlib import Path
from unittest.mock import patch

import common
import mod_sites
import release
from test_release import DRAFT, PLAN

# 版本、模组平台类型及 patch 升版规则。
CASES = [
    ("2.0.0", "release", True),
    ("2.0.0+alpha.1", "alpha", False),
    ("2.0.0+beta.2", "beta", False),
    ("2.0.0+pre-release.3", "beta", False),
    ("2.0.0+rc.4", "beta", False),
    ("2.0.0+build.5", "release", False),
    ("2.0.0+hotfix.6", "release", False),
]


@patch.dict(os.environ, {
    "GITHUB_REPOSITORY": "example/AnvilCraft", "GITHUB_RUN_ID": "123",
    "GITHUB_REF": "refs/heads/dev/1.21/1.6", "GITHUB_SHA": "sha-1.21",
    "DRY_RUN": "false", "GENERATE_RELEASE_NOTES": "false",
    "MODRINTH_TOKEN": "test", "CURSEFORGE_TOKEN": "test",
})
class ReleaseOptionsTests(unittest.TestCase):
    def setUp(self):
        self.enterContext(patch.object(release, "summary"))
        self.enterContext(patch.object(mod_sites, "summary"))

    def test_exact_version_formats_and_types(self):
        for version, kind, _ in CASES:
            with self.subTest(version=version):
                self.assertEqual(common.mod_version(f"mod_version={version}\r\n".encode()), version)
                self.assertEqual(common.release_type(version), kind)
        for version in ("2.0", "02.0.0", "2.0.0-beta.1", "2.0.0+alpha", "2.0.0+rc.01", "2.0.0+beta.-1",
                        "2.0.0+snapshot.5", "2.0.0+unknown.1", "2.0.0+build.1+rc.2", "2.0.0+hotfix.1/other"):
            with self.subTest(invalid=version), self.assertRaises(ValueError):
                common.release_type(version)

    def test_prepare_freezes_native_notes_and_bump_policy(self):
        refs = [{"ref": f"refs/heads/dev/{line}/1.6", "object": {"sha": f"sha-{line}"}} for line in common.LINES]
        notes = "## What's Changed\n* Main branch PR #42\n\n## New Contributors\n* @contributor\n"
        for version, _, bump in CASES:
            for enabled in (False, True):
                def content(path, _sha):
                    return b"mod_version=1.6.0\njava_version=25\n" if path == "gradle.properties" else b""
                def api(path, data=None):
                    if path == "git/matching-refs/heads/dev/":
                        return refs
                    self.fail(path)
                with self.subTest(version=version, notes=enabled), tempfile.TemporaryDirectory() as tmp, chdir(tmp), \
                        patch.dict(os.environ, {"GENERATE_RELEASE_NOTES": str(enabled).lower(), "DRY_RUN": "true", "RELEASE_TAG": version}), \
                        patch.object(release, "artifact", side_effect=[None, {"id": 1}]), \
                        patch.object(release, "contents", side_effect=content), \
                        patch.object(release, "github", side_effect=api) as github, \
                        patch.object(release, "generate_notes", return_value={"previous_tag_name": "v1.9.0", "release_notes": notes}) as generate, \
                        patch.object(release, "gh"), patch.object(release, "output") as output:
                    release.prepare()
                    plan = json.loads(Path("release-plan.json").read_text())
                    self.assertEqual(plan.get("release_notes"), notes if enabled else None)
                    self.assertEqual(dict(c.args for c in output.call_args_list)["bump_patch"], str(bump).lower())
                    # 分支或输入变化后，重跑仍需复用原计划。
                    with patch.dict(os.environ, {"GENERATE_RELEASE_NOTES": str(not enabled).lower()}):
                        release.prepare()
                    self.assertEqual(github.call_count, 1)
                    self.assertEqual(generate.call_count, 1 if enabled else 0)
                    self.assertEqual(plan.get("previous_tag_name"), "v1.9.0" if enabled else None)
                    self.assertEqual(json.loads(Path("release-plan.json").read_text()), plan)

    def test_invalid_tag_is_rejected_before_preflight(self):
        for tag in ("", "v2.0.0", "2.0", "2.0.0-beta.1", "2.0.0+snapshot.5", "2.0.0\n", "$(example)"):
            with self.subTest(tag=tag), patch.dict(os.environ, {"RELEASE_TAG": tag}), \
                    patch.object(release, "artifact") as artifact, patch.object(release, "github") as github:
                with self.assertRaises(ValueError):
                    release.prepare()
                artifact.assert_not_called()
                github.assert_not_called()

    def test_rerun_cannot_relabel_a_frozen_plan(self):
        with tempfile.TemporaryDirectory() as tmp, chdir(tmp), \
                patch.dict(os.environ, {"RELEASE_TAG": "2.0.1"}), \
                patch.object(release, "artifact", return_value={"id": 1}), patch.object(release, "gh"), \
                patch.object(release, "github") as github:
            Path("release-plan.json").write_text(json.dumps(PLAN))
            with self.assertRaisesRegex(ValueError, "tag differs"):
                release.prepare()
            github.assert_not_called()

    def test_qualified_tags_preserve_version_and_original_notes(self):
        for version, _, _ in CASES:
            tag = version
            targets = {}
            for line in common.LINES:
                stem = f"anvilcraft-neoforge-{line}-{version}"
                targets[line] = {
                    "sha": "a" * 40, "game": line, "jar": stem + ".jar",
                    "maven_group": "dev.dubhe", "maven_artifact": f"anvilcraft-neoforge-{line}",
                    "staging_sha256": "c" * 64,
                    "files": {stem + suffix: "b" * 64 for suffix in (".jar", "-sources.jar")},
                }
            plan = {"tag": tag, "version": version, "run": "123", "repo": "example/AnvilCraft",
                    "targets": targets, "release_notes": "## What's Changed\n* PR #42"}
            annotation = {"object": {"sha": "a" * 40}, "message": f"AnvilCraft {tag}\n\n" + json.dumps(plan)}
            with self.subTest(version=version), patch.object(common, "github", side_effect=[
                {"object": {"type": "tag", "sha": "c" * 40}}, annotation,
            ]):
                self.assertEqual(common.tag_manifest(tag), plan)

    def test_seal_preserves_generated_notes_when_reusing_a_draft(self):
        native = "## What's Changed\n* PR #42 by @contributor\n"
        plan = dict(PLAN, tag="2.0.0+beta.1", version="2.0.0+beta.1", release_notes=native)
        drafts, tags = [], []
        def api(path, data=None, **kwargs):
            if path == "git/tags":
                tags.append(json.loads(data["message"].split("\n\n", 1)[1]))
                return {"sha": "tag-object"}
            if path == "git/refs":
                return {}
            if path == "releases":
                drafts.append(dict(data, id=42, assets=DRAFT["assets"]))
            return drafts[0]
        with tempfile.TemporaryDirectory() as tmp, chdir(tmp), \
                patch.object(release, "tag_manifest", side_effect=lambda _: tags[0] if tags else None), \
                patch.object(release, "release_by_tag", side_effect=lambda _: drafts[0] if drafts else None), \
                patch.object(release, "github", side_effect=api), patch.object(release, "gh") as upload, \
                patch.object(release, "digest", side_effect=lambda p: Path(p).stem):
            Path("release-plan.json").write_text(json.dumps(plan))
            for line, target in plan["targets"].items():
                folder = Path("bundles") / f"build-{line}" / "dist"
                folder.mkdir(parents=True)
                (folder / "entry.json").write_text(json.dumps(target))
            release.seal()
            release.seal()
            self.assertEqual((len(tags), len(drafts)), (1, 1))
            self.assertTrue(drafts[0]["prerelease"])
            self.assertEqual(drafts[0]["target_commitish"], "sha-1.21")
            self.assertEqual(drafts[0]["body"].count(native), 1)
            self.assertIn("| Minecraft | Source | JAR | SHA-256 |", drafts[0]["body"])
            upload.assert_not_called()

    def test_publication_type_and_latest_follow_the_version(self):
        for version, kind, _ in CASES:
            prerelease = kind != "release"
            plan = dict(PLAN, version=version)
            draft = dict(DRAFT, prerelease=prerelease)
            with self.subTest(version=version), patch.object(release, "tag_manifest", return_value=plan), \
                    patch.object(release, "release_by_tag", return_value=draft), \
                    patch.object(release, "commit_status", return_value={"state": "success"}), \
                    patch.object(release, "github", return_value={"html_url": "example"}) as github:
                release.publish(version)
                github.assert_called_once_with("releases/42", {
                    "draft": False, "prerelease": prerelease, "make_latest": "false" if prerelease else "true",
                }, method="PATCH")

    def test_prerelease_waits_for_maven_and_rejects_type_drift(self):
        plan = dict(PLAN, version="2.0.0+rc.1")
        with patch.object(release, "tag_manifest", return_value=plan), \
                patch.object(release, "release_by_tag", return_value=dict(DRAFT, prerelease=True)) as lookup, \
                patch.object(release, "commit_status", return_value={"state": "pending"}), \
                patch.object(release, "github") as github:
            with self.assertRaisesRegex(ValueError, "Maven has not published"):
                release.publish(plan["version"])
            lookup.return_value = dict(DRAFT, prerelease=False)
            with self.assertRaisesRegex(ValueError, "Conflicting release type"):
                release.publish(plan["version"])
            github.assert_not_called()

    def test_all_qualified_versions_skip_bump_without_writing(self):
        for version, _, bump in CASES:
            if bump:
                continue
            with self.subTest(version=version), patch.object(release, "tag_manifest", return_value=dict(PLAN, version=version)), \
                    patch.object(release, "release_by_tag", return_value={"draft": False}), \
                    patch.object(release, "github") as github, patch.object(release, "contents") as contents:
                release.bump(version, "26.1")
                github.assert_not_called()
                contents.assert_not_called()

    def test_mod_sites_use_matching_types_for_primary_and_attachments(self):
        target = {"sha": "sha", "game": "26.1.2", "modrinth": "mod", "curseforge": "123", "jar": "x.jar",
                  "files": {n: "hash" for n in ("x.jar", "x-sources.jar")}}
        for version, kind, _ in CASES:
            plan = dict(PLAN, version=version, targets={"26.1": target})
            def send(_sha, _context, upload, reconcile=None):
                return str(upload())
            with self.subTest(version=version), patch.object(mod_sites, "send", side_effect=send), \
                    patch.object(mod_sites, "digest", return_value="hash"), \
                    patch.object(mod_sites, "multipart", return_value={"id": 12}) as multipart:
                mod_sites.publish_modrinth(plan, "26.1", Path("."), "mr")
                fields = multipart.call_args.args[2]["data"]
                self.assertEqual((fields["version_number"], fields["version_type"]), (version, kind))
                multipart.reset_mock()
                mod_sites.publish_curseforge(plan, "26.1", Path("."), "cf")
                self.assertEqual(len(multipart.call_args_list), 2)
                for call in multipart.call_args_list:
                    self.assertEqual(call.args[2]["metadata"]["releaseType"], kind)

    def test_modrinth_reconciles_prerelease_without_reupload(self):
        files = {n: "hash" for n in ("x.jar", "x-sources.jar")}
        target = {"sha": "sha", "game": "26.1.2", "modrinth": "mod", "jar": "x.jar", "files": files}
        plan = dict(PLAN, version="2.0.0+rc.1", targets={"26.1": target})
        for kind in ("beta", "release"):
            remote = {"id": "accepted", "project_id": "mod", "version_number": plan["version"],
                      "version_type": kind, "game_versions": ["26.1.2"], "loaders": ["neoforge"],
                      "files": [{"filename": n, "hashes": {"sha512": "hash"}} for n in files]}
            with self.subTest(remote_type=kind), patch.object(mod_sites, "digest", return_value="hash"), \
                    patch.object(mod_sites, "send", side_effect=lambda _s, _c, _u, reconcile: reconcile()), \
                    patch.object(mod_sites, "urlopen", return_value=io.BytesIO(json.dumps(remote).encode())), \
                    patch.object(mod_sites, "multipart") as upload:
                if kind == "beta":
                    self.assertEqual(mod_sites.publish_modrinth(plan, "26.1", Path("."), "mr"), "accepted")
                else:
                    with self.assertRaisesRegex(ValueError, "different release metadata"):
                        mod_sites.publish_modrinth(plan, "26.1", Path("."), "mr")
                upload.assert_not_called()
