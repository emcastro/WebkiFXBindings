package com.github.emcastro.webkitfxproxy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSArray<A> extends Iterable<A> {

    @ArrayGetter
    A get(int index);

    @ArraySetter
    void set(int index, A value);

    @Getter
    int length();

    @Override
    default Iterator<A> iterator() {
        return new Iterator<A>() {
            int length = length();
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < length;
            }

            @Override
            public A next() {
                return get(i++);
            }
        };
    }

    @Override
    default void forEach(Consumer<? super A> action) {
        int length = length();
        for (int i = 0; i < length; i++) {
            action.accept(get(i));
        }
    }

    default ArrayList<A> toList() {
        int length = length();
        ArrayList<A> list = new ArrayList<>(length);

        forEach(list::add);

        return list;
    }

    default Stream<A> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    default Spliterator<A> spliterator() {
        return Spliterators.spliterator(iterator(), length(), Spliterator.ORDERED);
    }
}
