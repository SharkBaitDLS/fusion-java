// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionUtils.EMPTY_STRING_ARRAY;
import java.io.File;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


final class ModuleDoc
{
    private final FusionRuntime myRuntime;
    final ModuleIdentity myModuleId;
    final String myIntroDocs;

    private Map<String,ModuleDoc>  mySubmodules;
    private Map<String,BindingDoc> myBindings;


    private static ModuleIdentity resolveModulePath(FusionRuntime runtime,
                                                    String modulePath)
        throws FusionException
    {
        assert modulePath.startsWith("/");

        StandardTopLevel top = (StandardTopLevel) runtime.getDefaultTopLevel();
        Evaluator eval = top.getEvaluator();
        ModuleNameResolver resolver =
            eval.getGlobalState().myModuleNameResolver;

        return resolver.resolveLib(eval, modulePath, null);
    }


    public static ModuleDoc buildDocTree(FusionRuntime runtime, Filter filter,
                                         File repoDir)
        throws IOException, FusionException
    {
        ModuleDoc doc = new ModuleDoc(runtime);
        buildTree(filter, repoDir, doc);
        return doc;
    }


    private static void buildTree(Filter filter, File dir, ModuleDoc doc)
        throws IOException, FusionException
    {
        String[] fileNames = dir.list();

        // First pass: build all "real" modules
        for (String fileName : fileNames)
        {
            if (fileName.endsWith(".ion"))
            {
                // We assume that all .ion files are modules.
                String moduleName =
                    fileName.substring(0, fileName.length() - 4);
                doc.addSubmodule(filter, moduleName);
            }
        }

        // Second pass: look for directories, which are implicitly submodules.
        for (String fileName : fileNames)
        {
            File testFile = new File(dir, fileName);
            if (testFile.isDirectory())
            {
                ModuleDoc d = doc.addImplicitSubmodule(filter, fileName);
                if (d != null)
                {
                    buildTree(filter, testFile, d);
                }
            }
        }
    }


    //========================================================================


    /**
     * Constructs the documentation root as a pseudo-module.
     */
    private ModuleDoc(FusionRuntime runtime)
        throws FusionException
    {
        myRuntime = runtime;
        myModuleId = null;
        myIntroDocs = null;
    }


    /**
     * Constructs docs for a real or implicit top-level module or submodule.
     */
    private ModuleDoc(FusionRuntime runtime, ModuleIdentity id)
        throws FusionException
    {
        myRuntime = runtime;
        myModuleId = id;

        StandardRuntime rt = (StandardRuntime) runtime;
        ModuleInstance moduleInstance = rt.getDefaultRegistry().lookup(id);

        myIntroDocs = moduleInstance.getDocs();

        build(moduleInstance);
    }


    String baseName()
    {
        return (myModuleId == null ? null : myModuleId.baseName());
    }


    String submodulePath(String name)
    {
        if (myModuleId == null)
        {
            return "/" + name;
        }

        String parentPath = myModuleId.internString();
        assert parentPath.startsWith("/");

        return parentPath + "/" + name;
    }


    String oneLiner()
    {
        if (myIntroDocs == null) return null;

        // TODO pick a better locale?
        BreakIterator breaks = BreakIterator.getSentenceInstance();
        breaks.setText(myIntroDocs);
        int start = breaks.first();
        int end = breaks.next();
        if (end == BreakIterator.DONE) return null;

        return myIntroDocs.substring(start, end);
    }


    Map<String, BindingDoc> bindingMap()
    {
        return myBindings;
    }

    String[] sortedExportedNames()
    {
        String[] names = EMPTY_STRING_ARRAY;
        if (myBindings != null)
        {
            names = myBindings.keySet().toArray(EMPTY_STRING_ARRAY);
            Arrays.sort(names);
        }
        return names;
    }

    Map<String, ModuleDoc> submoduleMap()
    {
        return mySubmodules;
    }

    Collection<ModuleDoc> submodules()
    {
        if (mySubmodules == null)
        {
            return Collections.emptySet();
        }
        return mySubmodules.values();
    }


    private void build(ModuleInstance module)
    {
        Set<String> names = module.providedNames();
        if (names.size() == 0) return;

        myBindings = new HashMap<String,BindingDoc>(names.size());

        for (String name : names)
        {
            BindingDoc doc = module.documentProvidedName(name);
            myBindings.put(name, doc);
        }
    }


    /**
     * @return null if the submodule is to be excluded from documentation.
     */
    private ModuleDoc addSubmodule(Filter filter, String name)
        throws FusionException
    {
        ModuleIdentity id;
        try
        {
            id = resolveModulePath(myRuntime, submodulePath(name));
            assert id.baseName().equals(name);
        }
        catch (ModuleNotFoundFailure e)
        {
            // This can happen for implicit modules with no stub .ion file.
            // For now we just stop here and don't handle further submodules.
            // TODO Recurse into implicit modules that don't have stub source.
            return null;
        }

        if (! filter.accept(id)) return null;


        ModuleDoc doc = new ModuleDoc(myRuntime, id);

        if (mySubmodules == null)
        {
            mySubmodules = new HashMap<String,ModuleDoc>();
        }

        assert ! mySubmodules.containsKey(name);
        mySubmodules.put(name, doc);

        return doc;
    }


    /**
     * Adds a submodule doc if and only if it doesn't already exist.
     * @return null if the submodule is to be excluded from documentation.
     */
    private ModuleDoc addImplicitSubmodule(Filter filter, String name)
        throws FusionException
    {
        if (mySubmodules != null)
        {
            ModuleDoc doc = mySubmodules.get(name);

            if (doc != null) return doc;
        }

        return addSubmodule(filter, name);
    }


    //========================================================================


    static final class Filter
    {
        boolean accept(ModuleIdentity id)
        {
            String name = id.internString();
            if (name.startsWith("#%")) return false;
            if (name.endsWith("/private")) return false;
            if (name.contains("/private/")) return false;
            return true;
        }
    }
}
