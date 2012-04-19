// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.ion.IonSexp;
import com.amazon.ion.util.IonTextUtils;
import java.io.IOException;
import java.io.Writer;

/**
 * Base class for syntactic forms
 * (as opposed to {@linkplain FunctionValue functions}).
 */
abstract class KeywordValue
    extends NamedValue
{
    protected final String myBodyPattern;
    protected final String myDoc;

    KeywordValue(String bodyPattern, String doc)
    {
        myBodyPattern = bodyPattern;
        myDoc = doc;
    }

    @Override
    abstract FusionValue invoke(Evaluator eval,
                                Environment env,
                                IonSexp expr)
        throws FusionException;

    @Override
    final void display(Writer out)
        throws IOException
    {
        out.write("// Keyword ");
        try
        {
            IonTextUtils.printQuotedSymbol(out, getEffectiveName());
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Shouldn't happen", e);
        }
        out.write('\n');
    }

    @Override
    void printHelp(Writer out)
        throws IOException
    {
        out.write("[SYNTAX]  (");
        out.write(getEffectiveName());
        if (myBodyPattern != null)
        {
            out.write(' ');
            out.write(myBodyPattern);
        }
        out.write(")\n\n");
        out.write(myDoc);
        out.write('\n');
    }
}
