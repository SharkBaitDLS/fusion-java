// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.BindingDoc.COLLECT_DOCS_MARK;
import static com.amazon.fusion.FusionVoid.voidValue;
import static com.amazon.fusion.FusionWrite.safeWriteToString;
import static com.amazon.ion.util.IonTextUtils.printQuotedSymbol;
import static java.lang.Boolean.TRUE;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonSystem;
import java.io.File;


final class StandardTopLevel
    implements TopLevel
{
    private final Evaluator myEvaluator;
    private final Namespace myNamespace;


    /**
     * @param initialModulePath must be absolute.
     */
    StandardTopLevel(GlobalState globalState,
                     Namespace namespace,
                     String initialModulePath,
                     boolean documenting)
        throws FusionException
    {
        assert initialModulePath.startsWith("/");

        Evaluator eval = new Evaluator(globalState);
        if (documenting)
        {
            eval = eval.markedContinuation(COLLECT_DOCS_MARK, TRUE);
        }

        myEvaluator = eval;
        myNamespace = namespace;
        namespace.use(myEvaluator, initialModulePath);
    }

    /**
     * @param initialModulePath must be absolute.
     */
    StandardTopLevel(GlobalState globalState,
                     ModuleRegistry registry,
                     String initialModulePath)
        throws FusionException
    {
        this(globalState, new Namespace(registry), initialModulePath, false);
    }


    //========================================================================

    // NOT PUBLIC
    Evaluator getEvaluator()
    {
        return myEvaluator;
    }


    @Override
    public Object eval(String source, SourceName name)
        throws ExitException, FusionException
    {
        IonSystem system = myEvaluator.getGlobalState().myIonSystem;
        IonReader i = system.newReader(source);
        return eval(i, name);
    }


    @Override
    public Object eval(String source)
        throws ExitException, FusionException
    {
        return eval(source, null);
    }


    @Override
    public Object eval(IonReader source, SourceName name)
        throws ExitException, FusionException
    {
        Object result = voidValue(myEvaluator);

        // TODO should work even if already positioned on first value

        while (source.next() != null)
        {
            SyntaxValue sourceExpr = Syntax.read(myEvaluator, source, name);
            result = FusionEval.eval(myEvaluator, sourceExpr, myNamespace);
        }

        return result;
    }


    @Override
    public Object eval(IonReader source)
        throws ExitException, FusionException
    {
        return eval(source, null);
    }


    @Override
    public Object load(File source)
        throws ExitException, FusionException
    {
        LoadHandler load = myEvaluator.getGlobalState().myLoadHandler;
        return load.loadTopLevel(myEvaluator, myNamespace, source.toString());
    }


    @Override
    public void requireModule(String moduleIdentifier)
        throws FusionException
    {
        // TODO FUSION-74 don't do this conversion!
        if (moduleIdentifier.startsWith("/"))
        {
            moduleIdentifier = moduleIdentifier.substring(1);
        }
        myNamespace.use(myEvaluator, moduleIdentifier);
    }


    @Override
    public void define(String name, Object value)
    {
        myNamespace.bind(name, value);
    }


    private Procedure lookupProcedure(String procedureName)
        throws FusionException
    {
        SyntaxSymbol id = SyntaxSymbol.make(procedureName);

        Object proc = FusionEval.eval(myEvaluator, id, myNamespace);
        if (proc instanceof Procedure)
        {
            return (Procedure) proc;
        }

        if (proc == null)
        {
            throw new FusionException(printQuotedSymbol(procedureName) +
                                      " is not defined");
        }

        throw new FusionException(printQuotedSymbol(procedureName) +
                                  " is not a procedure: " +
                                  safeWriteToString(myEvaluator, proc));
    }

    @Override
    public Object call(String procedureName, Object... arguments)
        throws FusionException
    {
        Procedure proc = lookupProcedure(procedureName);

        for (int i = 0; i < arguments.length; i++)
        {
            Object arg = arguments[i];
            arg = myEvaluator.injectMaybe(arg);
            if (arg == null)
            {
                throw new ArgTypeFailure("TopLevel.call",
                                         "injectable Java type",
                                         i, arguments[i]);
            }
            arguments[i] = arg;
        }

        return myEvaluator.callNonTail(proc, arguments);
    }
}
