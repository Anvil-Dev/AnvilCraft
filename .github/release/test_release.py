"""验证发布失败与重试，不连接外部服务。"""

import json
import os
import tempfile
import unittest
from contextlib import chdir
from pathlib import Path
from unittest.mock import Mock, patch

import mod_sites
import release
from common import LINES, digest, mod_version, staging_digest

TAG = "v2.0.0"
PLAN = {"tag": TAG, "version": "2.0.0", "targets": {
    line: {"sha": f"sha-{line}", "game": line, "jar": f"{line}.jar", "files": {f"{line}.jar": line}}
    for line in LINES}}
DRAFT = {"id": 42, "draft": True, "tag_name": TAG, "assets": [
    {"name": f"{line}.jar", "state": "uploaded", "digest": "sha256:" + line} for line in LINES]}


@patch.dict(os.environ, {"GITHUB_REPOSITORY": "example/AnvilCraft", "GITHUB_RUN_ID": "123",
                         "GITHUB_STEP_SUMMARY": os.devnull})
class ReleaseTests(unittest.TestCase):
    def setUp(self):
        for module in (release, mod_sites):
            self.enterContext(patch.object(module, "summary"))

    def test_preflight_rejects_a_branch_with_a_different_version(self):
        refs = [{"ref": f"refs/heads/dev/{line}/1.6", "object": {"sha": line}} for line in LINES]
        def content(path, sha):
            if path != "gradle.properties":
                return b""
            version = "2.0.1" if sha == "26.1" else "2.0.0"
            return f"java_version=25\nmod_version={version}\n".encode()
        with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/dev/1.21/1.6", "GITHUB_SHA": "1.21"}), \
                patch.object(release, "artifact", return_value=None), patch.object(release, "github", return_value=refs), \
                patch.object(release, "contents", side_effect=content), patch.object(release, "output") as output:
            with self.assertRaisesRegex(ValueError, "Branches disagree"):
                release.prepare()
            output.assert_not_called()

    def test_staging_digest_covers_extra_and_modified_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            pom = root / "module.pom"
            pom.write_bytes(b"original")
            expected = staging_digest(root)
            pom.write_bytes(b"changed")
            self.assertNotEqual(expected, staging_digest(root))
            pom.write_bytes(b"original")
            (root / "old-version.pom").touch()
            self.assertNotEqual(expected, staging_digest(root))

    def test_release_gates_on_every_maven_receipt_only(self):
        for missing in [None, LINES[-1]]:
            def status(sha, context):
                if context.endswith("/maven"):
                    return {"state": "pending" if sha == f"sha-{missing}" else "success"}
                # 两个模组平台均不可用。
                return {"state": "failure"}
            with self.subTest(missing=missing), patch.object(release, "tag_manifest", return_value=PLAN), \
                    patch.object(release, "release_by_tag", return_value=DRAFT), \
                    patch.object(release, "commit_status", side_effect=status), \
                    patch.object(release, "github", return_value={"html_url": "example"}) as github:
                if missing:
                    with self.assertRaisesRegex(ValueError, "Maven has not published"):
                        release.publish(TAG)
                    github.assert_not_called()
                else:
                    release.publish(TAG)
                    github.assert_called_once_with("releases/42", {
                        "draft": False, "prerelease": False, "make_latest": "true",
                    }, method="PATCH")

    def test_missing_or_changed_asset_blocks_publication(self):
        for assets in [DRAFT["assets"][:-1], [dict(DRAFT["assets"][0], digest="changed"), *DRAFT["assets"][1:]]]:
            with self.assertRaisesRegex(ValueError, "conflicting"):
                release.verify_assets(dict(DRAFT, assets=assets), PLAN["targets"])

    def test_seal_resumes_the_same_draft_after_partial_upload(self):
        tags, drafts, uploads = [], [], []
        interrupted = False
        def github(path, data=None, **kwargs):
            if path == "git/tags":
                tags.append(json.loads(data["message"].split("\n\n", 1)[1]))
                return {"sha": "tag-object"}
            if path == "git/refs":
                return {}
            if path == "releases":
                drafts.append(dict(data, id=42, assets=[]))
            return drafts[0]
        def upload(_release, _upload, _tag, path):
            nonlocal interrupted
            if len(uploads) == 1 and not interrupted:
                interrupted = True
                raise OSError("connection lost")
            name = Path(path).name
            uploads.append(name)
            drafts[0]["assets"].append({"name": name, "state": "uploaded", "digest": "sha256:" + digest(path)})
        with tempfile.TemporaryDirectory() as tmp, chdir(tmp), patch.dict(os.environ, {"DRY_RUN": "false"}), \
                patch.object(release, "tag_manifest", side_effect=lambda _: tags[0] if tags else None), \
                patch.object(release, "release_by_tag", side_effect=lambda _: drafts[0] if drafts else None), \
                patch.object(release, "github", side_effect=github), patch.object(release, "gh", side_effect=upload):
            Path("release-plan.json").write_text(json.dumps(PLAN))
            for line, target in PLAN["targets"].items():
                folder = Path("bundles") / f"build-{line}" / "dist"
                folder.mkdir(parents=True)
                jar = folder / target["jar"]
                jar.write_bytes(line.encode())
                (folder / "entry.json").write_text(json.dumps(dict(target, files={jar.name: digest(jar)})))
            with self.assertRaises(OSError):
                release.seal()
            release.seal()
            release.seal()
            self.assertEqual((len(tags), len(drafts), len(uploads), len(set(uploads))), (1, 1, 2, 2))

    def test_partial_curseforge_upload_resumes_each_file(self):
        ledger, uploads = {}, []
        def record(_, context, state, description):
            ledger[context] = {"state": state, "description": description}
        def upload(url, auth, fields):
            name = fields["file"].name
            uploads.append(name)
            if name == "x-sources.jar" and len(uploads) == 2:
                raise mod_sites.Rejected("HTTP 429")
            return {"id": len(uploads)}
        target = {"sha": "sha", "game": "26.1.2", "curseforge": "123", "jar": "x.jar"}
        with patch.dict(os.environ, {"CURSEFORGE_TOKEN": "test"}), \
                patch.object(mod_sites, "commit_status", side_effect=lambda _, key: ledger.get(key)), \
                patch.object(mod_sites, "record", side_effect=record), patch.object(mod_sites, "multipart", side_effect=upload):
            args = (dict(PLAN, targets={"26.1": target}), "26.1", mod_sites.Path("."), "cf")
            with self.assertRaises(mod_sites.Rejected):
                mod_sites.publish_curseforge(*args)
            for _ in range(2):
                self.assertEqual(mod_sites.publish_curseforge(*args), "1, 3")
            self.assertEqual(uploads, ["x.jar", "x-sources.jar", "x-sources.jar"])

    def test_uncertain_upload_requires_reconciliation(self):
        with patch.object(mod_sites, "commit_status", return_value={"state": "pending"}), \
                patch.object(mod_sites, "record"):
            upload = Mock()
            with self.assertRaisesRegex(ValueError, "remote file"):
                mod_sites.send("sha", "context", upload)
            self.assertEqual(mod_sites.send("sha", "context", upload, lambda: "verified-id"), "verified-id")
            upload.assert_not_called()

    def test_bump_preserves_bytes_and_reuses_single_commit_and_pr(self):
        original = b"# Config\r\nmod_version=2.0.0\r\njava_version=25\r\n"
        files = {"base": original}
        head, prs, payloads = [], [], []
        def github(path, data=None, **kwargs):
            if data is not None:
                payloads.append((path, data))
                if path == "git/trees":
                    files["bump"] = data["tree"][0]["content"].encode()
                    return {"sha": "new-tree"}
                if path == "git/commits":
                    return {"sha": "bump"}
                if path == "git/refs":
                    head.append({"object": {"sha": "bump"}})
                    return {}
                if path == "pulls":
                    prs.append(dict(data, state="open", number=1, html_url="example"))
                    return prs[0]
            if path.startswith("git/ref/heads/release-bump"):
                return head[0] if head else None
            if path.startswith("git/ref/heads/dev/"):
                return {"object": {"sha": "base"}}
            if path == "git/commits/base":
                return {"tree": {"sha": "base-tree"}}
            if path == "commits/bump":
                message = next(data["message"] for key, data in payloads if key == "git/commits")
                return {"commit": {"message": message}, "parents": [{"sha": "base"}], "files": [{"filename": "gradle.properties"}]}
            if path.startswith("pulls?"):
                return prs
            self.fail(path)
        with patch.object(release, "tag_manifest", return_value=PLAN), \
                patch.object(release, "release_by_tag", return_value={"draft": False}), \
                patch.object(release, "contents", side_effect=lambda _, sha: files[sha]), \
                patch.object(release, "github", side_effect=github):
            release.bump(TAG, "26.1")
            release.bump(TAG, "26.1")
            self.assertEqual(files["bump"], original.replace(b"2.0.0", b"2.0.1"))
            self.assertEqual(len(payloads), 4)
            self.assertEqual(prs[0]["title"], next(d["message"] for key, d in payloads if key == "git/commits"))
            self.assertIn("[skip ci]", prs[0]["title"])
            files["base"] = files["bump"]
            release.bump(TAG, "26.1")
            files["base"] = original.replace(b"2.0.0", b"2.1.0")
            with self.assertRaisesRegex(ValueError, "expected"):
                release.bump(TAG, "26.1")
            files["base"] = original
            prs[0]["state"] = "closed"
            with self.assertRaisesRegex(ValueError, "was closed"):
                release.bump(TAG, "26.1")
            self.assertEqual(len(payloads), 4)

    def test_invalid_release_versions(self):
        for raw in (b"", b"mod_version=2.0", b"mod_version=02.0.0", b"mod_version=2.0.0+snapshot.5",
                    b"mod_version=2.0.0\nmod_version=2.0.1"):
            with self.assertRaises(ValueError):
                mod_version(raw)


if __name__ == "__main__":
    unittest.main()
