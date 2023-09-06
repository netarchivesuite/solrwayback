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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Collects different types of messages or exceptions, intended for later inspection through webservice calls or
 * in the SolrWayback GUI.
 * <p>
 * Singleton.
 */
public class MessageBridge {
    private static final Logger log = LoggerFactory.getLogger(MessageBridge.class);

    // TODO: Consider making this a property
    public static final int MAX_MESSAGES_PER_TYPE = 100;

    private static final Map<String, List<Message>> messageMap = new HashMap<>();
    private static final Map<String, Long> messageCounter = new HashMap<>();

    /**
     * When adding messages, the contract is that there must be "few" unique types (< 50 or so) while there can
     * be an unlimited number of unique messages as only the last {@link #MAX_MESSAGES_PER_TYPE} are retained.
     * @param type    the overall type of the message, e.g. {@code "warc-access"}.
     * @param message the specific message, e.g. {@code "Unable to locate WARC-file /warcs/2002/foo.warc.gz}.
     */
    public static synchronized void addMessage(String type, String message) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must be defined");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message must be defined");
        }

        List<Message> messages = messageMap.get(type);
        if (messages == null) {
            messages = new ArrayList<>();
            messageMap.put(type, messages);
            messageCounter.put(type, 0L);
        }
        messageCounter.put(type, messageCounter.get(type)+1);
        messages.add(new Message(type, message));
        if (messages.size() > MAX_MESSAGES_PER_TYPE) {
            messages.remove(0); // This will be an array copy - plenty fast for small arrays, no need for LinkedList
        }
    }

    /**
     * Encapsulates a message consisting of a timestamp, a type and a Human readable String.
     */
    public static class Message {
        public final long creationTime = System.currentTimeMillis();
        public final String type;
        public final String message;

        /**
         * @param type    the overall type of the message, e.g. {@code "warc-access"}.
         * @param message the specific message, e.g. {@code "Unable to locate WARC-file /warcs/2002/foo.warc.gz}.
         */
        public Message(String type, String message) {
            this.type = type;
            this.message = message;
        }

        public String toString() {
            return "Message(created=" + getCreationTimeISO() + ", type='" + type + "', message='" + message + "')";
        }

        private String getCreationTimeISO() {
            // TODO: Does it make sense to timezone this? Wouldn't this always be the local timezone for easy reading?
            Instant dateTime = Instant.ofEpochSecond(creationTime, 0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);
            return formatter.format(dateTime);
        }
    }

}
