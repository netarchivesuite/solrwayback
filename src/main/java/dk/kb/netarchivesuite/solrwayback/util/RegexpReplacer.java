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

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies a given pattern to a given content. For each match of the pattern, the content of the first group is
 * transformed using the processor.
 */
public class RegexpReplacer implements UnaryOperator<String> {
    private static Log log = LogFactory.getLog(RegexpReplacer.class);

    private final Pattern pattern;
    private final UnaryOperator<String> processor;

    /**
     * Creates a replacer using the given pattern, which will be applied with {@link Pattern#DOTALL}. For each match,
     * the content of {@code group(1)} will be applied to the processor and the result will replace the original
     * content.
     * @param pattern   regular expression with at least 1 group.
     * @param processor processor for the content of the group.
     */
    public RegexpReplacer(String pattern, UnaryOperator<String> processor) {
        this.pattern = Pattern.compile(pattern, Pattern.DOTALL);
        this.processor = processor;
    }

    /**
     * Creates a replacer using the given pattern. For each match, the content of {@code group(1)} will be applied to
     * the processor and the result will replace the original content.
     * @param pattern   regular expression with at least 1 group.
     * @param processor processor for the content of the group.
     */
    public RegexpReplacer(Pattern pattern, UnaryOperator<String> processor) {
        if ((pattern.flags() & Pattern.DOTALL) == 0 && !pattern.pattern().startsWith("(?s)")) {
            log.debug("RegexpReplacer created with a pattern without DOT_ALL. This is most often an error. " +
                      "The suspicious pattern was '" + pattern.pattern() + "'");
        }
        this.pattern = pattern;
        this.processor = processor;
    }

    /**
     * Applies the given content to the pattern and the processor. If the processor returns null, the content is left
     * unchanged.
     * @param content any String to regexp replace.
     * @return the transformed content.
     */
    @Override
    public String apply(String content) {
        Matcher matcher = pattern.matcher(content);
        StringBuilder sb = new StringBuilder((int) (content.length() * 1.1));

        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(content, lastEnd, matcher.start(1));
            lastEnd = matcher.end(1);
            String newContent = processor.apply(matcher.group(1));
            sb.append(newContent == null ? matcher.group(1) : newContent);
        }
        sb.append(content, lastEnd, content.length());
        return sb.toString();
    }
}
