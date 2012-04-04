// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static org.junit.Assert.assertEquals;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonValue;
import com.amazon.ion.system.IonSystemBuilder;
import org.junit.After;


public class CoreTestCase
{
    private IonSystem mySystem = IonSystemBuilder.standard().build();
    private Environment myEnvironment = new CoreEnvironment(mySystem);
    private Evaluator myEvaluator = new Evaluator(mySystem);

    @After
    public void tearDown()
    {
        mySystem = null;
        myEnvironment = null;
        myEvaluator = null;
    }

    //========================================================================

    protected void assertEval(String expectedIon, String expressionIon)
    {
        IonValue expected   = mySystem.singleValue(expectedIon);
        IonValue expression = mySystem.singleValue(expressionIon);

        IonValue result = myEvaluator.evalToIon(myEnvironment, expression);

        assertEquals(expected, result);
    }

    protected void assertEval(int expectedInt, String expressionIon)
    {
        IonValue expected   = mySystem.newInt(expectedInt);
        IonValue expression = mySystem.singleValue(expressionIon);

        IonValue result = myEvaluator.evalToIon(myEnvironment, expression);

        assertEquals(expected, result);
    }

    protected void assertSelfEval(String expressionIon)
    {
        assertEval(expressionIon, expressionIon);
    }

    protected void eval(String expressionIon)
    {
        IonValue expression = mySystem.singleValue(expressionIon);
        myEvaluator.eval(myEnvironment, expression);
    }
}
