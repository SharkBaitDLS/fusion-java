// Copyright (c) 2018-2021 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion.util.hamt;

import com.amazon.fusion.BiFunction;
import com.amazon.fusion.BiPredicate;
import com.amazon.fusion.util.hamt.HashArrayMappedTrie.TrieNode;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A {@link MultiHashTrie} that has at most one entry per key.
 * <p>
 * <b>Warning:</b> This class is an internal implementation detail of FusionJava;
 * it is not for use by applications.
 * </p>
 *
 * @param <K> The type of key in an entry.
 * @param <V> The type of value in an entry.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FunctionalHashTrie<K, V>
    extends MultiHashTrie<K, V>
{
    private static final FunctionalHashTrie EMPTY =
        new FunctionalHashTrie<>(HashArrayMappedTrie.empty(), 0);


    // Mostly here so consumers don't have to use HashArrayMappedTrie.
    public static class Changes
        extends HashArrayMappedTrie.Changes
    {
    }


    FunctionalHashTrie(TrieNode<K, V> root, int size)
    {
        super(root, size);
    }


    private FunctionalHashTrie<K, V> resultFrom(TrieNode<K, V> newRoot,
                                                int priorChangeCount,
                                                int priorKeyCountDelta,
                                                Changes changes)
    {
        if (changes.changeCount() != priorChangeCount)
        {
            int newSize = keyCount() + changes.keyCountDelta() - priorKeyCountDelta;
            if (newSize == 0) return EMPTY;
            return new FunctionalHashTrie<>(newRoot, newSize);
        }

        assert root == newRoot;
        return this;
    }


    //=========================================================================
    // Creation

    public static <K, V> FunctionalHashTrie<K, V> empty()
    {
        return EMPTY;
    }

    public static <K, V> FunctionalHashTrie<K, V> create(Map<K, V> other)
    {
        Changes changes = new Changes();
        TrieNode<K, V> trie = HashArrayMappedTrie.fromMap(other, changes);
        return EMPTY.resultFrom(trie, 0, 0, changes);
    }


    public static <K, V> FunctionalHashTrie<K, V> fromEntries(Iterator<Entry<K, V>> items,
                                                              Changes changes)
    {
        int priorChangeCount   = changes.changeCount();
        int priorKeyCountDelta = changes.keyCountDelta();

        TrieNode<K, V> trie = HashArrayMappedTrie.fromEntries(items, changes);

        return EMPTY.resultFrom(trie, priorChangeCount, priorKeyCountDelta, changes);
    }

    public static <K, V> FunctionalHashTrie<K, V> fromEntries(Entry<K, V>[] items,
                                                       Changes changes)
    {
        return fromEntries(Arrays.asList(items).iterator(), changes);
    }

    public static <K, V> FunctionalHashTrie<K, V> fromArrays(K[] keys,
                                                             V[] values,
                                                             Changes changes)
    {
        int priorChangeCount   = changes.changeCount();
        int priorKeyCountDelta = changes.keyCountDelta();

        TrieNode<K, V> trie = HashArrayMappedTrie.fromArrays(keys, values, changes);

        return EMPTY.resultFrom(trie, priorChangeCount, priorKeyCountDelta, changes);
    }

    /**
     * Creates a trie by copying entries for the {@code keys} from the
     * {@code origin} trie.
     */
    public static <K, V> FunctionalHashTrie<K, V>
    fromSelectedKeys(FunctionalHashTrie<K, V> origin, K[] keys)
    {
        return fromSelectedKeys(origin, keys, new Changes());
    }

    public static <K, V> FunctionalHashTrie<K, V>
    fromSelectedKeys(FunctionalHashTrie<K, V> origin, K[] keys, Changes changes)
    {
        int priorChangeCount   = changes.changeCount();
        int priorKeyCountDelta = changes.keyCountDelta();

        TrieNode<K, V> trie =
                HashArrayMappedTrie.fromSelectedKeys(origin.root, keys, changes);
        return EMPTY.resultFrom(trie, priorChangeCount, priorKeyCountDelta, changes);
    }


    //=========================================================================
    // Inspection

    public int size()
    {
        return keyCount();
    }


    @Override
    public Iterator<Entry<K, V>> iterator()
    {
        return root.iterator();
    }


    @Override
    public V get(K key)
    {
        validateKey(key);

        return root.get(key);
    }


    //=========================================================================
    // Modification

    @Override
    public FunctionalHashTrie<K, V> with(K key, V value)
    {
        return with(key, value, new Changes());
    }

    /**
     * Functionally modify this trie, allowing for custom mappings.
     */
    public FunctionalHashTrie<K, V> with(K key, V value, Changes changes)
    {
        validateKeyAndValue(key, value);

        int priorChangeCount   = changes.changeCount();
        int priorKeyCountDelta = changes.keyCountDelta();

        TrieNode<K, V> newRoot = root.with(key, value, changes);
        return resultFrom(newRoot, priorChangeCount, priorKeyCountDelta, changes);
    }


    public FunctionalHashTrie<K, V> merge(FunctionalHashTrie<K, V> that,
                                          Changes changes)
    {
        int priorChangeCount   = changes.changeCount();
        int priorKeyCountDelta = changes.keyCountDelta();

        TrieNode<K, V> newRoot = root.with(that.iterator(), changes);
        return resultFrom(newRoot, priorChangeCount, priorKeyCountDelta, changes);
    }


    @Override
    public FunctionalHashTrie<K, V> without(K key)
    {
        validateKey(key);

        Changes changes = new Changes();
        TrieNode<K, V> newRoot = root.without(key, changes);
        return resultFrom(newRoot, 0, 0, changes);
    }

    @Override
    public FunctionalHashTrie<K, V> withoutKeys(K[] keys)
    {
        return withoutKeys(keys, new Changes());
    }

    /**
     * Functionally removes multiple keys from this trie.
     *
     * @param keys must not be null.
     *
     * @return the resulting trie.
     */
    public FunctionalHashTrie<K, V> withoutKeys(K[] keys, Changes changes)
    {
        int priorChangeCount   = changes.changeCount();
        int priorKeyCountDelta = changes.keyCountDelta();

        TrieNode<K, V> newRoot = root.withoutKeys(keys, changes);
        return resultFrom(newRoot, priorChangeCount, priorKeyCountDelta, changes);
    }


    @Override
    public FunctionalHashTrie<K, V> transform(BiFunction<K, V, V> xform)
    {
        return transform(xform, new Changes());
    }

    public FunctionalHashTrie<K, V> transform(BiFunction<K, V, V> xform, Changes changes)
    {
        int priorChangeCount   = changes.changeCount();
        int priorKeyCountDelta = changes.keyCountDelta();

        TrieNode<K, V> newRoot = root.transform(xform, changes);
        return resultFrom(newRoot, priorChangeCount, priorKeyCountDelta, changes);
    }


    //=========================================================================
    // Comparison

    public boolean equals(Object that)
    {
        return (this == that)
                   || (that instanceof FunctionalHashTrie
                           && this.equals((FunctionalHashTrie) that));
    }

    public boolean equals(FunctionalHashTrie<K, V> that)
    {
        return equals(that, EQUALS_BIPRED);
    }

    @Override
    protected boolean mappingEquals(V lv, V rv, BiPredicate<V, V> comp)
    {
        return comp.test(lv, rv);
    }
}
