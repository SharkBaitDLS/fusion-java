// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.fusion.ArityFailure.Variability;
import com.amazon.ion.IonSexp;
import com.amazon.ion.IonSymbol;
import com.amazon.ion.IonValue;

/**
 * A user-defined function, the result of evaluating a {@link FuncKeyword}.
 */
final class FuncValue
    extends FunctionValue
{
    private final Environment myEnclosure;
    private final IonSexp myDefinition;

    /**
     * Index within {@link #myDefinition} of the first body form.
     */
    private final int myBodyStart;

    /**
     * Constructs a new function from its source and enclosing lexical
     * environment.
     *
     * @param enclosure the lexical environment surrounding the source of this
     *  function.  Any free variables in the function are expected to be bound
     *  here.
     * @param definition the source text of the {@code func} expression.
     */
    FuncValue(Environment enclosure, IonSexp definition, String doc,
              int bodyStartIndex)
    {
        super(doc, determineParams((IonSexp) definition.get(1)));

        myEnclosure = enclosure;
        myDefinition = definition;
        myBodyStart = bodyStartIndex;
    }

    private static String[] determineParams(IonSexp paramsExpr)
    {
        int size = paramsExpr.size();
        String[] params = new String[size];
        for (int i = 0; i < size; i++)
        {
            IonSymbol param = (IonSymbol) paramsExpr.get(i);
            params[i] = param.stringValue();
        }
        return params;
    }


    @Override
    FusionValue invoke(Evaluator eval, final FusionValue[] args)
        throws FusionException
    {
        final int paramCount = myParams.length;
        if (paramCount != args.length)
        {
            throw new ArityFailure(this, paramCount, Variability.EXACT, args);
        }

        Environment bodyEnv;
        if (paramCount == 0)
        {
            bodyEnv = myEnclosure;
        }
        else
        {
            bodyEnv = new LocalEnvironment(myEnclosure, myParams, args);
        }

        FusionValue result;
        final int bodyEnd = myDefinition.size() - 1;
        for (int i = myBodyStart; i < bodyEnd; i++)
        {
            IonValue expr = myDefinition.get(i);
            result = eval.eval(bodyEnv, expr);
        }

        IonValue expr = myDefinition.get(bodyEnd);
        result = eval.bounceTailExpression(bodyEnv, expr);
        return result;
    }
}
