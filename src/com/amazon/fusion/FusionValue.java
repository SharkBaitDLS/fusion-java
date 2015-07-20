// Copyright (c) 2012-2015 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionBool.falseBool;
import static com.amazon.fusion.FusionUtils.EMPTY_STRING_ARRAY;
import com.amazon.fusion.FusionBool.BaseBool;
import com.amazon.ion.IonValue;
import com.amazon.ion.ValueFactory;
import java.io.IOException;

/**
 * Utilities for working with arbitrary Fusion runtime values.
 * Note that the set of Fusion values is a superset of the Ion values, so not
 * all of them are representable as Ion data values.
 */
public final class FusionValue
{
    private FusionValue() {}


    private static final class Undef
        extends BaseValue
    {
        @Override
        void write(Evaluator eval, Appendable out) throws IOException
        {
            out.append("{{{undefined}}}");
        }
    }


    /** The singular {@code undef} value. */
    static final BaseValue UNDEF = new Undef();


    //========================================================================


    /**
     * Determines whether a Fusion value is a null of an type; that is, is it
     * {@code null.null}, {@code null.bool}, {@code null.int}, <em>etc.</em>?
     *
     * @see FusionNull#isNullNull(Evaluator, Object)
     */
    public static boolean isAnyNull(TopLevel top, Object value)
        throws FusionException
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        return isAnyNull(eval, value);
    }

    static boolean isAnyNull(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseValue)
        {
            return ((BaseValue) value).isAnyNull();
        }

        return false;
    }


    /**
     * Determines whether a given Fusion value is "truthy".
     * Fusion defines truthiness as follows:
     * <ul>
     *   <li>
     *     Every value is truthy except for {@code false}, void, and any
     *     variant of {@code null}.
     *   </li>
     * </ul>
     * This definition is more lax (and hopefully more convenient) than Java,
     * but less lenient (and hopefully less error-prone) than C or C++.
     *
     * @see <a href="{@docRoot}/../nullvoid.html">Null and Void</a>
     * @see FusionBool#isTrue(TopLevel, Object)
     *
     * @deprecated As of R15 in March 2014.
     * Moved to {@link FusionBool#isTruthy(TopLevel, Object)}.
     */
    @Deprecated
    public static boolean isTruthy(TopLevel top, Object value)
        throws FusionException
    {
        return FusionBool.isTruthy(top, value);
    }

    /**
     * @deprecated As of R15 in March 2014.
     * Moved to {@link FusionBool#isTruthy(Evaluator, Object)}.
     */
    @Deprecated
    static BaseBool isTruthy(Evaluator eval, Object value)
        throws FusionException
    {
        return FusionBool.isTruthy(eval, value);
    }


    static BaseBool not(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseValue)
        {
            return ((BaseValue) value).not(eval);
        }

        return falseBool(eval);
    }


    /**
     * Determines whether a Fusion value has any annotations.
     */
    static boolean isAnnotated(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof Annotated)
        {
            // TODO inefficient
            return ((Annotated) value).annotationsAsJavaStrings().length != 0;
        }
        return false;
    }


    /**
     * Gets the annotations on a Fusion value as Java strings.
     *
     * @return not null, but possibly empty.
     */
    static String[] annotationsAsJavaStrings(Evaluator eval, Object value)
        throws FusionException
    {
        String[] anns;

        if (value instanceof Annotated)
        {
            anns = ((Annotated) value).annotationsAsJavaStrings();
        }
        else
        {
            anns = EMPTY_STRING_ARRAY;
        }

        return anns;
    }


    //========================================================================
    // Output methods




    //========================================================================
    // Static IonValue methods


    /**
     * Returns a new {@link IonValue} representation of a Fusion value,
     * if its type falls within the Ion type system.
     * The {@link IonValue} will use the given factory and will not have a
     * container.
     *
     * @param factory must not be null.
     *
     * @return a fresh instance, without a container, or null if the value is
     * not handled by the default ionization strategy.
     *
     * @throws FusionException if something goes wrong during ionization.
     *
     * @see FusionRuntime#ionizeMaybe(Object, ValueFactory)
     */
    static IonValue copyToIonValueMaybe(Object value, ValueFactory factory)
        throws FusionException
    {
        return copyToIonValue(value, factory, false);
    }


    /**
     * Returns a new {@link IonValue} representation of a Fusion value,
     * if its type falls within the Ion type system.
     * The {@link IonValue} will use the given factory and will not have a
     * container.
     *
     * @param factory must not be null.
     *
     * @throws FusionException if the value cannot be converted to Ion.
     */
    static IonValue copyToIonValue(Object value, ValueFactory factory)
        throws FusionException
    {
        return copyToIonValue(value, factory, true);
    }


    /**
     * Returns a new {@link IonValue} representation of a Fusion value,
     * if its type falls within the Ion type system.
     * The {@link IonValue} will use the given factory and will not have a
     * container.
     *
     * @param value may be an {@link IonValue}, in which case it is cloned.
     * @param factory must not be null.
     *
     * @throws FusionException if the value cannot be converted to Ion.
     *
     * @see FusionRuntime#ionize(Object, ValueFactory)
     */
    static IonValue copyToIonValue(Object       value,
                                   ValueFactory factory,
                                   boolean      throwOnConversionFailure)
        throws FusionException, IonizeFailure
    {
        if (value instanceof BaseValue)
        {
            BaseValue fv = (BaseValue) value;
            return fv.copyToIonValue(factory, throwOnConversionFailure);
        }

        if (throwOnConversionFailure)
        {
            throw new IonizeFailure(value);
        }

        return null;
    }
}
