import unittest

from release_metadata import Version, latest_stable_release, plan_release, read_version


def release(tag="v1.0.0", draft=False, commit="previous", prerelease=False):
    return {"tag_name": tag, "draft": draft, "prerelease": prerelease, "target_commitish": commit}


class ReleaseMetadataTest(unittest.TestCase):
    def test_reads_current_gradle_format(self):
        self.assertEqual(Version("1.0.1", 2), read_version('    versionCode = 2\n    versionName = "1.0.1"\n'))

    def test_rejects_ambiguous_or_unsafe_versions(self):
        for source in (
            'versionCode = 0\nversionName = "1.0.1"',
            'versionCode = 2\nversionName = "1.0.1-rc1"',
            'versionCode = 2\nversionName = "$(id)"',
            'versionCode = 2\nversionName = "01.0.1"',
            'versionCode = 2\nversionName = "1.0.1"\nversionName = "2.0.0"',
            'versionName = "1.0.1"',
        ):
            with self.subTest(source=source), self.assertRaises(ValueError):
                read_version(source)

    def test_new_version_creates_release(self):
        self.assertEqual("create", plan_release(Version("1.0.1", 2), [release()], Version("1.0.0", 1), "current"))

    def test_first_release_needs_no_previous_version(self):
        self.assertEqual("create", plan_release(Version("1.0.0", 1), [], None, "current"))

    def test_normal_push_or_rerun_does_not_replace_published_release(self):
        self.assertEqual("skip", plan_release(Version("1.0.0", 1), [release()], Version("1.0.0", 1), "later-commit"))

    def test_version_code_must_increase_for_android_updates(self):
        for code in (1, 2):
            with self.subTest(code=code), self.assertRaisesRegex(ValueError, "versionCode"):
                plan_release(Version("1.0.2", code), [], Version("1.0.1", 2), "current")

    def test_rejects_older_release_names(self):
        with self.assertRaisesRegex(ValueError, "versionName"):
            plan_release(Version("1.0.1", 5), [], Version("1.0.2", 4), "current")

    def test_version_comparison_is_numeric(self):
        self.assertEqual("create", plan_release(Version("1.0.10", 11), [], Version("1.0.9", 10), "current"))

    def test_failed_upload_can_resume_its_own_draft(self):
        self.assertEqual("resume", plan_release(
            Version("1.0.1", 2), [release("v1.0.1", draft=True, commit="current")], Version("1.0.0", 1), "current"
        ))

    def test_draft_from_another_commit_cannot_be_replaced(self):
        with self.assertRaisesRegex(ValueError, "another commit"):
            plan_release(Version("1.0.1", 2), [release("v1.0.1", draft=True)], Version("1.0.0", 1), "current")

    def test_latest_stable_ignores_drafts_and_prereleases_and_uses_numeric_order(self):
        latest = release("v1.0.10")
        self.assertEqual(latest, latest_stable_release([
            release("v2.0.0", draft=True), release("v3.0.0", prerelease=True),
            release("v1.0.9"), latest, release("preview"),
        ]))
        self.assertIsNone(latest_stable_release([]))


if __name__ == "__main__":
    unittest.main()
