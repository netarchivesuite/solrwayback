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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

/**
 * Pipes streamed output from a provider to an InputStream.
 */
public class StreamBridge {
    private static final Logger log = LoggerFactory.getLogger(StreamBridge.class);

    // An unbounded executor to avoid deadlocks between multiple concurrent calls to outputToInput
    private static int threadID = 0;
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        log.debug("Creating StreamBridge thread #" + threadID);
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
        return outputToInput(Collections.singletonList(provider));
    }

    /**
     * The providers are responsible for adding content to the provided OutputStream. The added content will be
     * available in the form of the returned InputStream.
     *
     * The providers will be called inside of a Thread and the coupling of the OutputStream and the InputStream will be
     * buffer-based, so there is low memory overhead, no matter how much content is produced.
     *
     * The stream will be automatically closed after the producer has finished processing or if processing fails.
     *
     * Important: This method uses a thread from {@link #executor}. Do not make thousands of call to this method without
     * ensuring that the InputStreams from previous calls has been depleted. The number of providers in a single call
     * has no limit per se.
     * @param providers the providers of the bytes to pipe to the returned InputStream.
     * @return an InputStream which will be populated with data from the provider.
     */
    public static InputStream outputToInput(Collection<Consumer<OutputStream>> providers) throws IOException {
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream(out);

        List<Consumer<OutputStream>> providerList = new ArrayList<>(providers);
        log.debug("Received " + providers.size() + " providers");
        OutputStream noCloseOut = new NonClosingOutputStream(out); // The sub-streams are concatenated

        executor.submit(() -> {
            for (int i = 0 ; i < providerList.size() ; i++) {
                log.debug(String.format(Locale.ENGLISH, "Activating provider #%d/%d", (i + 1), providers.size()));
                try {
                    providerList.get(i).accept(noCloseOut);
                } catch (Exception e) {
                    log.warn(String.format(
                            Locale.ENGLISH, "outputToInput: Exception calling accept on sub-provider #%d/%d",
                            i+1, providerList.size()));
                }
                log.debug(String.format(Locale.ENGLISH, "Finished provider #%d/%d", (i + 1), providers.size()));
            }
            //providers.forEach(provider -> provider.accept(noCloseOut));
            try {
                log.debug("Flushing PipedOutputStream");
                out.flush();
                log.debug("Closing PipedOutputStream (only affects current result block)");
                out.close();
            } catch (IOException e) {
                log.error("IOException closing piped stream", e);
            }
        });
        return in;
    }

    /**
     * Wrapper for an Outputstream that ignores calls to {@link OutputStream#close()}.
     * Used for ensuring that sub-providers in a list of providers does not close the overall OutputStream.
     */
    private static class NonClosingOutputStream extends FilterOutputStream {
        public NonClosingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            // no-op
        }
    }

    /**
     * Makes the provider output a gzip stream.
     * @param provider any provider.
     * @return the given provider wrapped in a gzipping provider.
     */
    public static Consumer<OutputStream> gzip(Consumer<OutputStream> provider) {
        return out -> {
            try {
                log.debug("Creating entry-level gzip stream");
                GZIPOutputStream gzip = new GZIPOutputStream(out);
                provider.accept(gzip);
                log.debug("Closing entry-level gzip stream");
                gzip.close();
            } catch (IOException e) {
                log.error("IOException with gzip", e);
                throw new RuntimeException("IOException with gzip", e);
            }
        };
    }
}
