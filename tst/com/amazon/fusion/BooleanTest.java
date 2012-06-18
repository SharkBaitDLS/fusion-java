// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.TailCallTest.STACK_OVERFLOW_DEPTH;

import org.junit.Ignore;
import org.junit.Test;

public class BooleanTest
    extends CoreTestCase
{
    public static final String[] TRUTHY_EXPRESSIONS =
    {
        "true",
        "((lambda () true))",
    };

    public static final String[] UNTRUTHY_EXPRESSIONS =
    {
        "false",
        "((lambda () false))",
    };

    public static final String[] FAILING_FORMS =
    {
         "null.bool",
         "undef",
         "(lambda () true)",
    };


    @Test @Ignore
    public void testBasicIf()
        throws Exception
    {
        assertEval(1, "(if true 1 2)");
        assertEval(2, "(if false 1 2)");
        assertEval(1, "(if (if false false true) 1 2)");
    }

    @Test
    public void testNotEvaluatingOtherBranch()
        throws Exception
    {
        assertEval(1, "(if true 1 (exit))");
        assertEval(2, "(if false (exit) 2)");
    }

    @Test @Ignore
    public void testTruthiness()
        throws Exception
    {
        eval("(define n null)");
        assertEval(2, "(if null 1 2)");
        assertEval(2, "(if null.bool 1 2)");
        assertEval(2, "(if n 1 2)");
        assertEval(2, "(if [] 1 2)");
        assertEval(2, "(if (lambda (x) true) 1 2)");
    }

    @Test
    public void testTruthyIf()
        throws Exception
    {
        for (String form : TRUTHY_EXPRESSIONS)
        {
            assertEval(1, "(if " + form + " 1 2)");
        }
    }

    @Test
    public void testUntruthyIf()
        throws Exception
    {
        for (String form : UNTRUTHY_EXPRESSIONS)
        {
            assertEval(2, "(if " + form + " 1 2)");
        }
    }

    @Test
    public void testIfArgType()
        throws Exception
    {
        for (String form : FAILING_FORMS)
        {
            String expr = "(if " + form + " 1 2)";
            expectArgTypeFailure(expr, 0);
        }
    }

    @Test
    public void testIfArity()
        throws Exception
    {
        expectSyntaxFailure("(if)");
        expectSyntaxFailure("(if true)");
        expectSyntaxFailure("(if true 1)");
        expectSyntaxFailure("(if true 1 2 3)");
    }


    //========================================================================


    @Test
    public void testAnd()
        throws Exception
    {
        assertEval(true, "(and)");
        assertEval(true, "(and true)");
        assertEval(true, "(and true true)");
        assertEval(true, "(and (= 1 1) true)");

        assertEval(false, "(and false)");
        assertEval(false, "(and true false)");
        assertEval(false, "(and false (exit))");
        assertEval(false, "(and true false true (exit))");
    }


    @Test
    public void testAndArgType()
        throws Exception
    {
        expectArgTypeFailure("(and null)", 0);
        expectArgTypeFailure("(and true null)", 1);
    }


    //========================================================================


    @Test @Ignore
    public void testOr()
        throws Exception
    {
        assertEval(true, "(or true)");
        assertEval(true, "(or true (exit))");
        assertEval(true, "(or false (= 1 1) (exit))");

        assertEval(false, "(or)");
        assertEval(false, "(or false)");
        assertEval(false, "(or false false)");
    }


    @Test
    public void testOrArgType()
        throws Exception
    {
        expectArgTypeFailure("(or null)", 0);
        expectArgTypeFailure("(or false null)", 1);
    }


    @Test @Ignore
    public void testAndOrTailCall()        // TODO FUSION-12 tail optimization
        throws Exception
    {
        assertEval(true,
                   "(letrec ((is_even (lambda (n)" +
                   "                    (or (= 0 n)" +
                   "                      (is_odd (- n 1)))))" +
                   "         (is_odd (lambda (n)" +
                   "                   (and (not (= 0 n))" +
                   "                     (is_even (- n 1))))))" +
                   "  (is_even " + STACK_OVERFLOW_DEPTH + "0))");
    }


    //========================================================================


    @Test
    public void testNot()
        throws Exception
    {
        assertEval(true,  "(not false)");
        assertEval(false, "(not true)");
    }


    @Test
    public void testNotArity()
        throws Exception
    {
        expectArityFailure("(not)");
        expectArityFailure("(not true false)");
    }


    @Test
    public void testNotArgType()
        throws Exception
    {
        for (String form : FAILING_FORMS)
        {
            String expr = "(not " + form + ")";
            expectArgTypeFailure(expr, 0);
        }
    }

    @Test
    public void testEqual()
        throws Exception
    {
        // integer test
        assertEval(true,  "(= 1 1)");
        assertEval(false, "(= 1 2)");

        // boolean test
        assertEval(true,  "(= true true)");
        assertEval(false, "(= false true)");

        // string test
        assertEval(true,  "(= \"hello\" \"hello\")");
        assertEval(false, "(= \"hello\" \"world\")");

        // date-and-time timestamp test
        assertEval(true,  "(= 2007-08-28T16:37:24.0000Z 2007-08-28T16:37:24.0000Z)");
        assertEval(false, "(= 2007-08-29T16:37:24.0000Z 2007-08-28T16:37:24.0000Z)");

        // date-only timestamp test
        assertEval(true,  "(= 2008-08-28 2008-08-28)");
        assertEval(false, "(= 2008-08-28 2007-08-28)");

        // mixed-state timestamp test
        assertEval(false, "(= 2008-08-28 2007-08-29T16:37:24.0000Z)");
        assertEval(true, "(= 2007-08-28T16:37:24.0000Z 2007-08-28T16:37:24Z)");
        assertEval(true, "(= 2008-08-28 2008-08-28T00:00:00.0000Z)");
        assertEval(true, "(= 2007-08-28 2007-08-28T00:00Z)");

    }

    @Test
    public void testLessThan()
        throws Exception
    {
        assertEval(false, "(< 2 1)");
        assertEval(false, "(< 1 1)");
        assertEval(true,  "(< 1 2)");

        // date-only timestamp test
        assertEval(true,  "(< 2008-08-28 2008-08-29)");
        assertEval(false, "(< 2008-08-29 2008-08-28)");
        assertEval(false, "(< 2008-08-29 2008-08-29)");

        // date-and-time timestamp test
        assertEval(true,  "(< 2007-08-28T16:37:24.0000Z 2007-08-28T16:37:25.0000Z)");
        assertEval(false, "(< 2007-08-28T16:37:24.0000Z 2007-08-28T16:37:24.0000Z)");
        assertEval(false, "(< 2007-08-28T16:37:25.0000Z 2007-08-28T16:37:24.0000Z)");

        // mixed-state timestamp test
        assertEval(true,  "(< 2008-08-28 2008-08-28T16:37:24.0000Z)");
        assertEval(false, "(< 2007-08-28 2007-08-28T00:00Z)");
        assertEval(false, "(< 2007-08-29 2007-08-28T00:00Z)");
    }

    @Test
    public void testGreaterThan()
            throws Exception
    {
        assertEval(true, "(> 2 1)");
        assertEval(false, "(> 1 1)");
        assertEval(false,  "(> 1 2)");

        // date-only timestamp test
        assertEval(false,  "(> 2008-08-28 2008-08-29)");
        assertEval(true, "(> 2008-08-29 2008-08-28)");
        assertEval(false, "(> 2008-08-29 2008-08-29)");

        // date-and-time timestamp test
        assertEval(false,  "(> 2007-08-28T16:37:24.0000Z 2007-08-28T16:37:25.0000Z)");
        assertEval(false, "(> 2007-08-28T16:37:24.0000Z 2007-08-28T16:37:24.0000Z)");
        assertEval(true, "(> 2007-08-28T16:37:25.0000Z 2007-08-28T16:37:24.0000Z)");

        // mixed-state timestamp test
        assertEval(false,  "(> 2008-08-28 2008-08-28T16:37:24.0000Z)");
        assertEval(false, "(> 2007-08-28 2007-08-28T00:00Z)");
        assertEval(true, "(> 2007-08-29 2007-08-28T00:00Z)");
    }

    @Test
    public void testComparisonFail()
        throws Exception
    {
        String [] ops = {"<", "=", ">"};

        for (int i = 0; i < ops.length; i++)
        {
            expectContractFailure("("+ops[i]+" 1 true)");
            expectContractFailure("("+ops[i]+" 1 \"hello\")");
            expectContractFailure("("+ops[i]+" 1 2008-08-28)");
            expectContractFailure("("+ops[i]+" 1 2007-08-28T16:37:24.0000Z)");
            expectContractFailure("("+ops[i]+" 1 undef)");

            expectContractFailure("("+ops[i]+" true 1)");
            expectContractFailure("("+ops[i]+" true \"hello\")");
            expectContractFailure("("+ops[i]+" true 2008-08-28)");
            expectContractFailure("("+ops[i]+" true 2007-08-28T16:37:24.0000Z)");
            expectContractFailure("("+ops[i]+" true undef)");

            expectContractFailure("("+ops[i]+" \"hello\" 1)");
            expectContractFailure("("+ops[i]+" \"hello\" true)");
            expectContractFailure("("+ops[i]+" \"hello\" 2008-08-28)");
            expectContractFailure("("+ops[i]+" \"hello\" 2007-08-28T16:37:24.0000Z)");
            expectContractFailure("("+ops[i]+" \"hello\" undef)");

            expectContractFailure("("+ops[i]+" 2008-08-28 1)");
            expectContractFailure("("+ops[i]+" 2008-08-28 \"hello\")");
            expectContractFailure("("+ops[i]+" 2008-08-28 true)");
            expectContractFailure("("+ops[i]+" 2008-08-28 undef)");

            expectContractFailure("("+ops[i]+" 2007-08-28T16:37:24.0000Z true)");
            expectContractFailure("("+ops[i]+" 2007-08-28T16:37:24.0000Z 1)");
            expectContractFailure("("+ops[i]+" 2007-08-28T16:37:24.0000Z \"hello\")");
            expectContractFailure("("+ops[i]+" 2008-08-28T16:37:24.0000Z  undef)");
        }
    }

    @Test
    public void testUnless()
        throws Exception
    {
        assertUndef("(unless true 1)");
        assertEval(1, "(unless false 1)");

        assertEval(3, "(unless false (begin 3))");

        assertEval(3, "(unless false 1 2 3)");
    }

    public void testWhen()
         throws Exception
    {
        assertUndef("(when false 1)");
        assertEval(1,"(when true 1)");

        assertEval(3, "(when true (begin 3))");

        assertEval(3, "(when true 1 2 3)");
    }

}
