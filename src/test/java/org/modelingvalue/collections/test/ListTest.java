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

package org.modelingvalue.collections.test;

import org.junit.jupiter.api.*;
import org.modelingvalue.collections.Collection;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.*;

import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

public class ListTest {

    private static final int LONG = 10_000;

    @Test
    public void sorted1() {
        List<String> l = List.of("n", "k", "c", "y", "a", "b");
        assertEquals(List.of("a", "b", "c", "k", "n", "y"), l.sorted().toList());
    }

    @Test
    public void sorted2() {
        StringBuilder l1 = new StringBuilder();
        StringBuilder l2 = new StringBuilder();
        Collection.of("n", "k", "c", "y", "a", "b").sorted().forEach(l1::append);
        Collection.of("n", "k", "c", "y", "a", "b").sorted().toList().forEach(l2::append);

        assertEquals(l2.toString(), l1.toString());
    }

    @Test
    public void sorted3() {
        StringBuilder       l1 = new StringBuilder();
        StringBuilder       l2 = new StringBuilder();
        Collection.of("n", "k", "c", "y", "a", "b").toMap(c -> Entry.of(c, new Object())).toKeys().sorted().forEachOrdered(l1::append);
        Collection.of("n", "k", "c", "y", "a", "b").toMap(c -> Entry.of(c, new Object())).toKeys().sorted().toList().forEachOrdered(l2::append);

        assertEquals(l2.toString(), l1.toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void compare() {
        List<String> list1 = List.of("c", "d");
        List<String> list2 = list1.prependList(List.of("a", "b"));
        List<String> list3 = list1.appendList(List.of("e", "f"));
        list1.compareAll(list2, list3).forEachOrdered(a -> System.err.println(Arrays.toString(a)));
        System.err.println("...");
        List<String> lista = List.of("a", "b");
        List<String> listb = List.of("x", "y");
        List<String> listc = List.of("p", "q");
        lista.compareAll(listb, listc).forEachOrdered(a -> System.err.println(Arrays.toString(a)));
    }

    @Test
    public void reuseTest() {
        List<String[]> ab1 = List.of(new String[]{"a"}, new String[]{"b"});
        System.err.println(ab1);
        System.err.println(ab1.map(Object::hashCode).toList());
        List<String>   axb = List.of("b", "b");
        List<String[]> ab2 = axb.reuse(ab1, (t, s) -> t[0].equals(s), (t, s) -> t[0] = s, t -> 0L, (t, s) -> true, (s, i) -> new String[1]);
        System.err.println(ab2);
        System.err.println(ab2.map(Object::hashCode).toList());
    }

    static class MyClass {
        final int i;

        MyClass(int i) {
            this.i = i;
        }
    }

    @Test
    public void basicListOperations() {
        List<String> list1 = List.of("a", "b");
        List<String> list2 = List.of("a", "b");
        assertEquals(1, list1.filter(a -> a.equals("a")).size(), "list1 should contains one 'a'");

        List<String> list3 = list1.appendList(list2);
        assertEquals(2, list3.filter(a -> a.equals("a")).size(), "list3 should contains two 'a' ");

        Collection<String> list4 = list3.distinct();
        assertEquals(1, list4.filter(a -> a.equals("a")).size(), "list4 should contains one 'a' ");

        MyClass       c1    = new MyClass(1);
        MyClass       c2    = new MyClass(2);
        List<MyClass> list5 = List.of(c1, c2);
        assertEquals(1, list5.filter(a -> a.equals(c1)).size(), "list5 should contains c1 ");

        MyClass       c3    = new MyClass(3);
        List<MyClass> list6 = list5.insert(1, c3);
        assertEquals(c3, list6.get(1), "list6 should have c3 at index 1 ");
        Collection<MyClass> list7 = list6.appendList(list5).distinct();
        assertEquals(3, list7.size(), "list7 should have 3 elements ");

        assertEquals("a", list1.get(0), "list1 should 'a' at index 0 ");
        List<String> list8 = list1.replaceList(0, 0, List.of("c"));
        assertEquals(List.of("c", "a", "b"), list8, "list8 should be ['c', 'a', 'b']");
        List<String> list9 = list8.replaceList(1, 1, List.of("z"));
        assertEquals(List.of("c", "z", "a", "b"), list9, "list9 should be ['c' 'z','a','b']");
        List<String> list10 = list9.replaceList(1, 2, List.of("y"));
        assertEquals(List.of("c", "y", "a", "b"), list10, "list10 should be ['c' 'y','a','b']");

        Collection<Integer> list    = List.of(2);
        Collection<Integer> indexes = list10.indexesOfList(List.of("a")).toList();
        assertEquals(list, indexes, "list10 indexesOfList should be [2]");

    }

    @Test
    public void orderSpliteratorTest() {
        List<Integer> list1 = Collection.of(IntStream.range(0, 100000)).toList();
        List<Integer> list2 = list1.map(e -> 10).toList();
        List<Integer> list3 = Collection.of(IntStream.range(100000, 200000)).map(e -> 10).toList();
        assertEquals(list3.size(), list2.size());
        assertEquals(list2.hashCode(), list3.hashCode());
        assertEquals(list2, list3);

    }

    @Test
    public void random() {
        List<Integer> list = Collection.of(IntStream.range(0, 64)).toList();
        for (int i = 0; i < 10; i++) {
            System.err.println(list.random().toList());
        }
    }

    @Test
    public void reverse() {
        List<Integer> list1 = Collection.of(IntStream.range(0, 100000)).toList();
        List<Integer> list2 = list1.sorted((a, b) -> Integer.compare(b, a)).toList();
        List<Integer> list3 = list1.reverse().toList();
        assertEquals(list3.size(), list2.size());
        assertEquals(list2.hashCode(), list3.hashCode());
        assertEquals(list2, list3);
    }

    @SuppressWarnings("resource")
    @Test
    public void test() {
        List<String> list1 = List.of("a", "b", "c", "d");
        List<String> list2 = List.of("a", "b", "c", "d");
        List<String> list3 = List.of("d", "a", "b", "c");
        List<String> list4 = List.of("a", "b", "c", "d", "e", "f");
        assertEquals(4, list1.size());
        assertEquals(4, list2.size());
        assertEquals(list1.hashCode(), list2.hashCode());

        assertEquals(list3.get(3), list1.get(2));
        System.err.println(list4);
        assertEquals(list1, list2);
        assertNotEquals(list1.hashCode(), list3.hashCode());
        assertNotEquals(list1, list3);
        assertNotEquals(list3, list1);
        assertNotEquals(list1.hashCode(), list4.hashCode());
        assertNotEquals(list1, list4);
        assertNotEquals(list4, list1);

        List<String> list5 = list1.appendList(list3);
        List<String> list6 = list3.prependList(list1);
        assertEquals(8, list5.size());
        assertEquals(8, list6.size());
        assertEquals(list5.hashCode(), list6.hashCode());
        assertEquals(list5, list6);

        List<String> list7 = list1.appendList(list3).appendList(list1);
        List<String> list8 = list1.appendList(list1).insertList(4, list3);
        assertEquals(12, list7.size());
        assertEquals(12, list8.size());
        assertEquals(list7, list8);

        List<String> empty  = List.of();
        List<String> a      = empty.append("a");
        List<String> ab     = a.append("b");
        List<String> abc    = ab.append("c");
        List<String> abcd   = abc.append("d");
        List<String> abcde  = abcd.append("e");
        List<String> abcdef = abcde.append("f");
        assertEquals(abcdef, list4);

        assertEquals(List.of("b", "c", "d", "e", "f"), abcdef.removeFirst());
        assertEquals(abcde, abcdef.removeLast());

        assertEquals(List.of("d", "e", "f"), abcdef.removeAllFirst(3));
        assertEquals(abc, abcdef.removeAllLast(3));

        assertEquals(List.of("a", "b", "x", "d", "e", "f"), abcdef.replace(2, "x"));
        assertEquals(List.of("a", "b", "c", "x", "y", "z", "f"), abcdef.replaceList(3, 5, List.of("x", "y", "z")));

        assertEquals(4, list7.firstIndexOfList(list3));
        assertEquals(-1, list7.firstIndexOfList(List.of("b", "a")));
        assertEquals(-1, list7.firstIndexOfList(1, 5, ab));
        assertEquals(0, list7.firstIndexOfList(list1));
        assertEquals(8, list7.lastIndexOfList(list1));
        assertEquals(0, list7.firstIndexOfList(ab));
        assertEquals(5, list7.firstIndexOfList(1, 8, ab));
        assertEquals(8, list7.lastIndexOfList(ab));

        assertEquals(-1, list7.firstIndexOf(List.of("b", "a")));
        assertEquals(-1, list7.firstIndexOf(1, 5, "a"));
        assertEquals(1, list7.firstIndexOf("b"));
        assertEquals(0, list7.firstIndexOf("a"));
        assertEquals(8, list7.lastIndexOf("a"));
        assertEquals(0, list7.firstIndexOf("a"));
        assertEquals(5, list7.firstIndexOf(1, 8, "a"));
        assertEquals(8, list7.lastIndexOf("a"));

        assertEquals(List.of(2, 10), list7.indexesOfList(List.of("c", "d")).sorted().toList());

        List<String> bcde = abcdef.sublist(1, 5);
        assertEquals(List.of("b", "c", "d", "e"), bcde);

        List<String> list9 = abcdef.sublist(0, 6);
        assertEquals(list9, abcdef);

        List<String> af = abcdef.removeList(1, 5);
        assertEquals(af, List.of("a", "f"));

        List<String> abcef = abcdef.removeIndex(3);
        assertEquals(abcef, List.of("a", "b", "c", "e", "f"));

        List<Integer> longList1 = List.of();
        for (int i = 0; i < LONG; i++) {
            longList1 = longList1.append(i);
        }
        List<Integer> longList2 = List.of();
        for (int i = LONG - 1; i >= 0; i--) {
            longList2 = longList2.prepend(i);
        }
        List<Integer>    longList3 = List.of();
        Random           random    = new Random();
        HashSet<Integer> hashSet   = new HashSet<>();
        for (int i1 = 0; i1 < LONG; i1++) {
            int     v           = nextInt(random, hashSet);
            boolean higherFound = false;
            for (int i2 = 0; i2 < longList3.size(); i2++) {
                if (longList3.get(i2) > v) {
                    longList3   = longList3.insert(i2, v);
                    higherFound = true;
                    break;
                }
            }
            if (!higherFound) {
                longList3 = longList3.append(v);
            }
        }
        assertEquals(longList1.size(), LONG);
        assertEquals(longList2.size(), LONG);
        assertEquals(longList3.size(), LONG);
        assertEquals(longList1, longList2);
        assertEquals(longList1, longList3);
        assertEquals(longList3, longList2);
        assertEquals(longList3, longList1);
        for (int i = 0; i < LONG; i++) {
            assertEquals(longList1.get(i), (Integer) i);
            assertEquals(longList2.get(i), (Integer) i);
            assertEquals(longList3.get(i), (Integer) i);
        }

        for (int t = 0; t < 100; t++) {
            int           shift     = random.nextInt(LONG / 10);
            List<Integer> longList4 = longList1.sublist(LONG / 4 + shift, LONG - (LONG / 4) - shift);
            assertEquals(longList4.size(), LONG / 2 - 2 * shift);
            for (int i = 0; i < LONG / 2 - 2 * shift; i++) {
                assertEquals(longList4.get(i), (Integer) (i + LONG / 4 + shift));
            }
        }
    }

    private static int nextInt(Random random, HashSet<Integer> hashSet) {
        int r = random.nextInt(LONG);
        while (!hashSet.add(r)) {
            r = random.nextInt(LONG);
        }
        return r;
    }
}
