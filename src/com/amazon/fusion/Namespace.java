// Copyright (c) 2012-2016 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionIo.safeWriteToString;
import static com.amazon.fusion.FusionVoid.voidValue;
import com.amazon.fusion.FusionSymbol.BaseSymbol;
import com.amazon.fusion.ModuleNamespace.ModuleDefinedBinding;
import com.amazon.fusion.ModuleNamespace.ProvidedBinding;
import com.amazon.fusion.TopLevelNamespace.TopLevelDefinedBinding;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Expand- and compile-time environment for all top-level sequences, and
 * eval-time store for non-module top levels.
 * <p>
 * Since top-levels and modules have different behavior around imports and
 * defines, that responsibility is delegated to subclasses.
 *
 * @see TopLevelNamespace
 * @see ModuleNamespace
 */
abstract class Namespace
    implements Environment, NamespaceStore
{
    /**
     * Denotes all bindings resolved to namespace-level.
     * The binding can swing between free, definition, and required targets.
     */
    static final class NsBinding
        extends Binding
    {
        private EffectiveNsBinding myEffectiveBinding;

        NsBinding(EffectiveNsBinding binding)
        {
            myEffectiveBinding = binding;
        }

        @Override
        public String toString()
        {
            return "NsBinding::{active:" + myEffectiveBinding + "}";
        }

        void setEffectiveBinding(EffectiveNsBinding binding)
        {
            assert myEffectiveBinding.getName() == binding.getName();
            myEffectiveBinding = binding;
        }

        @Override
        BaseSymbol getName()
        {
            return myEffectiveBinding.getName();
        }

        @Override
        Binding target()
        {
            return myEffectiveBinding.target();
        }

        @Override
        ProvidedBinding provideAs(BaseSymbol name)
        {
            return myEffectiveBinding.provideAs(name);
        }

        @Override
        Object lookup(Namespace ns)
        {
            return myEffectiveBinding.lookup(ns);
        }

        @Override
        String mutationSyntaxErrorMessage()
        {
            return myEffectiveBinding.mutationSyntaxErrorMessage();
        }

        @Override
        CompiledForm compileDefine(Evaluator eval,
                                   Environment env,
                                   SyntaxSymbol id,
                                   CompiledForm valueForm)
            throws FusionException
        {
            return myEffectiveBinding.compileDefine(eval, env, id, valueForm);
        }

        @Override
        CompiledForm compileReference(Evaluator eval, Environment env)
            throws FusionException
        {
            return myEffectiveBinding.compileReference(eval, env);
        }

        @Override
        CompiledForm compileTopReference(Evaluator eval, Environment env,
                                         SyntaxSymbol id)
            throws FusionException
        {
            return myEffectiveBinding.compileTopReference(eval, env, id);
        }

        @Override
        CompiledForm compileSet(Evaluator eval, Environment env,
                                CompiledForm valueForm) throws FusionException
        {
            return myEffectiveBinding.compileSet(eval, env, valueForm);
        }
    }


    abstract static class EffectiveNsBinding
        extends Binding
    {
        /**
         * Attempts to bind a definition atop this effective binding.
         *
         * @return the definition to use.  Null indicates that a new definition
         * is to be installed.
         *
         * @throws AmbiguousBindingFailure if this binding cannot be replaced
         *   with a definition.
         */
        abstract NsDefinedBinding redefine(SyntaxSymbol identifier,
                                           SyntaxValue formForErrors)
            throws AmbiguousBindingFailure;

        /**
         * Attempts to bind an import atop this effective binding.
         *
         * @return the binding to use.
         *
         * @throws AmbiguousBindingFailure if this binding cannot be replaced
         *   with an import.
         */
        abstract RequiredBinding require(SyntaxSymbol localId,
                                         ProvidedBinding provided,
                                         SyntaxValue formForErrors)
            throws AmbiguousBindingFailure;

        /**
         * Returns the namespace-level definition that this binding <em>is</em>
         * or <em>shadows</em>.
         *
         * @return null if there's no namespace-level definition.
         */
        abstract NsDefinedBinding definition();
    }



    /**
     * Denotes a namespace-level definition binding, either top-level or
     * module-level.
     *
     * @see TopLevelDefinedBinding
     * @see ModuleDefinedBinding
     */
    abstract class NsDefinedBinding
        extends EffectiveNsBinding
    {
        private final BaseSymbol myName;
        private final String     myDebugName;
        final int myAddress;

        NsDefinedBinding(SyntaxSymbol identifier, int address)
        {
            myName      = identifier.getName();
            myDebugName = identifier.debugString();
            myAddress   = address;
        }

        @Override
        final BaseSymbol getName()
        {
            return myName;
        }

        final String getDebugName()
        {
            return myDebugName;
        }

        @Override
        final NsDefinedBinding definition()
        {
            return this;
        }

        final boolean isOwnedBy(Namespace ns)
        {
            return Namespace.this == ns;
        }

        final CompiledForm compileLocalTopReference(Evaluator   eval,
                                                    Environment env)
            throws FusionException
        {
            // TODO This fails when a macro references a prior local defn
            // since the defn isn't installed yet.  I think the code is bad
            // and mixes phases of macro processing.
//          assert (env.namespace().ownsBinding(this));
            return new CompiledTopVariableReference(myAddress);
        }

        @Override
        final CompiledForm compileSet(Evaluator eval, Environment env,
                                      CompiledForm valueForm)
            throws FusionException
        {
            throw new IllegalStateException("Mutation should have been rejected");
        }

        CompiledForm compileDefineSyntax(Evaluator eval,
                                         Environment env,
                                         CompiledForm valueForm)
        {
            String name = getName().stringValue();
            return new CompiledTopDefineSyntax(name, myAddress, valueForm);
        }

        @Override
        public boolean equals(Object other)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public final int hashCode()
        {
            return System.identityHashCode(this);
        }

        @Override
        public abstract String toString(); // Force subclasses to implement
    }


    /**
     * A binding added to a namepace via {@code require} or a language
     * declaration.
     */
    abstract static class RequiredBinding
        extends EffectiveNsBinding
    {
        private final BaseSymbol myName;
        private final String     myDebugName;
        final ProvidedBinding myTarget;

        RequiredBinding(SyntaxSymbol identifier, ProvidedBinding target)
        {
            assert target != null;
            myName      = identifier.getName();
            myDebugName = identifier.debugString();
            myTarget = target;
        }

        @Override
        final BaseSymbol getName()
        {
            return myName;
        }

        final String getDebugName()
        {
            return myDebugName;
        }

        @Override
        final ModuleDefinedBinding target()
        {
            return myTarget.target();
        }

        @Override
        final Object lookup(Namespace ns)
        {
            return myTarget.lookup(ns);
        }

        @Override
        final CompiledForm compileDefine(Evaluator eval, Environment env,
                                         SyntaxSymbol id, CompiledForm valueForm)
            throws FusionException
        {
            return myTarget.compileDefine(eval, env, id, valueForm);
        }

        @Override
        final CompiledForm compileReference(Evaluator eval, Environment env)
            throws FusionException
        {
            return myTarget.compileReference(eval, env);
        }

        @Override
        final CompiledForm compileSet(Evaluator eval, Environment env,
                                      CompiledForm valueForm)
            throws FusionException
        {
            // This isn't currently reachable, but it's an easy safeguard.
            String message = "Mutation of imported binding is not allowed";
            throw new ContractException(message);
        }

        @Override
        public final boolean equals(Object other)
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * Exposes the bindings visible at namespace-level.
     */
    static abstract class NamespaceWrap
        extends EnvironmentWrap
    {
        NamespaceWrap(Namespace ns)
        {
            super(ns);
        }

        @Override
        Binding resolveTop(BaseSymbol           name,
                           Iterator<SyntaxWrap> moreWraps,
                           Set<MarkWrap>        returnMarks)
        {
            if (moreWraps.hasNext())
            {
                SyntaxWrap nextWrap = moreWraps.next();
                return nextWrap.resolve(name, moreWraps, returnMarks);
            }
            return null;
        }

        @Override
        public String toString()
        {
            ModuleIdentity id =
                ((Namespace) getEnvironment()).getModuleId();
            return "{{{NS " + id.absolutePath() + "}}}";
        }
    }


    private final ModuleRegistry myRegistry;
    private final ModuleIdentity myModuleId;

    /**
     * Assigns required modules to integer addresses, for use in compiled
     * forms.
     */
    private final HashMap<ModuleIdentity,Integer> myRequiredModules =
        new HashMap<>();
    private final ArrayList<ModuleStore> myRequiredModuleStores =
        new ArrayList<>();

    private final SyntaxWraps          myWraps;
    private final BoundIdMap<NsBinding> myBindings = new BoundIdMap<>();
    private       int                   myDefinitionCount;
    private final ArrayList<Object>    myValues   = new ArrayList<>();
    private ArrayList<BindingDoc> myBindingDocs;


    /**
     * @param registry must not be null.
     * @param id must not be null.
     * @param wraps generates the {@link SyntaxWraps} for this namespace, given
     *   a reference to {@code this}.
     */
    Namespace(ModuleRegistry                   registry,
              ModuleIdentity                   id,
              Function<Namespace, SyntaxWraps> wraps)
    {
        myRegistry = registry;
        myModuleId = id;
        myWraps    = wraps.apply(this);
    }

    @Override
    public final ModuleRegistry getRegistry()
    {
        return myRegistry;
    }

    @Override
    public final Namespace namespace()
    {
        return this;
    }

    @Override
    public final int getDepth()
    {
        return 0;
    }

    @Override
    public final Object lookup(int rib, int address)
    {
        String message = "Rib not found: " + rib + ',' + address;
        throw new IllegalStateException(message);
    }

    @Override
    public final void set(int rib, int address, Object value)
    {
        String message = "Rib not found: " + rib + ',' + address;
        throw new IllegalStateException(message);
    }

    /**
     * Gets the identity of the module associated with this namespace.
     * For non-module namespaces, this returns a synthetic identity that's
     * unique amongst all namespaces.
     *
     * @return not null.
     */
    final ModuleIdentity getModuleId()
    {
        return myModuleId;
    }

    /**
     * How many namspace-level definitions do we have?
     * This doesn't count imports.
     */
    final int definitionCount()
    {
        return myDefinitionCount;
    }

    /**
     * Collects the bindings defined in this namespace; does not include imported
     * bindings.
     *
     * @return not null.
     */
    final Collection<NsDefinedBinding> getDefinedBindings()
    {
        ArrayList<NsDefinedBinding> definitions =
            new ArrayList<>(definitionCount());

        for (NsBinding b : myBindings.values())
        {
            NsDefinedBinding def = b.myEffectiveBinding.definition();
            if (def != null)
            {
                definitions.add(def);
            }
        }

        assert definitions.size() == definitionCount();
        return definitions;
    }

    //========================================================================

    /**
     * Adds wraps to the syntax object to give it the bindings of this
     * namespace and of required modules.
     */
    final SyntaxValue syntaxIntroduce(SyntaxValue source)
        throws FusionException
    {
        // TODO there's a case where we are applying the same wraps that are
        // already on the source.  This happens when expand-ing (and maybe when
        // eval-ing at top-level source that's from that same context.
        source = source.addWraps(myWraps);
        return source;
    }


    final void installNewBinding(SyntaxSymbol boundIdentifier,
                                 EffectiveNsBinding effective)
    {
        NsBinding nsBinding = new NsBinding(effective);
        NsBinding prior = myBindings.put(boundIdentifier, nsBinding);
        assert prior == null;
    }

    /**
     * Resolve as with {@code bound_identifier_equal}.
     *
     * @return null if there's no binding in this namespace.
     */
    private final NsBinding resolveBound(SyntaxSymbol identifier)
    {
        Binding binding = identifier.resolve();
        Set<MarkWrap> marks = identifier.computeMarks();
        return myBindings.get(binding, marks);
    }

    /**
     * @param binding must not be null.
     * @param marks must not be null.
     *
     * @return null if identifier isn't bound here.
     */
    final NsBinding resolve(Binding binding, Set<MarkWrap> marks)
    {
        return myBindings.get(binding, marks);
    }

    @Override
    public final NsBinding substituteFree(BaseSymbol name, Set<MarkWrap> marks)
    {
        Binding b = new FreeBinding(name);
        return resolve(b, marks);
    }

    @Override
    public final Binding substitute(Binding binding, Set<MarkWrap> marks)
    {
        Binding subst = resolve(binding, marks);
        if (subst == null) subst = binding;
        return subst;
    }

    /**
     * Resolves an identifier to a namespace-level definition (not an import).
     *
     * @return null if identifier isn't defined here.
     */
    final NsDefinedBinding resolveDefinition(SyntaxSymbol identifier)
    {
        identifier = identifier.copyAndResolveTop();
        NsBinding nsb = resolveBound(identifier);
        return (nsb == null ? null : nsb.myEffectiveBinding.definition());
    }


    /**
     * @param name must be non-empty.
     *
     * @return null is equivalent to a {@link FreeBinding}.
     */
    final Binding resolve(BaseSymbol name)
    {
        return myWraps.resolve(name);
    }

    /**
     * @param name must be non-empty.
     *
     * @return null is equivalent to a {@link FreeBinding}.
     */
    final Binding resolve(String name)
    {
        BaseSymbol symbol = FusionSymbol.makeSymbol(null, name);
        return resolve(symbol);
    }


    abstract NsDefinedBinding newDefinedBinding(SyntaxSymbol identifier,
                                                int          address);


    /**
     * Creates a definition binding, but no value, for a name.
     * Used during expansion phase, before evaluating the right-hand side.
     *
     * @return a copy of the identifier that has the new binding attached.
     */
    SyntaxSymbol predefine(SyntaxSymbol identifier, SyntaxValue formForErrors)
        throws FusionException
    {
        NsDefinedBinding newDefinition;

        identifier = identifier.copyAndResolveTop();

        NsBinding entry = resolveBound(identifier);
        if (entry == null)
        {
            newDefinition = newDefinedBinding(identifier, myDefinitionCount);
            myDefinitionCount++;

            installNewBinding(identifier, newDefinition);
        }
        else
        {
            EffectiveNsBinding effective = entry.myEffectiveBinding;

            newDefinition = effective.redefine(identifier, formForErrors);

            if (newDefinition == null)
            {
                newDefinition = newDefinedBinding(identifier, myDefinitionCount);
                myDefinitionCount++;
            }

            entry.setEffectiveBinding(newDefinition);
        }

        return identifier.copyReplacingBinding(newDefinition);
    }

    /**
     * Creates or updates a namespace-level binding.
     * Allows rebinding of existing names!
     *
     * @param value must not be null
     */
    final void bind(NsDefinedBinding binding, Object value)
    {
        set(binding.myAddress, value);

        if (value instanceof NamedValue)
        {
            String inferredName = binding.getName().stringValue();
            if (inferredName != null)
            {
                ((NamedValue)value).inferName(inferredName);
            }
        }
    }


    private <T> void set(ArrayList<T> list, int address, T value)
    {
        int size = list.size();
        if (address < size)
        {
            list.set(address, value);
        }
        else // We need to grow the list. Annoying lack of API to do this.
        {
            list.ensureCapacity(myDefinitionCount);        // Grow all at once
            for (int i = size; i < address; i++)
            {
                list.add(null);
            }
            list.add(value);
        }
    }


    /**
     * Updates a pre-defined namespace-level variable.
     * Allows rebinding of existing names!
     *
     * @param value must not be null
     */
    @Override
    public final void set(int address, Object value)
    {
        set(myValues, address, value);
    }


    /**
     * Creates or updates a namespace-level binding.
     * Allows rebinding of existing names!
     *
     * @param value must not be null
     *
     * @throws IllegalArgumentException if the name is null or empty.
     */
    public final void bind(String name, Object value)
        throws FusionException
    {
        if (name == null || name.length() == 0)
        {
            String message = "bound name must be non-null and non-empty";
            throw new IllegalArgumentException(message);
        }

        // WARNING: We pass null evaluator because we know its not used.
        //          That is NOT SUPPORTED for user code!
        SyntaxSymbol identifier = SyntaxSymbol.make(null, name);

        identifier = predefine(identifier, null);
        NsDefinedBinding binding = (NsDefinedBinding) identifier.getBinding();
        bind(binding, value);
    }


    //========================================================================
    //  Require

    /**
     * Denotes a new binding created by {@code require}, possibly renaming the
     * exported symbol along the way.
     */
    static final class RequireRenameMapping
    {
        final SyntaxSymbol myLocalIdentifier;
        final BaseSymbol   myExportedIdentifier;

        RequireRenameMapping(SyntaxSymbol localIdentifier,
                             BaseSymbol exportedIdentifier)
        {
            myLocalIdentifier = localIdentifier;
            myExportedIdentifier = exportedIdentifier;
        }

        @Override
        public String toString()
        {
            return "RequireRenameMapping::{local:" + myLocalIdentifier
                + ",exported:" + myExportedIdentifier + '}';
        }
    }


    /**
     * @param modulePath is an absolute or relative module path.
     */
    final void require(Evaluator eval, String modulePath)
        throws FusionException
    {
        ModuleNameResolver resolver =
            eval.getGlobalState().myModuleNameResolver;
        ModuleIdentity id =
            resolver.resolveModulePath(eval,
                                       getModuleId(),
                                       modulePath,
                                       true /* load */,
                                       null /* stxForErrors */);
        require(eval, null /* lexical context */, id);
    }

    /**
     * Imports all exported bindings from a module.
     * This is used by {@code (require module_path)}.
     */
    final void require(Evaluator      eval,
                       SyntaxSymbol   context,
                       ModuleIdentity maduleId)
        throws FusionException
    {
        ModuleInstance module = myRegistry.instantiate(eval, maduleId);

        for (ProvidedBinding provided : module.providedBindings())
        {
            SyntaxSymbol id = SyntaxSymbol.make(eval, null, provided.getName());
            id = (SyntaxSymbol) Syntax.applyContext(eval, context, id);
            installRequiredBinding(eval, id, provided);
        }
    }


    final void require(Evaluator eval, ModuleIdentity moduleId,
                       Iterator<RequireRenameMapping> mappings)
        throws FusionException
    {
        ModuleInstance module = myRegistry.instantiate(eval, moduleId);

        while (mappings.hasNext())
        {
            RequireRenameMapping mapping = mappings.next();
            BaseSymbol exportedId = mapping.myExportedIdentifier;
            ProvidedBinding provided = module.resolveProvidedName(exportedId);

            // TODO this is horribly hacky. Ideally we'd raise a syntax exn on
            //   the original require spec
            if (provided == null)
            {
                // TODO Error reporting is bad here, it's lost the location of
                //   the unbound id so it reports the entire require form (when
                //   this gets wrapped.
                SyntaxSymbol sym = SyntaxSymbol.make(eval, null, exportedId);
                throw new UnboundIdentifierException(sym);
            }

            installRequiredBinding(eval, mapping.myLocalIdentifier, provided);
        }
    }

    /**
     * @param eval must not be null.
     * @param localId will be re-resolved to the outside edge of this namespace.
     * @param provided the binding to import.
     * @throws AmbiguousBindingFailure
     */
    final void installRequiredBinding(Evaluator       eval,
                                      SyntaxSymbol    localId,
                                      ProvidedBinding provided)
        throws AmbiguousBindingFailure
    {
        localId = localId.copyAndResolveTop();

        NsBinding entry = resolveBound(localId);
        if (entry == null)
        {
            RequiredBinding required = newRequiredBinding(localId, provided);
            installNewBinding(localId, required);
        }
        else
        {
            EffectiveNsBinding effective = entry.myEffectiveBinding;

            RequiredBinding required = effective.require(localId, provided, null);

            entry.setEffectiveBinding(required);
        }
    }

    /**
     * Creates a binding for a require that has no prior binding.
     */
    abstract RequiredBinding newRequiredBinding(SyntaxSymbol    localId,
                                                ProvidedBinding target);


    //========================================================================


    final boolean ownsDefinedBinding(Binding binding)
    {
        if (binding instanceof NsBinding)
        {
            binding = ((NsBinding) binding).myEffectiveBinding;
        }
        if (binding instanceof NsDefinedBinding)
        {
            return ((NsDefinedBinding) binding).isOwnedBy(this);
        }
        return false;
    }



    abstract CompiledForm compileDefine(Evaluator eval,
                                        FreeBinding binding,
                                        SyntaxSymbol id,
                                        CompiledForm valueForm)
        throws FusionException;

    abstract CompiledForm compileDefine(Evaluator eval,
                                        TopLevelDefinedBinding binding,
                                        SyntaxSymbol id,
                                        CompiledForm valueForm)
        throws FusionException;

    abstract CompiledForm compileDefine(Evaluator eval,
                                        ModuleDefinedBinding binding,
                                        SyntaxSymbol id,
                                        CompiledForm valueForm)
        throws FusionException;


    /**
     * Compile a free variable reference.  These are allowed at top-level but
     * not within a module.
     */
    abstract CompiledForm compileFreeTopReference(SyntaxSymbol identifier)
        throws FusionException;


    /**
     * Looks for a definition's value in this namespace, ignoring imports.
     *
     * @return the binding's value, or null if there is none.
     */
    final Object lookupDefinition(NsDefinedBinding binding)
    {
        if (binding.isOwnedBy(this))
        {
            int address = binding.myAddress;
            if (address < myValues.size())         // for prepare-time lookup
            {
                return myValues.get(address);
            }
        }
        return null;
    }


    /**
     * Looks for a binding's value in this namespace, finding both definitions
     * and imports.
     *
     * @return the binding's value, or null if there is none.
     */
    final Object lookup(Binding binding)
    {
        return binding.lookup(this);
    }


    /**
     * Looks for a binding's value in this namespace, finding both definitions
     * and imports.
     *
     * @return the binding's value, or null if there is none.
     */
    final Object lookup(String name)
    {
        Binding b = resolve(name);
        if (b == null)
        {
            return b;
        }
        else
        {
            return lookup(b);
        }
    }


    @Override
    public final Object lookup(int address)
    {
        return myValues.get(address);
    }

    @Override
    public final Object lookupImport(int moduleAddress, int bindingAddress)
    {
        ModuleStore store = myRequiredModuleStores.get(moduleAddress);
        return store.lookup(bindingAddress);
    }

    final Object[] extractValues()
    {
        return myValues.toArray();
    }


    /**
     * Translates a required module identity into an integer address for use
     * by compiled forms.  Note that some required modules may not be
     * explicitly declared in the source of the module, since they may come in
     * via macro expansion.
     * <p>
     * Building this list is delayed to compile-time to avoid compiling
     * addresses for modules that are declared but never used.
     * This may be a useless optimization.
     * <p>
     * @return a zero-based address for the module, valid only within this
     * namespace (or its compiled form).
     */
    final synchronized int requiredModuleAddress(ModuleIdentity moduleId)
    {
        Integer id = myRequiredModules.get(moduleId);
        if (id == null)
        {
            ModuleInstance module = myRegistry.lookup(moduleId);

            id = myRequiredModules.size();
            myRequiredModules.put(moduleId, id);
            myRequiredModuleStores.add(module.getNamespace());
        }
        return id;
    }

    final synchronized ModuleIdentity[] requiredModuleIds()
    {
        ModuleIdentity[] ids = new ModuleIdentity[myRequiredModules.size()];
        for (Map.Entry<ModuleIdentity, Integer> entry
                : myRequiredModules.entrySet())
        {
            int address = entry.getValue();
            ids[address] = entry.getKey();
        }
        return ids;
    }

    @Override
    public final ModuleStore lookupRequiredModule(int moduleAddress)
    {
        return myRequiredModuleStores.get(moduleAddress);
    }


    //========================================================================
    // Documentation


    final void setDoc(String name, BindingDoc.Kind kind, String doc)
    {
        BindingDoc bDoc = new BindingDoc(name, kind,
                                         null, // usage
                                         doc);
        setDoc(name, bDoc);
    }

    final void setDoc(String name, BindingDoc doc)
    {
        NsBinding top = (NsBinding) resolve(name);
        NsDefinedBinding binding = (NsDefinedBinding) top.myEffectiveBinding;
        setDoc(binding.myAddress, doc);
    }

    public void setDoc(int address, BindingDoc doc)
    {
        if (myBindingDocs == null)
        {
            myBindingDocs = new ArrayList<BindingDoc>();
        }
        set(myBindingDocs, address, doc);
    }

    final BindingDoc document(int address)
    {
        if (myBindingDocs != null && address < myBindingDocs.size())
        {
            return myBindingDocs.get(address);
        }
        return null;
    }

    /**
     * @return may be shorter than the number of provided variables.
     */
    final BindingDoc[] extractBindingDocs()
    {
        if (myBindingDocs == null) return BindingDoc.EMPTY_ARRAY;

        BindingDoc[] docs = myBindingDocs.toArray(BindingDoc.EMPTY_ARRAY);
        myBindingDocs = null;
        return docs;
    }


    //========================================================================


    /**
     * A reference to a top-level variable in the lexically-enclosing namespace.
     */
    static final class CompiledTopVariableReference
        implements CompiledForm
    {
        final int myAddress;

        CompiledTopVariableReference(int address)
        {
            myAddress = address;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            NamespaceStore ns = store.namespace();
            Object result = ns.lookup(myAddress);
            assert result != null : "No value for namespace address " + myAddress;
            return result;
        }
    }


    // TODO rename CompiledNsDefine
    static class CompiledTopDefine
        implements CompiledForm
    {
        private final String       myName;
        private final int          myAddress;
        private final CompiledForm myValueForm;

        CompiledTopDefine(String name, int address, CompiledForm valueForm)
        {
            assert name != null;
            myName      = name;
            myAddress   = address;
            myValueForm = valueForm;
        }

        @Override
        public final Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object value = eval.eval(store, myValueForm);

            value = processValue(eval, store, value);

            NamespaceStore ns = store.namespace();
            ns.set(myAddress, value);

            if (value instanceof NamedValue)
            {
                ((NamedValue)value).inferName(myName);
            }

            return voidValue(eval);
        }

        Object processValue(Evaluator eval, Store store, Object value)
            throws FusionException
        {
            return value;
        }
    }


    private static final class CompiledTopDefineSyntax
        extends CompiledTopDefine
    {
        private CompiledTopDefineSyntax(String name, int address,
                                        CompiledForm valueForm)
        {
            super(name, address, valueForm);
        }

        @Override
        Object processValue(Evaluator eval, Store store, Object value)
            throws FusionException
        {
            if (value instanceof Procedure)
            {
                Procedure xformProc = (Procedure) value;
                value = new MacroForm(xformProc);
            }
            else if (! (value instanceof SyntacticForm))
            {
                String message =
                    "define_syntax value is not a transformer: " +
                    safeWriteToString(eval, value);
                throw new ContractException(message);
            }

            return value;
        }
    }
}
