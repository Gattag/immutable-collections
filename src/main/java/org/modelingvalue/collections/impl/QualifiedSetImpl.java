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

package org.modelingvalue.collections.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Predicate;

import org.modelingvalue.collections.Collection;
import org.modelingvalue.collections.QualifiedSet;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.ArrayUtil;
import org.modelingvalue.collections.util.Deserializer;
import org.modelingvalue.collections.util.Mergeables;
import org.modelingvalue.collections.util.QuadFunction;
import org.modelingvalue.collections.util.SerializableFunction;
import org.modelingvalue.collections.util.Serializer;

@SuppressWarnings("serial")
public class QualifiedSetImpl<K, V> extends HashCollectionImpl<V> implements QualifiedSet<K, V> {

    private SerializableFunction<V, K> qualifier;

    public QualifiedSetImpl(SerializableFunction<V, K> qualifier, V[] es) {
        this.qualifier = qualifier.of();
        this.value = es.length == 1 ? es[0] : addAll(null, key(), es);
    }

    public QualifiedSetImpl(SerializableFunction<V, K> qualifier, java.util.Collection<? extends V> coll) {
        this.qualifier = qualifier.of();
        this.value = coll.size() == 1 ? coll.iterator().next() : addAll(null, key(), coll);
    }

    public QualifiedSetImpl(SerializableFunction<V, K> qualifier, Object value) {
        this.qualifier = qualifier.of();
        this.value = value;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ qualifier.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && qualifier.equals(((QualifiedSetImpl<?, ?>) obj).qualifier);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected final SerializableFunction<V, Object> key() {
        return (SerializableFunction) qualifier;
    }

    @Override
    public Spliterator<V> spliterator() {
        return new DistinctCollectionSpliterator<>(value, 0, length(value), size(value), false);
    }

    @Override
    public Spliterator<V> reverseSpliterator() {
        return new DistinctCollectionSpliterator<>(value, 0, length(value), size(value), true);
    }

    @Override
    public QualifiedSet<K, V> add(V e) {
        return create(add(value, key(), e, key()));
    }

    @Override
    public QualifiedSet<K, V> put(V e) {
        return create(put(value, key(), e, key()));
    }

    @Override
    public QualifiedSet<K, V> removeKey(K k) {
        return create(remove(value, key(), k, identity()));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public QualifiedSet<K, V> removeAllKey(Collection<K> c) {
        return create(remove(value, key(), ((SetImpl) c.toSet()).value, identity()));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <X> QualifiedSet<K, V> removeAllKey(QualifiedSet<K, X> m) {
        return create(remove(value, key(), ((QualifiedSetImpl) m).value, key()));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public QualifiedSet<K, V> addAll(QualifiedSet<? extends K, ? extends V> c) {
        return create(add(value, key(), ((QualifiedSetImpl) c).value, key()));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public QualifiedSet<K, V> putAll(QualifiedSet<? extends K, ? extends V> c) {
        return create(put(value, key(), ((QualifiedSetImpl) c).value, key()));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void deduplicate(QualifiedSet<K, V> other) {
        deduplicate(value, key(), ((QualifiedSetImpl) other).value, key());
    }

    @Override
    public QualifiedSet<K, V> addAll(Collection<? extends V> e) {
        return addAll(e.toQualifiedSet(qualifier));
    }

    @Override
    public Collection<K> toKeys() {
        return map(qualifier);
    }

    @Override
    public V get(K k) {
        return get(value, key(), k);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Collection<V> getAll(Set<K> keys) {
        return create(retain(value, key(), ((SetImpl) keys).value, identity()));
    }

    @Override
    protected QualifiedSetImpl<K, V> create(Object val) {
        return val != value ? new QualifiedSetImpl<>(qualifier, val) : this;
    }

    @Override
    public QualifiedSet<K, V> merge(QualifiedSet<K, V>[] branches, int length) {
        return merge((k, v, vs, l) -> Mergeables.merge(v, vs, l), branches, length);
    }

    @Override
    public QualifiedSet<K, V> merge(QuadFunction<K, V, V[], Integer, V> merger, QualifiedSet<K, V>[] branches, int length) {
        return create(visit((a, l) -> {
            Object r = a[0];
            for (int i = 1; i < l; i++) {
                if (!Objects.equals(a[i], a[0]) && !Objects.equals(a[i], r)) {
                    if (!Objects.equals(a[0], r)) {
                        return merge(merger, a, l);
                    } else {
                        r = a[i];
                    }
                }
            }
            return r;
        }, branches, length));
    }

    @SuppressWarnings("unchecked")
    private Object merge(QuadFunction<K, V, V[], Integer, V> merger, Object[] es, int el) {
        K key = es[0] != null ? qualifier.apply((V) es[0]) : null;
        V v = (V) es[0];
        V[] vs = null;
        for (int i = 1; i < el; i++) {
            if (es[i] != null) {
                if (key == null) {
                    key = qualifier.apply((V) es[i]);
                }
                vs = ArrayUtil.set(vs, i - 1, (V) es[i], el - 1);
            }
        }
        V result = merger.apply(key, v, vs, el - 1);
        if (result == null) {
            return null;
        } else {
            for (int i = 0; i < el; i++) {
                if (es[i] != null && Objects.equals(result, es[i])) {
                    return es[i];
                }
            }
            return result;
        }
    }

    @Override
    public QualifiedSet<K, V> getMerger() {
        return create(null);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class<QualifiedSet> getMeetClass() {
        return QualifiedSet.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public QualifiedSet<K, V> remove(Object e) {
        K key = qualifier.apply((V) e);
        V val = get(key);
        return e.equals(val) ? removeKey(key) : this;
    }

    @Override
    public QualifiedSet<K, V> replace(Object pre, V post) {
        QualifiedSet<K, V> rem = remove(pre);
        return rem != this ? rem.add(post) : this;
    }

    @Override
    public QualifiedSet<K, V> removeAll(Collection<?> e) {
        @SuppressWarnings("resource")
        QualifiedSet<K, V> result = this;
        for (Object r : e) {
            result = result.remove(r);
        }
        return result;
    }

    @Override
    public SerializableFunction<V, K> qualifier() {
        return qualifier;
    }

    @Override
    public QualifiedSet<K, V> filter(Predicate<? super K> keyPredicate, Predicate<? super V> valuePredicate) {
        return filter(v -> keyPredicate.test(qualifier.apply(v)) && valuePredicate.test(v)).toQualifiedSet(qualifier);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean containsAll(Collection<?> c) {
        if (c instanceof QualifiedSetImpl) {
            return c.size() == size(retain(value, key(), ((QualifiedSetImpl) c).value, key()));
        } else {
            return containsAll(c.toQualifiedSet(qualifier));
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public QualifiedSet<K, V> exclusiveAll(Collection<? extends V> c) {
        if (c instanceof QualifiedSetImpl) {
            return create(exclusive(value, key(), ((QualifiedSetImpl) c).value, key()));
        } else {
            return exclusiveAll(c.toQualifiedSet(qualifier));
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public QualifiedSet<K, V> retainAll(Collection<?> c) {
        if (c instanceof QualifiedSetImpl) {
            return create(retain(value, key(), ((QualifiedSetImpl) c).value, key()));
        } else {
            return retainAll(c.toQualifiedSet(qualifier));
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        Serializer.wrap(s, this::javaSerialize);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        Deserializer.wrap(s, this::javaDeserialize);
    }

    @Override
    public void javaSerialize(Serializer s) {
        s.writeObject(qualifier.original());
        super.javaSerialize(s);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void javaDeserialize(Deserializer s) {
        qualifier = ((SerializableFunction<V, K>) s.readObject()).of();
        super.javaDeserialize(s);
    }

    @SuppressWarnings("unused")
    private void serialize(Serializer s) {
        s.writeObject(qualifier.original());
        s.writeInt(size());
        for (V e : this) {
            s.writeObject(e);
        }
    }

    @SuppressWarnings({"unchecked", "unused"})
    private static <K, V> QualifiedSetImpl<K, V> deserialize(Deserializer s) {
        SerializableFunction<V, K> f = (SerializableFunction<V, K>) ((SerializableFunction<V, K>) s.readObject()).original();
        V[] entries = (V[]) s.readArray(new Object[]{});
        return new QualifiedSetImpl<>(f, entries);
    }

    @Override
    public QualifiedSet<K, V> clear() {
        return create(null);
    }

}
