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
package dk.kb.netarchivesuite.solrwayback.interfaces;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Encapsulates a supplier of data from a WARC- or ARC, independent of storage system.
 */
public class ArcSource implements Supplier<InputStream> {
    private final String source;
    private final Supplier<InputStream> supplier;

    /**
     * @param source the source (URL, file path or similar) of the ArcData.
     * @return a Supplier that delivers an InputStream for the (w)arc file, positioned at the beginning.
     */
    public ArcSource(String source, Supplier<InputStream> supplier) {
        this.source = source;
        this.supplier = supplier;
    }

    /**
     * @return the source of the data. Used for guessing the type of the source (warc/warc.gz/arc/arc.gz) by
     * looking at the last part of the String.
     */
    public String getSource() {
        return source;
    }

    @Override
    public InputStream get() {
        return supplier.get();
    }

    /**
     * @param file a file on the local file system.
     * @return an ArcSource for the given file.
     */
    public static ArcSource fromFile(String file) {
        return new ArcSource(file, () -> {
            try {
                return new FileInputStream(file);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create FileInputStream for '" + file + "'", e);
            }
        });
    }
}
