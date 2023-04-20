/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.netarchivesuite.solrwayback.util;

import java.util.function.Consumer;

/**
 * Wrapper for {@link Consumer} that takes a name and uses it in {@link #toString()}.
 * Intended for log messages and debugging.
 */
public class NamedConsumer<C> implements Consumer<C> {
    private final String name;
    private final Consumer<C> inner;

    public NamedConsumer(Consumer<C> inner, String name) {
        this.name = name;
        this.inner = inner;
    }

    @Override
    public void accept(C c) {
        inner.accept(c);
    }

    @Override
    public Consumer<C> andThen(Consumer<? super C> consumer) {
        return Consumer.super.andThen(consumer);
    }

    public String toString() {
        return "NamedConsumer(" + name + ")";
    }
}
