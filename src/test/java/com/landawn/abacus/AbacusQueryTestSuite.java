package com.landawn.abacus;

import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.landawn.abacus.query")
@ExcludeTags("slow-test")
public class AbacusQueryTestSuite {
}
