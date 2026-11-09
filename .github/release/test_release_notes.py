"""验证日志只比较符合版本类型的祖先 Release tag。"""

import unittest
from unittest.mock import patch

import release_notes


PLAN = {"tag": "v2.0.0", "version": "2.0.0", "targets": {"example": {"sha": "head"}}}


def release(tag, day, *, prerelease=False, draft=False):
    return {"tag_name": tag, "id": day, "published_at": f"2026-01-{day:02}T00:00:00Z",
            "prerelease": prerelease, "draft": draft}


class ReleaseNotesTests(unittest.TestCase):
    def setUp(self):
        self.enterContext(patch.object(release_notes, "summary"))

    def test_latest_skips_prereleases_drafts_and_other_branch_tags(self):
        items = [release("v1.0.0", 1), release("v1.1.0", 2),
                 release("v1.2.0+rc.1", 3, prerelease=True),
                 release("other/branch", 4), release("v2.0.0", 5), release("draft", 6, draft=True)]
        def api(path, data=None, **kwargs):
            if path.startswith("releases?"):
                return items
            if path.startswith("compare/"):
                return {"status": "diverged" if "other%2Fbranch" in path else "ahead"}
            self.assertEqual(data, {"tag_name": "v2.0.0", "target_commitish": "head",
                                    "previous_tag_name": "v1.1.0"})
            return {"body": "## What's Changed\n* PR #42"}
        with patch.object(release_notes, "github", side_effect=api) as github:
            result = release_notes.generate(PLAN, "example")
        self.assertEqual(result, {"previous_tag_name": "v1.1.0", "release_notes": "## What's Changed\n* PR #42"})
        comparisons = [c.args[0] for c in github.call_args_list if c.args[0].startswith("compare/")]
        self.assertEqual(comparisons, ["compare/other%2Fbranch...head?per_page=1", "compare/v1.1.0...head?per_page=1"])

    def test_prerelease_can_follow_either_type_including_same_commit(self):
        plan = dict(PLAN, tag="v2.0.0+rc.2", version="2.0.0+rc.2")
        for pre in (False, True):
            tag = "v2.0.0+rc.1" if pre else "v1.0.0"
            with self.subTest(prerelease=pre), patch.object(release_notes, "github", side_effect=[
                [release(tag, 1, prerelease=pre)], {"status": "identical"}, {"body": "native"},
            ]) as github:
                self.assertEqual(release_notes.generate(plan, "example")["previous_tag_name"], tag)
                self.assertEqual(github.call_args.args[1]["previous_tag_name"], tag)

    def test_reads_all_pages_before_choosing_by_publication_date(self):
        first_page = [release(f"draft-{i}", 1, draft=True) for i in range(100)]
        with patch.object(release_notes, "github", side_effect=[first_page, [release("v1.0.0", 2)],
                                                               {"status": "ahead"}, {"body": "native"}]) as github:
            self.assertEqual(release_notes.generate(PLAN, "example")["previous_tag_name"], "v1.0.0")
        self.assertEqual(github.call_args_list[1].args[0], "releases?per_page=100&page=2")

    def test_first_repository_release_uses_native_initial_notes(self):
        with patch.object(release_notes, "github", side_effect=[[], {"body": "initial"}]) as github:
            self.assertEqual(release_notes.generate(PLAN, "example"),
                             {"previous_tag_name": None, "release_notes": "initial"})
        self.assertEqual(github.call_args.args[1], {"tag_name": "v2.0.0", "target_commitish": "head"})

    def test_no_eligible_ancestor_does_not_let_github_choose_another_type_or_branch(self):
        for status in ("behind", "diverged", None):
            with self.subTest(status=status), patch.object(release_notes, "github", side_effect=[
                [release("v1.0.0+rc.1", 1, prerelease=True), release("other", 2)],
                {"status": status} if status else None,
            ]) as github:
                self.assertEqual(release_notes.generate(PLAN, "example"),
                                 {"previous_tag_name": None, "release_notes": ""})
                self.assertEqual(github.call_count, 2)
