// Copyright (c) 2012-2014 Amazon.com, Inc. All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionBool.falseBool;
import static com.amazon.fusion.FusionBool.makeBool;
import static com.amazon.fusion.FusionIo.safeWriteToString;
import static com.amazon.fusion.FusionUtils.EMPTY_STRING_ARRAY;
import com.amazon.fusion.FusionBool.BaseBool;
import com.amazon.ion.IonException;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import com.amazon.ion.system.IonTextWriterBuilder;
import com.amazon.ion.util.IonTextUtils;
import java.io.IOException;

/**
 * Root class for most (if not all) Fusion values.
 */
abstract class BaseValue
{
    BaseValue() {}


    String[] annotationsAsJavaStrings()
    {
        return EMPTY_STRING_ARRAY;
    }


    boolean isAnyNull()
    {
        return false;
    }


    BaseBool isTruthy(Evaluator eval)
    {
        return makeBool(eval, ! isAnyNull());
    }


    BaseBool not(Evaluator eval)
    {
        return makeBool(eval, isAnyNull());
    }


    BaseBool looseEquals(Evaluator eval, Object right)
        throws FusionException
    {
        return falseBool(eval);
    }

    /**
     * @return null if something in the datum can't be converted into syntax.
     */
    SyntaxValue toStrippedSyntaxMaybe(Evaluator eval)
        throws FusionException
    {
        return null;
    }


    /**
     * Returns the documentation of this value.
     * <p>
     * <b>Implementations are expected to return the same object instance on
     * every call, in order to preserve proper documentation indexing.</b>
     *
     * @return the documentation model, or null if there's no documentation.
     */
    BindingDoc document()
    {
        return null;
    }


    /**
     * Writes an Ion representation of a value.
     * An exception is thrown if the value contains any non-Ion data
     * like closures.
     *
     * @param eval may be null, in which case output may fall back to default
     * format of some kind.
     * @param out the output stream; not null.
     *
     * @throws IOException Propagated from the output stream.
     * @throws IonizeFailure if the data cannot be ionized.
     */
    void ionize(Evaluator eval, IonWriter out)
        throws IOException, IonException, FusionException, IonizeFailure
    {
        throw new IonizeFailure(this);
    }


    /**
     * Writes a representation of this value, following Ion syntax where
     * possible.
     * <p>
     * Most code shouldn't call this method, and should prefer
     * {@link FusionIo#write(Evaluator, Appendable, Object)}.
     *
     * @param eval may be null!
     * @param out the output stream; not null.
     *
     * @throws IOException Propagated from the output stream.
     * @throws FusionException
     */
    abstract void write(Evaluator eval, Appendable out)
        throws IOException, FusionException;


    /**
     * Builder for temporary IonWriters needed for {@link #write}ing scalars.
     * <p>
     * TODO WORKAROUND ION-398
     * <p>
     * TODO FUSION-247 Write output without building an IonWriter.
     *
     * @deprecated Try to avoid this.
     */
    @Deprecated
    static final IonTextWriterBuilder WRITER_BUILDER =
        IonTextWriterBuilder.minimal().immutable();


    /** Helper method for subclasses. */
    static void writeAnnotations(Appendable out, String[] annotations)
        throws IOException
    {
        for (String ann : annotations)
        {
            IonTextUtils.printSymbol(out, ann);
            out.append("::");
        }
    }


    /**
     * Prints a representation of this value for human consumption, generally
     * translating character/string data to it's content without using Ion
     * quotes or escapes. Non-character data is output as per
     * {@link #write(Evaluator, Appendable)}.
     *
     * @param out the output stream; not null.
     *
     * @throws IOException Propagated from the output stream.
     */
    void display(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        write(eval, out);
    }


    /**
     * Returns a representation of this value for debugging and diagnostics.
     * Currently, it behaves like {@link FusionIo#write} but the behavior may
     * change at any time.
     */
    @Override
    public final String toString()
    {
        return safeWriteToString((Evaluator) null, this);
    }


    /**
     * @throws IonizeFailure (when {@code throwOnConversionFailure})
     * if this value cannot be ionized.
     */
    IonValue copyToIonValue(ValueFactory factory,
                            boolean throwOnConversionFailure)
        throws FusionException, IonizeFailure
    {
        if (throwOnConversionFailure)
        {
            throw new IonizeFailure(this);
        }

        return null;
    }


    //========================================================================
    // Helper methods to avoid name conflicts with FusionValue imports


    static BaseBool isAnyNull(Evaluator eval, Object value)
        throws FusionException
    {
        boolean r = FusionValue.isAnyNull(eval, value);
        return makeBool(eval, r);
    }


    static BaseBool isTruthy(Evaluator eval, Object value)
        throws FusionException
    {
        return FusionValue.isTruthy(eval, value);
    }


    static BaseBool not(Evaluator eval, Object value)
        throws FusionException
    {
        return FusionValue.not(eval, value);
    }


    static BaseBool looseEquals(Evaluator eval, Object left, Object right)
        throws FusionException
    {
        // TODO This would be a valuable shortcut, but it goes beyond the
        //   current specification.
//      if (arg0 == arg1) return trueBool(eval);

        if (left instanceof BaseValue)
        {
            return ((BaseValue) left).looseEquals(eval, right);
        }

        return falseBool(eval);
    }

    /**
     * Gets the annotations on a Fusion value as Java strings.
     *
     * @return not null, but possibly empty.
     */
    static String[] annotationsAsJavaStrings(Evaluator eval, Object value)
        throws FusionException
    {
        return FusionValue.annotationsAsJavaStrings(eval, value);
    }


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
     * @see FusionValue#copyToIonValueMaybe(Object, ValueFactory)
     */
    static IonValue copyToIonValueMaybe(Object value, ValueFactory factory)
        throws FusionException
    {
        return FusionValue.copyToIonValue(value, factory, false);
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
     *
     * @see FusionValue#copyToIonValue(Object, ValueFactory)
     */
    static IonValue copyToIonValue(Object value, ValueFactory factory)
        throws FusionException
    {
        return FusionValue.copyToIonValue(value, factory, true);
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
     * @see FusionValue#copyToIonValue(Object, ValueFactory, boolean)
     */
    static IonValue copyToIonValue(Object value, ValueFactory factory,
                                   boolean throwOnConversionFailure)
        throws FusionException, IonizeFailure
    {
        return FusionValue.copyToIonValue(value,
                                          factory,
                                          throwOnConversionFailure);
    }
}
