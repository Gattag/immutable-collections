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

import java.util.function.Function;

import org.modelingvalue.collections.impl.SetImpl;
import org.modelingvalue.collections.mutable.MutableSet;
import org.modelingvalue.collections.util.Mergeable;

public interface Set<T> extends ContainingCollection<T>, Mergeable<Set<T>> {
    @SuppressWarnings("unchecked")
    static <T> Set<T> of() {
        return SetImpl.EMPTY;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <T> Set<T> of(T... elements) {
        return elements.length == 0 ? SetImpl.EMPTY : new SetImpl<>(elements);
    }

    @SafeVarargs
    static <T> Set<T> notNull(T... elements) {
        Set<T> result = Set.of();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] != null) {
                result = result.add(elements[i]);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static <F, T> Set<T> of(Function<F, T> function, F[] f) {
        T[] e = (T[]) new Object[f.length];
        for (int i = 0; i < e.length; i++) {
            e[i] = function.apply(f[i]);
        }
        return e.length == 0 ? SetImpl.EMPTY : new SetImpl<>(e);
    }

    @SuppressWarnings("unchecked")
    static <T> Set<T> of(java.util.Collection<? extends T> coll) {
        return coll.isEmpty() ? SetImpl.EMPTY : new SetImpl<>(coll);
    }

    boolean containsAll(Collection<?> c);

    @Override
    Set<T> replace(Object pre, T post);

    @Override
    Set<T> add(T e);

    @Override
    Set<T> remove(Object e);

    @Override
    Set<T> addAll(Collection<? extends T> c);

    Set<T> exclusiveAll(Collection<? extends T> c);

    @Override
    Set<T> removeAll(Collection<?> c);

    Set<T> retainAll(Collection<?> c);

    void deduplicate(Set<T> other);

    @Override
    default Set<T> toSet() {
        return this;
    }

    @Override
    Set<T> clear();

    java.util.Set<T> toMutable();

    static <E> Set<E> fromMutable(java.util.Collection<E> mutable) {
        return mutable instanceof MutableSet ? ((MutableSet<E>) mutable).toImmutable() : Collection.of(mutable).toSet();
    }

}
