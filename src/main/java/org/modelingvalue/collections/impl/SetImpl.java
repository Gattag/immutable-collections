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
import java.util.function.Function;

import org.modelingvalue.collections.Collection;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableSet;
import org.modelingvalue.collections.util.Deserializer;
import org.modelingvalue.collections.util.Serializer;

public class SetImpl<T> extends HashCollectionImpl<T> implements Set<T> {

    private static final long serialVersionUID = 5749660673243596767L;
    @SuppressWarnings("rawtypes")
    public static final Set   EMPTY            = new SetImpl((Object) null);

    public SetImpl(T[] es) {
        this.value = es.length == 1 ? es[0] : addAll(null, key(), es);
    }

    public SetImpl(java.util.Collection<? extends T> es) {
        this.value = addAll(null, key(), es);
    }

    private SetImpl(Object value) {
        this.value = value;
    }

    @Override
    protected final Function<T, Object> key() {
        return identity();
    }

    @Override
    public Spliterator<T> spliterator() {
        return new DistinctCollectionSpliterator<>(value, 0, length(value), size(value), false);
    }

    @Override
    public Spliterator<T> reverseSpliterator() {
        return new DistinctCollectionSpliterator<>(value, 0, length(value), size(value), true);
    }

    @Override
    public Set<T> add(T e) {
        return create(add(value, key(), e, identity()));
    }

    @Override
    public Set<T> remove(Object e) {
        return create(remove(value, key(), e, identity()));
    }

    @Override
    public Set<T> replace(Object pre, T post) {
        Object rem = remove(value, key(), pre, identity());
        return rem != value ? create(add(rem, key(), post, identity())) : this;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void deduplicate(Set<T> other) {
        deduplicate(value, key(), ((SetImpl) other).value, key());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Set<T> retainAll(org.modelingvalue.collections.Collection<?> c) {
        if (c instanceof SetImpl) {
            return create(retain(value, key(), ((SetImpl) c).value, key()));
        } else {
            return retainAll(c.toSet());
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Set<T> exclusiveAll(org.modelingvalue.collections.Collection<? extends T> c) {
        if (c instanceof SetImpl) {
            return create(exclusive(value, key(), ((SetImpl) c).value, key()));
        } else {
            return exclusiveAll(c.toSet());
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Set<T> addAll(Collection<? extends T> c) {
        if (c instanceof SetImpl) {
            return c.isEmpty() ? this : create(add(value, key(), ((SetImpl) c).value, key()));
        } else {
            return addAll(c.toSet());
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Set<T> removeAll(Collection<?> c) {
        if (c instanceof SetImpl) {
            return create(remove(value, key(), ((SetImpl) c).value, key()));
        } else {
            return removeAll(c.toSet());
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean containsAll(Collection<?> c) {
        if (c instanceof SetImpl) {
            return c.size() == size(retain(value, key(), ((SetImpl) c).value, key()));
        } else {
            return containsAll(c.toSet());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SetImpl<T> create(Object val) {
        return val != value ? (val == null ? (SetImpl<T>) EMPTY : new SetImpl<>(val)) : this;
    }

    @Override
    public Set<T> merge(Set<T>[] branches, int length) {
        return create(visit((a, l) -> {
            for (int i = 1; i < l; i++) {
                if (!Objects.equals(a[i], a[0])) {
                    return a[i];
                }
            }
            return a[0];
        }, branches, length));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<T> getMerger() {
        return EMPTY;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class<Set> getMeetClass() {
        return Set.class;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        Serializer.wrap(s, this::javaSerialize);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        Deserializer.wrap(s, this::javaDeserialize);
    }

    @SuppressWarnings("unused")
    private void serialize(Serializer s) {
        s.writeInt(size());
        for (T e : this) {
            s.writeObject(e);
        }
    }

    @SuppressWarnings({"unchecked", "unused"})
    private static <T> SetImpl<T> deserialize(Deserializer s) {
        T[] entries = (T[]) s.readArray(new Object[]{});
        return entries.length == 0 ? (SetImpl<T>) SetImpl.EMPTY : new SetImpl<>(entries);
    }

    @Override
    public Set<T> clear() {
        return create(null);
    }

    @Override
    public java.util.Set<T> toMutable() {
        return new MutableSet<>(this);
    }

}
