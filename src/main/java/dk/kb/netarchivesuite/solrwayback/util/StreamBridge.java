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

import javax.security.auth.login.AccountLockedException;
import java.io.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

/**
 * Pipes streamed output from a provider to an InputStream.
 */
public class StreamBridge {
    private static final Logger log = LoggerFactory.getLogger(StreamBridge.class);

    // We use an unbounded executor to avoid deadlocks between multiple concurrent calls to outputToInput
    private static int threadID = 0;
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "StreamBridge_" + threadID++);
        t.setDaemon(true);
        return t;
    });

    /**
     * The provider is responsible for adding content to the provided OutputStream. The added content will be available
     * in the form of the returned InputStream.
     *
     * The provider will be called inside of a Thread and the coupling of the OutputStream and the InputStream will be
     * buffer-based, so there is low memory overhead, no matter how much content is produced.
     *
     * The stream will be automatically closed after the producer has finished processing.
     *
     * Important: This method uses a thread from {@link #executor}. Do not make thousands of call to this method without
     * ensuring that the InputStreams from previous calls has been depleted.
     * @param provider the provider of the bytes to pipe to the returned InputStream.
     * @return an InputStream which will be populated with data from the provider.
     */
    public static InputStream outputToInput(Consumer<OutputStream> provider) throws IOException {
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream(out);
        executor.submit(() -> {
            provider.accept(out);
            try {
                out.close();
            } catch (IOException e) {
                log.error("IOException closing piped stream", e);
            }
        });
        return in;
    }

    /**
     * The provider is responsible for adding content to the provided OutputStream. The added content will be available
     * in the form of the returned InputStream, which will automatically be gzipped.
     *
     * The provider will be called inside of a Thread and the coupling of the OutputStream and the InputStream will be
     * buffer-based, so there is low memory overhead, no matter how much content is produced.
     *
     * The stream will be automatically closed after the producer has finished processing.
     *
     * Important: This method uses a thread from {@link #executor}. Do not make thousands of call to this method without
     * ensuring that the InputStreams from previous calls has been depleted.
     * @param provider the provider of the bytes to pipe to the returned InputStream.
     * @return an InputStream which will be populated with data from the provider and gzipped.
     */
    public static InputStream outputToGzipInput(Consumer<OutputStream> provider) throws IOException {
        return outputToInput(out -> {
            try {
                GZIPOutputStream gzip = new GZIPOutputStream(out);
                provider.accept(gzip);
                gzip.close();
            } catch (IOException e) {
                throw new RuntimeException("IOException with gzip", e);
            }
        });
    }
}
