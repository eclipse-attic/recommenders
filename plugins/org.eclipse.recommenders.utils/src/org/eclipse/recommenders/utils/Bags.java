package org.eclipse.recommenders.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Ordering;

public class Bags {

    private static final Comparator<Entry<?>> BY_COUNT = new Comparator<Multiset.Entry<?>>() {
        @Override
        public int compare(Entry<?> o1, Entry<?> o2) {
            return ComparisonChain.start().compare(o1.getCount(), o2.getCount())
                    .compare(o1.getElement().toString(), o2.getElement().toString()).result();
        }
    };

    private static final Comparator<Entry<?>> BY_STRING = new Comparator<Multiset.Entry<?>>() {
        @Override
        public int compare(Entry<?> o1, Entry<?> o2) {
            return ComparisonChain.start().compare(o1.getElement().toString(), o2.getElement().toString())
                    .compare(o1.getCount(), o2.getCount()).result();
        }
    };

    public static <T> List<Entry<T>> topUsingCount(Multiset<T> set, int i) {
        Set<Entry<T>> entries = set.entrySet();
        return Ordering.from(BY_COUNT).greatestOf(entries, i);
    }

    public static <T> List<Entry<T>> topUsingToString(Multiset<T> set, int i) {
        Set<Entry<T>> entries = set.entrySet();
        return Ordering.from(BY_STRING).greatestOf(entries, i);
    }

}
