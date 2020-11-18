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

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * Stream-wrapper with status for the content.
 */
public class StatusInputStream extends FilterInputStream {
    public enum STATUS {ok, exception, empty}

    private final STATUS status;
    private final Exception exception;
    private final long size;

    public StatusInputStream(InputStream in, STATUS status, long expectedSize) {
        super(in);
        this.status = status;
        this.exception = null;
        this.size = expectedSize;
    }

    public StatusInputStream(InputStream in, Exception exception, long expectedSize) {
        super(in);
        this.status = STATUS.exception;
        this.exception = exception;
        this.size = expectedSize;
    }

    public STATUS getStatus() {
        return status;
    }

    public Exception getException() {
        return exception;
    }

    public long size() {
        return size;
    }
}
