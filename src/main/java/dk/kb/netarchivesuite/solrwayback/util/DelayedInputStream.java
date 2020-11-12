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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * InputStream where the content is produced by a supplier upon first call to any methods.
 *
 * Important: Exceptions thrown while the {@link #streamSupplier} is evaluated are caught and a 0-byte Inputstream
 * is used instead.
 */
public class DelayedInputStream extends InputStream {
    private static final Logger log = LoggerFactory.getLogger(DelayedInputStream.class);

    private InputStream inner = null;
    private final Supplier<InputStream> streamSupplier;
    private Exception e = null;

    public DelayedInputStream(Supplier<InputStream> streamSupplier) {
        this.streamSupplier = streamSupplier;
    }

    /**
     * Call {@link #streamSupplier} if it hasn't already been done.
     */
    private void ensureStream() {
        if (inner == null) {
            try {
                inner = streamSupplier.get();
            } catch (Exception e) {
                log.error("Exception evaluating lambda. Returning empty stream", e);
                this.e = e;
                inner = new ByteArrayInputStream(new byte[0]);
            }
        };
    }

    /**
     * @return an exception if it was thrown by evaluating {@link #streamSupplier}.
     */
    public Exception getException() {
        return e;
    }

    /**
     * @return true if {@link #streamSupplier} has been called.
     */
    public boolean hasEvaluated() {
        return inner != null;
    }

    /**
     * @return the inner stream constructed by {@link #streamSupplier}.
     */
    public InputStream getInner() {
        ensureStream();
        return inner;
    }

    /* Delegates */
    
    @Override
    public int read() throws IOException {
        ensureStream();
        return inner.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        ensureStream();
        return inner.read(b);    // TODO: Implement this
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureStream();
        return inner.read(b, off, len);    // TODO: Implement this
    }

    @Override
    public long skip(long n) throws IOException {
        ensureStream();
        return inner.skip(n);    // TODO: Implement this
    }

    @Override
    public int available() throws IOException {
        ensureStream();
        return inner.available();    // TODO: Implement this
    }

    @Override
    public void close() throws IOException {
        ensureStream();
        inner.close();    // TODO: Implement this
    }

    @Override
    public synchronized void mark(int readlimit) {
        ensureStream();
        inner.mark(readlimit);    // TODO: Implement this
    }

    @Override
    public synchronized void reset() throws IOException {
        ensureStream();
        inner.reset();    // TODO: Implement this
    }

    @Override
    public boolean markSupported() {
        ensureStream();
        return inner.markSupported();    // TODO: Implement this
    }
}
