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

import java.io.IOException;
import java.io.Reader;

/**
 * Wrapper for a {@link Reader} that limits the amount of characters that are delivered.
 * Excess characters are ignored.
 */
public class LimitedReader extends Reader {
    private final Reader source;
    private long charactersLeft;

    public LimitedReader(Reader source, long maxCharacters) {
        this.source = source;
        charactersLeft = maxCharacters;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (charactersLeft == 0) {
            return -1;
        }
        int newLen = (int) Math.min(charactersLeft, len);
        int read = source.read(cbuf, off, newLen);
        if (read != -1) {
            charactersLeft -= read;
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}
