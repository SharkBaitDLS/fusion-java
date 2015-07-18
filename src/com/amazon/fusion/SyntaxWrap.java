// Copyright (c) 2012-2015 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import java.util.Iterator;
import java.util.Set;

/**
 * Linked-list of lexical information wrapped around a {@link SyntaxValue}
 * hierarchy.
 */
abstract class SyntaxWrap
{
    /**
     * @param name must be interned.
     * @param returnMarks <em>returns</em> the marks from this wrap and those
     * deeper. Must be mutable and not null.
     *
     * @return null indicates a free variable.
     */
    abstract Binding resolve(String name,
                             Iterator<SyntaxWrap> moreWraps,
                             Set<Integer> returnMarks);


    /**
     * @param name must be interned.
     */
    Binding resolveTop(String name,
                       Iterator<SyntaxWrap> moreWraps,
                       Set<Integer> returnMarks)
    {
        if (moreWraps.hasNext())
        {
            SyntaxWrap nextWrap = moreWraps.next();
            return nextWrap.resolveTop(name, moreWraps, returnMarks);
        }
        return resolve(name, moreWraps, returnMarks);
    }

    /**
     * Returns an iterator over sub-wraps, if this is a composite, or null
     * otherwise.
     *
     * @return a non-empty iterator, or null.
     */
    abstract Iterator<SyntaxWrap> iterator();
}
