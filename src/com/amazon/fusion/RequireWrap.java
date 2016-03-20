// Copyright (c) 2012-2016 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.fusion.FusionSymbol.BaseSymbol;
import com.amazon.fusion.ModuleNamespace.ModuleBinding;
import java.util.Iterator;
import java.util.Set;

/**
 * Syntax wrap that adds all exported bindings from a specific module.
 * It's added to syntax in the scope of a {@code require} form.
 */
class RequireWrap
    extends SyntaxWrap
{
    private final ModuleInstance myModule;

    RequireWrap(ModuleInstance module)
    {
        assert module != null;
        myModule = module;
    }

    ModuleBinding localResolveMaybe(BaseSymbol name)
    {
        return myModule.resolveProvidedName(name);
    }

    @Override
    Binding resolve(BaseSymbol name,
                    Iterator<SyntaxWrap> moreWraps,
                    Set<Integer> returnMarks)
    {
        // TODO FUSION-117 Resolve the whole identifier, including marks.
        Binding local = myModule.resolveProvidedName(name);
        if (local != null) return local;

        if (moreWraps.hasNext())
        {
            SyntaxWrap nextWrap = moreWraps.next();
            return nextWrap.resolve(name, moreWraps, returnMarks);
        }

        return null;
    }


    @Override
    Iterator<SyntaxWrap> iterator()
    {
        return null;
    }


    @Override
    public String toString()
    {
        String id = myModule.getIdentity().absolutePath();
        return "{{{Required module " + id + "}}}";
    }
}
