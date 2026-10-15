import unittest

from check_test_layout import layout_issues
from parse_tests import parse_source


class TestLayoutTest(unittest.TestCase):
    def issues(self, source):
        return layout_issues(parse_source(source, 'ExampleTest.java')[1])

    def test_nested_scopes_have_independent_order(self):
        source = '''class ExampleTest {
            @Test void testZ() {}
            @Nested class Nested {
                @Test void testA() {}
                @Test void testB() {}
            }
        }'''
        self.assertEqual([], self.issues(source))

    def test_parameterized_methods_participate_in_source_order(self):
        source = '''class ExampleTest {
            @ParameterizedTest @ValueSource(ints = {1, 2}) void testZ(int n) {}
            @Test void testA() {}
        }'''
        issues = self.issues(source)
        self.assertEqual(1, len(issues))
        self.assertEqual('testA', issues[0]['method'])
        self.assertIn('testZ', issues[0]['reason'])

    def test_method_visibility_and_behavior_names(self):
        source = '''class ExampleTest {
            @Test public void testA() {}
            @Test void unnamedBehavior() {}
            @Test void test2ndPass_parse() {}
            @Test void testParse_Pass3() {}
            @Test void testParse_Batch2() {}
        }'''
        reasons = [issue['reason'] for issue in self.issues(source)]
        self.assertEqual(1, reasons.count('Test methods must be package-private'))
        self.assertEqual(1, reasons.count('Test names must start with test'))
        self.assertEqual(3, reasons.count('Name the behavior instead of the cleanup pass or batch'))

    def test_comments_literals_and_helpers_do_not_create_tests(self):
        source = '''class ExampleTest {
            // @Test public void testZ() {}
            String example = "@Test public void testZ() {}";
            public void helper() {}
            @Test void testA() { String message = "public"; }
            @Test void testB() {}
        }'''
        self.assertEqual([], self.issues(source))

    def test_order_is_case_insensitive_and_stays_with_the_file(self):
        _, first = parse_source('class ExampleTest { @Test void testAlpha() {} @Test void testbeta() {} }', 'First.java')
        _, second = parse_source('class ExampleTest { @Test void testA() {} }', 'Second.java')
        self.assertEqual([], layout_issues(first + second))


if __name__ == '__main__':
    unittest.main()
