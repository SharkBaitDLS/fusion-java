// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionValue.UNDEF;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonType;

/**
 * The core set of objects from that are needed by other parts of the
 * implementation.
 */
final class GlobalState
{
    static final String KERNEL_MODULE_NAME = "/fusion/private/kernel";
    static final ModuleIdentity KERNEL_MODULE_IDENTITY =
        ModuleIdentity.internBuiltinName(KERNEL_MODULE_NAME);

    static final String BEGIN         = "begin";
    static final String DEFINE        = "define";
    static final String DEFINE_SYNTAX = "define_syntax";
    static final String LAMBDA        = "lambda";
    static final String LETREC        = "letrec";
    static final String MODULE        = "module";
    static final String REQUIRE       = "require";
    static final String USE           = "use";

    final IonSystem          myIonSystem;
    final ModuleInstance     myKernelModule;
    final ModuleNameResolver myModuleNameResolver;
    final LoadHandler        myLoadHandler;
    final UseForm            myUseForm;
    final DynamicParameter   myCurrentNamespaceParam;

    final SyntaxSymbol       myKernelBeginIdentifier;
    final SyntaxSymbol       myKernelLambdaIdentifier;
    final SyntaxSymbol       myKernelLetrecIdentifier;
    final SyntaxSymbol       myKernelModuleIdentifier;

    final Binding myKernelBeginBinding;
    final Binding myKernelDefineBinding;
    final Binding myKernelDefineSyntaxBinding;
    final Binding myKernelRequireBinding;
    final Binding myKernelUseBinding;

    private GlobalState(IonSystem          ionSystem,
                        ModuleInstance     kernel,
                        ModuleNameResolver resolver,
                        LoadHandler        loadHandler,
                        UseForm            useForm,
                        DynamicParameter   currentNamespaceParam)
    {
        myIonSystem             = ionSystem;
        myKernelModule          = kernel;
        myModuleNameResolver    = resolver;
        myLoadHandler           = loadHandler;
        myUseForm               = useForm;
        myCurrentNamespaceParam = currentNamespaceParam;

        SyntaxWrap wrap = new ModuleRenameWrap(kernel);
        myKernelBeginIdentifier  = SyntaxSymbol.make(BEGIN, wrap);
        myKernelLambdaIdentifier = SyntaxSymbol.make(LAMBDA, wrap);
        myKernelLetrecIdentifier = SyntaxSymbol.make(LETREC, wrap);
        myKernelModuleIdentifier = SyntaxSymbol.make(MODULE, wrap);

        myKernelBeginBinding        = kernelBinding(BEGIN);
        myKernelDefineBinding       = kernelBinding(DEFINE);
        myKernelDefineSyntaxBinding = kernelBinding(DEFINE_SYNTAX);
        myKernelRequireBinding      = kernelBinding(REQUIRE);
        myKernelUseBinding          = kernelBinding(USE);
    }


    static GlobalState initialize(IonSystem system,
                                  FusionRuntimeBuilder builder,
                                  ModuleRegistry registry,
                                  Namespace initialCurrentNamespace)
        throws FusionException
    {
        ModuleBuilderImpl ns =
            new ModuleBuilderImpl(registry, KERNEL_MODULE_IDENTITY);

        Object userDir =
            FusionValue.forIonValue(system.newString(builder.getInitialCurrentDirectory().toString()));
        DynamicParameter currentDirectory =
            new DynamicParameter(userDir);
        DynamicParameter currentLoadRelativeDirectory =
            new DynamicParameter(UNDEF);
        DynamicParameter currentModuleDeclareName =
            new DynamicParameter(UNDEF);
        DynamicParameter currentNamespaceParam =
            new DynamicParameter(initialCurrentNamespace);
        LoadHandler loadHandler =
            new LoadHandler(currentLoadRelativeDirectory, currentDirectory);
        ModuleNameResolver resolver =
            new ModuleNameResolver(loadHandler,
                                   currentLoadRelativeDirectory,
                                   currentDirectory,
                                   currentModuleDeclareName,
                                   builder.buildModuleRepositories());
        UseForm useForm = new UseForm(resolver);

        // These must be bound before 'module' since we need the bindings
        // for the partial-expansion stop-list.
        ns.define(DEFINE, new DefineForm());
        ns.define(DEFINE_SYNTAX, new DefineSyntaxForm());
        ns.define(REQUIRE, new RequireForm(resolver));
        ns.define(USE, useForm);

        SyntacticForm moduleForm =
            new ModuleForm(resolver, currentModuleDeclareName);
        LoadProc loadProc = new LoadProc(loadHandler);

        ns.define(BEGIN, new BeginForm());    // Needed by hard-coded macro

        ns.define("current_directory", currentDirectory,
                  "A [parameter](parameter.html) holding the thread-local working directory.");

        ns.define("current_namespace", currentNamespaceParam,
                  "A [parameter](parameter.html) holding the thread-local namespace.  This value has no direct\n" +
                  "relationship to the namespace lexically enclosing the parameter call.");

        ns.define("if", new IfForm());          // Needed by hard-coded macro
        ns.define("java_new", new JavaNewProc());
        ns.define(LAMBDA, new LambdaForm());    // Needed by hard-coded macro
        ns.define(LETREC, new LetrecForm());    // Needed by hard-coded macro
        ns.define("load", loadProc);
        ns.define(MODULE, moduleForm);
        ns.define("quote_syntax", new QuoteSyntaxForm()); // For fusion/syntax

        for (IonType t : IonType.values())
        {
            if (t != IonType.NULL &&
                t != IonType.DATAGRAM &&
                t != IonType.LIST &&
                t != IonType.SEXP &&
                t != IonType.STRUCT)
            {
                String name = "is_" + t.name().toLowerCase();
                ns.define(name, new IonTypeCheckingProc(t));
            }
        }

        ns.define("is_list",   new FusionList.IsListProc());
        ns.define("is_sexp",   new FusionSexp.IsSexpProc());
        ns.define("is_struct", new FusionStruct.IsStructProc());

        ns.define("=", new FusionCompare.EqualProc());

        ModuleInstance kernel = ns.build();
        registry.register(kernel);

        GlobalState globals =
            new GlobalState(system, kernel, resolver, loadHandler, useForm,
                            currentNamespaceParam);
        return globals;
    }


    /** Helper to ensure we have a real binding. */
    private Binding kernelBinding(String name)
    {
        Binding b = myKernelModule.resolveProvidedName(name).originalBinding();
        assert b != null;
        return b;
    }
}
