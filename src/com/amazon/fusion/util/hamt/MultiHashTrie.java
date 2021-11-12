// Copyright (c) 2018-2021 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion.util.hamt;

import com.amazon.fusion.BiFunction;
import com.amazon.fusion.BiPredicate;
import com.amazon.fusion.util.hamt.HashArrayMappedTrie.TrieNode;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A persistent hash map based on {@link HashArrayMappedTrie}, allowing multiple
 * entries per key.
 * <p>
 * <b>Warning:</b> This class is an internal implementation detail of FusionJava;
 * it is not for use by applications.
 * </p>
 * <p>
 * There's currently no support for null keys or values. Attempts to use null
 * on operations such as {@link #get(Object)} or {@link #with(Object, Object)}
 * will throw a {@link NullPointerException}.
 * </p>
 * <p>
 * Iteration order of this data structure is undefined and not guaranteed to be stable.
 * </p>
 *
 * @param <K> The type of key in an entry.
 * @param <V> The type of value in an entry.
 */
public abstract class MultiHashTrie<K, V>
    implements Iterable<Entry<K, V>>
{
    private static final String NULL_ERROR_MESSAGE =
        "Hashes do not support null keys or values";

    protected void validateKey(Object key)
    {
        if (key == null)
        {
            throw new NullPointerException(NULL_ERROR_MESSAGE);
        }
    }

    protected void validateKeyAndValue(Object key, Object value)
    {
        if (key == null || value == null)
        {
            throw new NullPointerException(NULL_ERROR_MESSAGE);
        }
    }


    protected final TrieNode<K, V> root;
    private   final int            keyCount;

    MultiHashTrie(TrieNode<K, V> root, int keyCount)
    {
        this.root     = root;
        this.keyCount = keyCount;
    }


    //=========================================================================
    // Inspection

    public final boolean isEmpty()
    {
        return keyCount == 0;
    }

    /**
     * Returns the number of distinct keys in the hash.
     */
    public final int keyCount()
    {
        return keyCount;
    }

    /**
     * Returns the number of entries (key-value pairs) in the hash.
     */
    public abstract int size();


    /**
     * @param key to examine the map for.
     * @return true if the key is in the map, false otherwise.
     */
    public boolean containsKey(K key)
    {
        return root.get(key) != null;
    }

    /**
     * @param key the key to search for.
     * @return the value associated with key, null if it is not in the map.
     */
    public abstract V get(K key);


    //=========================================================================
    // Modification

    /**
     * Replaces all existing entries for a key with a single value.
     *
     * @return the resulting trie; {@code this} if nothing has changed.
     */
    public abstract MultiHashTrie<K, V> with(K key, V value);


    /**
     * Removes all existing entries for a key.
     *
     * @param key must not be null.

     * @return the resulting trie; {@code this} if nothing has changed.
     */
    public abstract MultiHashTrie<K, V> without(K key);


    /**
     * Removes multiple keys from this trie.
     *
     * @param keys must not be null and must not contain a null element.
     *
     * @return the resulting trie; {@code this} if nothing has changed.
     */
    @SuppressWarnings("unchecked")
    public abstract MultiHashTrie<K, V> withoutKeys(K... keys);


    /**
     * Applies a transformation function to each key-value entry in the trie.
     *
     * @param xform accepts the existing key and value, returning a transformed
     *              value for the key.
     *
     * @return the resulting trie; {@code this} if nothing has changed.
     */
    public abstract MultiHashTrie<K, V> transform(BiFunction<K, V, V> xform);


    //=========================================================================
    // Comparison

    @SuppressWarnings("rawtypes")
    protected static final BiPredicate EQUALS_BIPRED = new BiPredicate()
    {
        public boolean test(Object o1, Object o2)
        {
            return o1.equals(o2);
        }
    };

    /**
     * Compare against another hash, using a predicate to compare values.
     */
    public boolean equals(MultiHashTrie<K,V> that, BiPredicate<V, V> comp)
    {
        // FIXME null check on that
        if (this.getClass() != that.getClass()) return false;

        if (size() != that.size()) return false;

        for (Entry<K, V> entry : root)
        {
            K fieldName = entry.getKey();

            V lv = entry.getValue();
            V rv = that.root.get(fieldName);

            if (rv == null) return false;

            if (! mappingEquals(lv, rv, comp)) return false;
        }

        return true;
    }

    protected abstract boolean mappingEquals(V lv, V rv, BiPredicate<V, V> comp);


    public Set<K> keySet()
    {
        // FIXME This is an extremely expensive implementation.
        return new AbstractSet<K>()
        {
            @Override
            public Iterator<K> iterator()
            {
                final Iterator<Entry<K, V>> entryIter = MultiHashTrie.this.iterator();
                return new Iterator<K>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        return entryIter.hasNext();
                    }

                    @Override
                    public K next()
                    {
                        return entryIter.next().getKey();
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size()
            {
                return keyCount;
            }
        };
    }
}
