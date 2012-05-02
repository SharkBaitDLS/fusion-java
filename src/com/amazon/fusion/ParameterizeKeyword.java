// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.ion.IonSequence;
import com.amazon.ion.IonSexp;
import com.amazon.ion.IonValue;

/**
 *
 */
final class ParameterizeKeyword
    extends KeywordValue
{
    ParameterizeKeyword()
    {
        //    "                                                                               |
        super("((PARAM EXPR) ...) BODY ...+",
              "Dynamically binds the PARAMs to the EXPR values while evaluating the BODY.\n" +
              "The PARAMs are evaluated first, in order; each must result in a dynamic\n" +
              "parameter procedure. The EXPRs are then evaluated in order, and then the params\n" +
              "are changed to their results for the dynamic extent of the BODY.\n" +
              "BODY may be one or more forms; the result of the last form is the result of the\n" +
              "entire expression.");
    }


    @Override
    FusionValue invoke(Evaluator eval, Environment env, IonSexp expr)
        throws FusionException
    {
        final int letrecExprSize = expr.size();
        if (letrecExprSize < 3)
        {
            throw new SyntaxFailure(getEffectiveName(), "", expr);
        }

        IonSequence bindingForms =
            requiredSequence("sequence of parameterizations", 1, expr);

        final int numBindings = bindingForms.size();
        DynamicParameter[] parameters = new DynamicParameter[numBindings];
        IonValue[] boundExprs = new IonValue[numBindings];
        for (int i = 0; i < numBindings; i++)
        {
            IonSexp binding =
                requiredSexp("parameter/value binding", i, bindingForms);
            IonValue paramExpr = requiredForm("parameter/value binding", 0, binding);

            FusionValue paramValue = eval.eval(env, paramExpr);
            // TODO error handling
            parameters[i] = (DynamicParameter) paramValue;
            boundExprs[i] = requiredForm("parameter/value binding", 1, binding);
        }

        FusionValue[] boundValues = new FusionValue[numBindings];
        for (int i = 0; i < numBindings; i++)
        {
            IonValue boundExpr = boundExprs[i];
            FusionValue boundValue = eval.eval(env, boundExpr);
            boundValues[i] = boundValue;
        }

        Evaluator bodyEval = eval.markedContinuation(parameters, boundValues);

        // TODO tail recursion
        FusionValue result = null;
        final int bodyEnd = letrecExprSize/* - 1*/;
        for (int i = 2; i < bodyEnd; i++)
        {
            IonValue bodyExpr = expr.get(i);
            result = bodyEval.eval(env, bodyExpr);
        }
/*
        IonValue bodyExpr = expr.get(bodyEnd);
        result = eval.bounceTailExpression(env, bodyExpr);
        */
        return result;
    }
}
