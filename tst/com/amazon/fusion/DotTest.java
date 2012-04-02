// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import org.junit.Test;


public class DotTest
    extends CoreTestCase
{
    @Test
    public void testNoParts()
    {
        assertEval("3", "(. 3)");
        assertEval("null", "(. null.null)");
        assertEval("[]", "(. [])");
        assertEval("{}", "(.{})");
        assertEval("{f:true}", "(. {f:true})");
    }

    @Test
    public void testStructParts()
    {
        assertEval("true", "(. {f:true} \"f\")");
        assertEval("\"oy\"", "(. {f:{g:'''oy'''}, h:true} \"f\" \"g\")");
    }

    @Test
    public void testSequenceParts()
    {
        assertEval("99", "(. [99, \"hello\"] 0)");
        assertEval("{{}}", "(. [99, [true, {{}}]] 1 1)");
    }
}
