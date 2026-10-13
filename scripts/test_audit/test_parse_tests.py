"""Regressions for the audit rules used to review test deletions."""
import tempfile
import unittest
import subprocess
import sys
from pathlib import Path

from java_tokens import normalize_body
from parse_tests import inventory, parse_source
from merge_file import merge_file


class TestInventory(unittest.TestCase):
    def test_comment_and_literal_annotations_are_not_tests(self):
        src = '''class ExampleTest {
            // @Test void fake() { fail(); }
            String text = "@Test void fake() { fail(); }";
            @Test void real() { assertEquals("// /* @Test */", text); }
        }'''
        _, methods = parse_source(src, "ExampleTest.java")
        self.assertEqual(["real"], [m["name"] for m in methods])

    def test_parameterized_annotations_and_nested_classes(self):
        src = '''class ExampleTest {
            @Nested class Cases {
                @ParameterizedTest(name = "{0}")
                @CsvSource({"a,1", "b,2"})
                void matches(String text, int value) { assertEquals(value, text.length()); }
            }
        }'''
        _, methods = parse_source(src, "ExampleTest.java")
        self.assertEqual("ExampleTest.Cases", methods[0]["class_name"])
        self.assertEqual("matches", methods[0]["name"])
        self.assertEqual("ParameterizedTest", methods[0]["annotation"])

    def test_literal_contents_and_code_token_boundaries_are_preserved(self):
        first = 'void first() { assertEquals("a // b", "x"); /* comment */ }'
        same = 'public void renamed() {\n assertEquals("a // b", "x"); }'
        different = 'void different() { assertEquals("a // c", "x"); }'
        self.assertEqual(normalize_body(first), normalize_body(same))
        self.assertNotEqual(normalize_body(first), normalize_body(different))
        self.assertNotEqual(normalize_body('void x() { a + +b; }'), normalize_body('void x() { a++ + b; }'))

    def test_text_blocks_do_not_change_brace_matching(self):
        src = '''class ExampleTest {
            @Test void sql() { assertEquals("""
                @Test { } // not Java
                """, value); }
            @Test void next() { assertTrue(true); }
        }'''
        _, methods = parse_source(src, "ExampleTest.java")
        self.assertEqual(["sql", "next"], [m["name"] for m in methods])

    def test_assertion_words_in_literals_do_not_count(self):
        _, methods = parse_source('class T { @Test void example() { String x = "assertEquals(1, 1)"; } }', "T.java")
        self.assertFalse(methods[0]["has_assertion"])

    def test_inherited_tags_reach_nested_tests(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tests = root / "src/test/java"
            tests.mkdir(parents=True)
            (tests / "Base.java").write_text('@Tag("unit") class Base {}', encoding="utf-8")
            (tests / "ExampleTest.java").write_text(
                'class ExampleTest extends Base { @Nested class Cases { @Test void works() { assertTrue(true); } } }', encoding="utf-8")
            data = inventory(root)
            self.assertEqual(["unit"], data["methods"][0]["class_tags"])

    def test_merge_refuses_same_name_with_different_assertions(self):
        src = '''class ExampleTest extends TestBase {
            @Test void sameName() { assertEquals("one", value); }
        }
        class LegacyTest extends TestBase {
            @Test void sameName() { assertEquals("two", value); }
        }'''
        with self.assertRaisesRegex(ValueError, "colliding members"):
            merge_file(src, "ExampleTest")

    def test_duplicate_key_preserves_parameter_sources_before_or_after_test_annotation(self):
        src = '''class ExampleTest {
            @ValueSource(strings = "first") @ParameterizedTest
            void first(String value) { assertNotNull(value); }
            @ParameterizedTest @ValueSource(strings = "second")
            void second(String value) { assertNotNull(value); }
        }'''
        _, methods = parse_source(src, "ExampleTest.java")
        self.assertEqual(methods[0]["norm_body"], methods[1]["norm_body"])
        self.assertNotEqual(methods[0]["annotation_key"], methods[1]["annotation_key"])
        self.assertIn('"first"', methods[0]["annotation_key"])
        self.assertIn('"second"', methods[1]["annotation_key"])

    def test_merge_preserves_slow_test_tag(self):
        src = '''@Tag("slow-test") public class ExampleTest extends TestBase {
            @Test void first() { assertEquals(1, value); }
        }
        class LegacyTest extends TestBase {
            @Test void second() { assertEquals(2, value); }
        }'''
        merged = merge_file(src, "ExampleTest")["text"]
        self.assertIn('@Tag("slow-test")', merged)
        self.assertNotIn('@Tag("2025")', merged)

    def test_cross_file_merge_collision_leaves_both_files_unchanged(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "ExampleTest.java"
            source = Path(directory) / "LegacyTest.java"
            original_target = 'class ExampleTest extends TestBase { @Test void sameName() { assertEquals(1, value); } }'
            original_source = 'class LegacyTest extends TestBase { @Test void sameName() { assertEquals(2, value); } }'
            target.write_text(original_target, encoding="utf-8")
            source.write_text(original_source, encoding="utf-8")
            result = subprocess.run([sys.executable, "-B", str(Path(__file__).with_name("merge_across_files.py")),
                                     "--target", str(target), "--source", str(source), "--apply"], capture_output=True, text=True)
            self.assertEqual(2, result.returncode, result.stderr)
            self.assertEqual(original_target, target.read_text(encoding="utf-8"))
            self.assertEqual(original_source, source.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
