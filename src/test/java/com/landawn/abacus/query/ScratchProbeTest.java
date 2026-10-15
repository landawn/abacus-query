package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Temporary probe test for the deep review; deleted after the run. */
public class ScratchProbeTest extends TestBase {

    @Test
    void probeIndexOfQualifyingDot() {
        assertEquals(1, QueryUtil.indexOfQualifyingDot("t.col"));
        assertEquals(-1, QueryUtil.indexOfQualifyingDot("\"a.b\""));
        assertEquals(7, QueryUtil.indexOfQualifyingDot("U&\"a.b\".c"));
        assertEquals(5, QueryUtil.indexOfQualifyingDot("`a.b`.c"));
        assertEquals(5, QueryUtil.indexOfQualifyingDot("[a.b].c"));
        // doubled delimiters are escapes, not region ends
        assertEquals(-1, QueryUtil.indexOfQualifyingDot("\"a\"\"b.c\""));
        assertEquals(8, QueryUtil.indexOfQualifyingDot("\"a\"\"b.c\".d"));
        // unterminated quote swallows the rest
        assertEquals(-1, QueryUtil.indexOfQualifyingDot("\"a.b"));
        // U& with UESCAPE clause: the escape char and clause are part of the identifier
        assertEquals(-1, QueryUtil.indexOfQualifyingDot("U&\"a\" UESCAPE '.'"));
        assertEquals(17, QueryUtil.indexOfQualifyingDot("U&\"a\" UESCAPE '!'.x"));
    }

    @Test
    void probeBackslashEscapedQuoteEnd() {
        // The result is the closing quote's index, not the position after it.
        assertEquals(2, QueryUtil.backslashEscapedQuoteEnd("'a'", 1, '\''));
        assertEquals(4, QueryUtil.backslashEscapedQuoteEnd("'a''", 1, '\'')); // doubled quote then unterminated -> len
        assertEquals(5, QueryUtil.backslashEscapedQuoteEnd("'a''x'", 1, '\''));
        assertEquals(4, QueryUtil.backslashEscapedQuoteEnd("'a\\''", 1, '\'')); // backslash-escaped quote does not close
        assertEquals(4, QueryUtil.backslashEscapedQuoteEnd("'a\\'", 1, '\'')); // escaped closing quote -> unterminated -> len
    }

    @Test
    void probePlaceholders() {
        assertEquals("", QueryUtil.placeholders(0));
        assertEquals("?", QueryUtil.placeholders(1));
        assertEquals("?, ?, ?", QueryUtil.placeholders(3));
        assertEquals(100, QueryUtil.placeholders(100).split(", ").length);
        assertEquals(31, QueryUtil.placeholders(31).split(", ").length);
    }

    @Test
    void probeTerminateLineCommentAndBlockComment() {
        assertEquals("a = 1", QueryUtil.terminateLineComment("a = 1"));
        assertEquals("a = 1 -- c\n", QueryUtil.terminateLineComment("a = 1 -- c"));
        assertTrue(QueryUtil.hasUnterminatedBlockComment("a /* b"));
        assertFalse(QueryUtil.hasUnterminatedBlockComment("a /* b */"));
        assertFalse(QueryUtil.hasUnterminatedBlockComment("a -- /* b"));
        assertTrue(QueryUtil.hasUnterminatedBlockComment("a --x /* b")); // MySQL: --x is not a comment
    }

    @Test
    void probePositionalParameterOffsets() {
        assertArrayEquals(new int[] {}, QueryUtil.positionalParameterOffsets("SELECT a = 1"));
        assertArrayEquals(new int[] { 12 }, QueryUtil.positionalParameterOffsets("SELECT a =  ?"));
        assertArrayEquals(new int[] {}, QueryUtil.positionalParameterOffsets("SELECT a = '?'"));
        // ParsedSql detects parameters only in recognized data-operation statements, not standalone fragments.
        assertArrayEquals(new int[] {}, QueryUtil.positionalParameterOffsets("a =  ?"));
    }
}
