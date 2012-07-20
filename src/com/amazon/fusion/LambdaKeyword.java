// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

/**
 * The {@code lambda} syntactic form, which evaluates to a {@link Closure}.
 */
final class LambdaKeyword
    extends KeywordValue
{
    LambdaKeyword()
    {
        //    "                                                                               |
        super("(PARAM ...) DOC? BODY",
              "Returns a new procedure. When invoked, the caller's arguments are bound to the\n" +
              "PARAMs and the BODY is evaluated and returned.\n" +
              "DOC is an optional documentation string.\n" +
              "BODY may be one or more forms; the result of the last form is the result of the\n" +
              "procedure invocation.");
    }

    @Override
    FusionValue invoke(Evaluator eval, Environment env, SyntaxSexp expr)
    {
        String doc;
        int bodyStart;

        SyntaxValue maybeDoc = expr.get(2);
        if (maybeDoc.getType() == SyntaxValue.Type.STRING
            && expr.size() > 3)
        {
            doc = ((SyntaxString) maybeDoc).stringValue();
            if (doc != null) doc = doc.trim();
            bodyStart = 3;
        }
        else
        {
            doc = null;
            bodyStart = 2;
        }

        return new Closure(env, expr, doc, bodyStart);
    }
}
