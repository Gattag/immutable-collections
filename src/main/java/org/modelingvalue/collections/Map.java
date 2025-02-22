//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2023 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus, Ronald Krijgsheld                                                                           ~
// Contributors:                                                                                                       ~
//     Arjan Kok, Carel Bast                                                                                           ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.collections;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

import org.modelingvalue.collections.impl.MapImpl;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.collections.util.Mergeable;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.collections.util.QuadFunction;

@SuppressWarnings("unused")
public interface Map<K, V> extends ContainingCollection<Entry<K, V>>, Mergeable<Map<K, V>> {

    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> of(Entry<K, V>... entries) {
        if (entries.length == 0) {
            return MapImpl.EMPTY;
        } else {
            return new MapImpl<>(entries);
        }
    }

    V get(K key);

    V getOrDefault(K key, V defaultValue);

    default boolean containsKey(K key) {
        return getEntry(key) != null;
    }

    Entry<K, V> getEntry(K key);

    Collection<V> getAll(Set<K> keys);

    Map<K, V> put(Entry<K, V> entry);

    Map<K, V> put(K key, V value);

    Map<K, V> putIfAbsent(K key, V value);

    Map<K, V> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

    Map<K, V> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    Map<K, V> compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    Map<K, V> merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction);

    Map<K, V> putAll(Map<? extends K, ? extends V> c);

    Map<K, V> add(K key, V value);

    Map<K, V> add(Entry<K, V> entry, BinaryOperator<V> merger);

    Map<K, V> add(K key, V value, BinaryOperator<V> merger);

    Map<K, V> addAll(Map<? extends K, ? extends V> c, BinaryOperator<V> merger);

    Map<K, V> remove(Entry<K, V> entry, BinaryOperator<V> merger);

    Map<K, V> remove(K key, V value, BinaryOperator<V> merger);

    Map<K, V> removeAll(Map<? extends K, ? extends V> c, BinaryOperator<V> merger);

    Map<K, V> removeKey(K key);

    Map<K, V> removeAllKey(Collection<?> c);

    Map<K, V> filter(Predicate<? super K> keyPredicate, Predicate<? super V> valuePredicate);

    <V2> Map<K, V> removeAllKey(Map<K, V2> m);

    void deduplicate(Map<K, V> other);

    Collection<K> toKeys();

    Collection<V> toValues();

    Map<K, V> merge(QuadFunction<K, V, V[], Integer, V> merger, Map<K, V>[] branches, int length);

    @Override
    Map<K, V> remove(Object e);

    @Override
    Map<K, V> removeAll(Collection<?> e);

    @Override
    Map<K, V> add(Entry<K, V> e);

    @Override
    Map<K, V> replace(Object pre, Entry<K, V> post);

    @Override
    Map<K, V> addAll(Collection<? extends Entry<K, V>> es);

    Collection<Entry<K, Pair<V, V>>> diff(Map<K, V> other);

    void forEach(BiConsumer<K, V> action);

    @Override
    Map<K, V> clear();

    java.util.Map<K, V> toMutable();

    static <S, E> Map<S, E> fromMutable(java.util.Map<S, E> mutable) {
        return mutable instanceof MutableMap ? ((MutableMap<S, E>) mutable).toImmutable() : Collection.of(mutable.entrySet()).toMap(e -> Entry.of(e.getKey(), e.getValue()));
    }
}
