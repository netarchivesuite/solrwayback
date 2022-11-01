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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
     * A "safe" version of {@link #outputToInput(Consumer)} where all OutputStream Exceptions are caught and rethrown
     * as RuntimeExceptions. Intended for Stream-oriented processing.
     *
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
    public static InputStream outputToInputSafe(Consumer<SafeOutputStream> provider) throws IOException {
        return outputToInputSafe(Collections.singletonList(provider));
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
        OutputStream noCloseOut = new NonClosingOutputStream(out); // The sub-streams are concatenated

        // Iterate all providers, sending their content to the piped stream
        // If a provider fails, the Exception is logged and processing continues with the next provider
        executor.submit(() -> {
            for (int i = 0 ; i < providerList.size() ; i++) {
                try {
                    providerList.get(i).accept(noCloseOut);
                } catch (Exception e) {
                    log.warn(String.format(
                            Locale.ENGLISH, "outputToInput: Exception calling accept on sub-provider #%d/%d. " +
                                            "Switching to next sub-provider",
                            i+1, providerList.size()), e);
                }
            }
            //providers.forEach(provider -> provider.accept(noCloseOut));
            try {
                out.flush();
                out.close();
            } catch (IOException e) {
                log.error("IOException closing piped stream", e);
            }
        });
        return in;
    }

    /**
     * A "safe" version of {@link #outputToInput(Consumer)} where all OutputStream Exceptions are caught and rethrown
     * as RuntimeExceptions. Intended for Stream-oriented processing.
     *
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
    public static InputStream outputToInputSafe(Collection<Consumer<SafeOutputStream>> providers) throws IOException {
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream(out);
        ExceptionCatchingInputStream ein = new ExceptionCatchingInputStream(in);

        List<Consumer<SafeOutputStream>> providerList = new ArrayList<>(providers);
        OutputStream noCloseOut = new NonClosingOutputStream(out); // The sub-streams are concatenated
        SafeOutputStream safeOut = new SafeOutputStream(noCloseOut);

        // Iterate all providers, sending their content to the piped stream
        // If a provider fails, the Exception is logged and processing continues with the next provider
        executor.submit(() -> {
            for (int i = 0 ; i < providerList.size() ; i++) {
                try {
                    providerList.get(i).accept(safeOut);
                } catch (Exception e) {
                    String message = String.format(
                            Locale.ENGLISH, "outputToInput: Exception calling accept on sub-provider #%d/%d",
                            i+1, providerList.size());
                    log.warn(message, e);
                    // Next read call on the returned InputStream will throw this Exception
                    ein.setException(new IOException(message, e));
                    break;
                }
            }
            //providers.forEach(provider -> provider.accept(noCloseOut));
            try {
                out.flush();
                out.close();
            } catch (IOException e) {
                log.error("IOException closing piped stream", e);
            }
        });
        return ein;
    }

    /**
     * Wrapper that throws an assigned IOException on reads. Used to propagate Exceptions using {@link PipedInputStream}
     * and {@link PipedOutputStream}.
     */
    @SuppressWarnings("NullableProblems")
    private static final class ExceptionCatchingInputStream extends FilterInputStream {
        private IOException e;
        public ExceptionCatchingInputStream(InputStream inputStream) {
            super(inputStream);
        }

        /**
         * After setting an exception it will be thrown on calls to any read method.
         * @param e an IOException to be propagated.
         */
        public void setException(IOException e) {
            this.e = e;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (e != null) { // This check must be after the call to super to ensure happens-before
                throw e;
            }
            return b;
        }

        @Override
        public int read(byte[] bytes) throws IOException {
            int read = super.read(bytes);
            if (e != null) { // This check must be after the call to super to ensure happens-before
                throw e;
            }
            return read;
        }

        @Override
        public int read(byte[] bytes, int i, int i1) throws IOException {
            int read = super.read(bytes, i, i1);    // TODO: Implement this
            if (e != null) { // This check must be after the call to super to ensure happens-before
                throw e;
            }
            return read;
        }

        @Override
        public long skip(long l) throws IOException {
            long skipped = super.skip(l);
            if (e != null) { // This check must be after the call to super to ensure happens-before
                throw e;
            }
            return skipped;
        }

        @Override
        public int available() throws IOException {
            int available = super.available();
            if (e != null) { // This check must be after the call to super to ensure happens-before
                throw e;
            }
            return available;
        }
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
     * Wrapper for an OutputStream where all exceptions are catched and rethrown as RuntimeExceptions.
     *
     * Intended for use with Streams.
     */
    public static class SafeOutputStream extends OutputStream {
        final OutputStream inner;

        public SafeOutputStream(OutputStream inner) {
            this.inner = inner;
        }

        /**
         * Convenience method for writing the given String s as UTF-8 bytes.
         * @param s any String.
         */
        public void write(String s) {
            write(s.getBytes(StandardCharsets.UTF_8));
        }

        /**
         * Convenience method for writing the given String s as UTF-8 bytes followed by newline {@code \n}.
         * @param s any String.
         */
        public void writeln(String s) {
            write(s.getBytes(StandardCharsets.UTF_8));
            write(LN);
        }
        private static final byte[] LN = "\n".getBytes(StandardCharsets.UTF_8);

        @Override
        public void write(int b) {
            try {
                inner.write(b);
            } catch (IOException e) {
                throw new RuntimeException("IOException while writing integer " + b, e);
            }
        }

        @Override
        public void write(byte[] b) {
            try {
                inner.write(b);
            } catch (IOException e) {
                throw new RuntimeException("IOException while writing byte array", e);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            try {
                inner.write(b, off, len);
            } catch (IOException e) {
                throw new RuntimeException("IOException while writing byte array", e);
            }
        }

        @Override
        public void flush() {
            try {
                inner.flush();
            } catch (IOException e) {
                throw new RuntimeException("IOException while calling flush()", e);
            }
        }

        @Override
        public void close() {
            try {
                inner.close();
            } catch (IOException e) {
                throw new RuntimeException("IOException while calling close()", e);
            }
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
                GZIPOutputStream gzip = new GZIPOutputStream(out);
                provider.accept(gzip);
                gzip.close();
            } catch (IOException e) {
                log.error("IOException with gzip", e);
                throw new RuntimeException("IOException with gzip", e);
            }
        };
    }

    /**
     * Retrieves the content from the given {@code is} and stores it in a cache, handling Exceptions by marking them in
     * the returned {@link StatusInputStream}. Guaranteed to return a StatusInputStream, unless it runs out of heap or
     * storage space.
     *
     * This method does a best effort attempt to read as much as possible, keeping the bytes received from is if an
     * Exception is raised.
     * @param is stream with potential problems.
     * @param heapBuffer the maximum number of bytes to hold in memory.
     * @return a stream with the content from {@code is}.
     */
    public static StatusInputStream guaranteedStream(InputStream is, int heapBuffer) {
        final byte[] buf = new byte[GUARANTEED_BUFFER];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int read = 0;

        // Read up to heapBuffer bytes onto the heap
        while (read < heapBuffer+1) { // +1 to match the case where heapBuffer = stream size
            try {
                int r = is.read(buf, 0, Math.min(buf.length, heapBuffer+1-read));
                if (r == -1) {
                    // All OK
                    safeClose(is);
                    return new StatusInputStream(
                            new ByteArrayInputStream(bos.toByteArray()), StatusInputStream.STATUS.ok, read);
                } else {
                    bos.write(buf, 0, r);
                }
                read += r;
            } catch (IOException e) {
                // Fail read
                log.info("guaranteedStream: Exception reading from input stream", e);
                return new StatusInputStream(new ByteArrayInputStream(bos.toByteArray()), e, read);
            }
        }

        // Content exceeds heapBuffer. Switch to storage based buffering
        return guaranteedStream(is, bos.toByteArray());
    }

    /**
     * Copies the content of {@code alreadyRead} into a temporary file, followed by the remaining content in {@code is}.
     * An InputStream is returned that delivers the bytes from {@code alreadyRead} + {@code is}.
     * Any Exceptions encountered during the process are caught and stored in the returne StatusInputStream.
     * @param is a potentially unreliable InputStream.
     * @param alreadyRead bytes already read. Can be null.
     * @return
     */
    private static StatusInputStream guaranteedStream(InputStream is, byte[] alreadyRead) {
        if (alreadyRead == null) {
            alreadyRead = new byte[0];
        }

        // Create a temporary file
        final File tmp;
        FileOutputStream fos;
        try {
            tmp = File.createTempFile("solrwayback.guaranteedStream", "dat");
            tmp.deleteOnExit(); // In case is was not deleted on close
            fos = new FileOutputStream(tmp);
        } catch (IOException e) {
            log.error("Unable to create temporary file", e);
            return new StatusInputStream(new ByteArrayInputStream(alreadyRead), e, alreadyRead.length);
        }

        // Copy alreadyRead to the file
        try {
            IOUtils.copy(new ByteArrayInputStream(alreadyRead), fos);
        } catch (IOException e) {
            safeClose(is);
            closeAndDelete(tmp, fos);
            log.warn("guaranteedStream: Unable to copy heap cached buffer of size " + alreadyRead.length +
                     " to temp file " + tmp, e);
            return new StatusInputStream(new ByteArrayInputStream(alreadyRead), e, alreadyRead.length);
        }

        // Retrieve the rest of the bytes from is and store them in the file
        long readSecond;
        try {
            readSecond = IOUtils.copy(is, fos);
            fos.close();
        } catch (IOException e) {
            safeClose(is);
            closeAndDelete(tmp, fos);
            log.warn("guaranteedStream: Unable to copy content from the privided InputStream to temp file " + tmp, e);
            return new StatusInputStream(new ByteArrayInputStream(alreadyRead), e, alreadyRead.length);
        }

        // Create an InputStream from the file
        FileInputStream fis;
        try {
            fis = new FileInputStream(tmp);
        } catch (FileNotFoundException e) {
            log.warn("guaranteedStream: Unable to construct FileInputStream(" + tmp + ")", e);
            return new StatusInputStream(new ByteArrayInputStream(alreadyRead), e, alreadyRead.length);
        }

        // Return the Inputstream with the full content, wrapped in a StatusInputstream that deletes the temporary
        // file when closed.
        return new StatusInputStream(fis, StatusInputStream.STATUS.ok, alreadyRead.length + readSecond) {
            @Override
            public void close() throws IOException {
                super.close();
                try {
                    Files.delete(tmp.toPath());
                } catch (IOException e) {
                    log.warn("guaranteedStream: Non-critical exception while deleting '" + tmp + "'", e);
                }
            }
        };
    }

    private final static int GUARANTEED_BUFFER = 1024; // Not too large as we want what we can get

    /**
     * Close {@code fos} and delete {@code file}, catching and logging any thrown Exceptions.
     * @param file a file that was the source of {@code fos}.
     * @param fos a FileOutputStream created form {@code file}.
     */
    private static void closeAndDelete(File file, FileOutputStream fos) {
        try {
            fos.close();
        } catch (Exception e) {
            log.warn("closeAndDelete: Non-critical exception while closing FileOutputStream(" + file + ")", e);
        }
        try {
            Files.delete(file.toPath());
        } catch (Exception e) {
            log.warn("closeAndDelete: Non-critical exception while deleting '" + file + "'", e);
        }
    }

    /**
     * Close {@code is}, catching and logging any Exceptions.
     * @param is any InputStream.
     */
    public static void safeClose(InputStream is) {
        try {
            is.close();
        } catch (Exception e) {
            log.warn("close: Non-critical exception while closing InputStream", e);
        }
    }


    /**
     * Construct an InputStream that is the concatenation of all the given {@code is}s.
     * @param is 0 or more InputStreams.
     * @return a delayed concatenation of the given InputStreams og null if there are no InputStreams.
     */
    public static InputStream concat(InputStream... is) {
        if (is.length == 0) {
            return null;
        }
        InputStream result = is[0];
        for (int i = 1 ; i < is.length ; i++) {
            result = new SequenceInputStream(result, is[i]);
        }
        return result;
    }

}
