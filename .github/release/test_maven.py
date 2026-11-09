"""验证 Maven 部分上传恢复和原版附件补发，不连接外部服务。"""

import hashlib
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from unittest.mock import patch

import maven
import mod_sites
from common import staging_digest


class MavenTests(unittest.TestCase):
    def setUp(self):
        self.root = Path(self.enterContext(tempfile.TemporaryDirectory()))
        self.prefix = "dev/dubhe/anvilcraft-neoforge-1.21.1"
        self.version = "1.6.0+rc.1"
        self.folder = self.root / self.prefix / self.version
        self.folder.mkdir(parents=True)
        self.file = self.folder / f"anvilcraft-neoforge-1.21.1-{self.version}.jar"
        self.file.write_bytes(b"original jar")
        self.metadata_path = self.prefix + "/maven-metadata.xml"
        self.metadata = ("<metadata><groupId>dev.dubhe</groupId><artifactId>anvilcraft-neoforge-1.21.1</artifactId>"
                         "<versioning><latest>{version}</latest><release>{version}</release>"
                         "<versions><version>{version}</version></versions><lastUpdated>20261109000000</lastUpdated>"
                         "</versioning></metadata>")
        (self.root / self.metadata_path).write_text(self.metadata.format(version=self.version), encoding="utf-8")
        self.target = {"sha": "source", "game": "1.21.1", "maven_group": "dev.dubhe",
                       "maven_artifact": "anvilcraft-neoforge-1.21.1", "staging_sha256": staging_digest(self.root)}
        self.plan = {"tag": "v" + self.version, "version": self.version, "targets": {"1.21": self.target}}
        self.enterContext(patch.object(maven, "tag_manifest", return_value=self.plan))
        self.enterContext(patch.object(maven, "summary"))
        self.args = (self.plan["tag"], "1.21", self.root)

    def test_retry_reconciles_accepted_put_and_preserves_older_versions(self):
        remote = {self.metadata_path: self.metadata.format(version="1.5.0").encode()}
        receipts, puts = {}, []
        interrupted = False

        def request(path, data=None):
            nonlocal interrupted
            if data is None:
                return remote.get(path)
            puts.append(path)
            remote[path] = data
            if not interrupted:
                interrupted = True
                raise TimeoutError("response lost after acceptance")
            return b""

        def record(sha, context, state, description):
            receipts[context] = {"state": state, "description": description}

        with patch.object(maven, "request", side_effect=request), patch.object(maven, "record", side_effect=record), \
                patch.object(maven, "commit_status", side_effect=lambda sha, context: receipts.get(context)):
            with self.assertRaises(TimeoutError):
                maven.deploy(*self.args)
            maven.deploy(*self.args)
            completed_puts = puts.copy()
            maven.deploy(*self.args)
        self.assertEqual(puts, completed_puts)
        self.assertEqual(puts.count(self.file.relative_to(self.root).as_posix()), 1)
        self.assertEqual([v.text for v in ET.fromstring(remote[self.metadata_path]).findall("versioning/versions/version")],
                         ["1.5.0", self.version])
        for algorithm in ("md5", "sha1", "sha256", "sha512"):
            self.assertEqual(remote[self.metadata_path + "." + algorithm],
                             hashlib.new(algorithm, remote[self.metadata_path]).hexdigest().encode())
        self.assertEqual(next(iter(receipts.values()))["state"], "success")

    def test_conflicting_remote_file_is_never_overwritten(self):
        with patch.object(maven, "request", return_value=b"other jar") as request, \
                patch.object(maven, "commit_status", return_value={"state": "pending"}), \
                patch.object(maven, "record") as record:
            with self.assertRaisesRegex(ValueError, "Conflicting Maven file"):
                maven.deploy(*self.args)
            self.assertEqual(request.call_count, 1)
            self.assertEqual(record.call_args.args[2], "pending")

    def test_changed_staging_blocks_upload(self):
        self.file.write_bytes(b"changed jar")
        with patch.object(maven, "request") as request, patch.object(maven, "commit_status", return_value=None), \
                patch.object(maven, "record") as record:
            with self.assertRaisesRegex(ValueError, "Staging files changed"):
                maven.deploy(*self.args)
            request.assert_not_called()
            record.assert_not_called()

    def test_expired_artifacts_use_verified_original_maven_sources(self):
        jar = self.file.name
        source = jar[:-4] + "-sources.jar"
        target = dict(self.target, jar=jar, files={
            jar: hashlib.sha256(b"main").hexdigest(), source: hashlib.sha256(b"sources").hexdigest(),
        })
        plan = dict(self.plan, run="123", targets={"1.21": target})
        with patch.object(mod_sites, "github", return_value={"artifacts": [{"expired": True}]}), \
                patch.object(mod_sites, "gh", side_effect=lambda *args: (self.root / jar).write_bytes(b"main")), \
                patch.object(mod_sites, "maven_request", return_value=b"sources") as download:
            self.assertEqual(mod_sites.files_for(plan, "1.21", self.root), self.root)
            download.assert_called_once_with(self.prefix + "/" + self.version + "/" + source)
            download.return_value = b"modified sources"
            with self.assertRaisesRegex(ValueError, "Checksum mismatch"):
                mod_sites.files_for(plan, "1.21", self.root)


if __name__ == "__main__":
    unittest.main()
