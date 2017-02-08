package dk.kb.netarchivesuite.solrwayback.parsers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProximityHtmlParser {
    private static final Logger log = LoggerFactory.getLogger(ProximityHtmlParser.class);

    public static final double NO_TERMS_WEIGHT = 0.25d;

    // http://www.w3schools.com/tags/tag_base.asp
    private static Pattern basePattern = Pattern.compile("<base[^>]*href=\"([^\"]+)\"");
    // http://www.w3schools.com/tags/tag_img.asp
    private static Pattern imgPattern = Pattern.compile("<img[^>]*src=\"([^\"]+)\"");

    /**
     * Get a prioritized list of images contained in the given htmlString.
     * @param url         the full origin of the htmlString.
     * @param baseScore   this will be multiplied to all weights.
     * @param htmlString  html. Need not be well-formed.
     * @param queryTerms  Optional set of query terms. Can be null.
     * @param maxDistance the maximum distance measured in images from a term, in order for the image to be included
     *                    in the result set.
     * @return images from the page.
     */
    public static List<WeightedImage> getImageUrls(
            URL url, double baseScore, String htmlString, Set<String> queryTerms, int maxDistance){
        if (queryTerms == null) {
            queryTerms = Collections.emptySet();
        }
        URL base = null;
        List<Token> tokens = new ArrayList<>();

        Matcher matcher = createAllPattern(queryTerms).matcher(htmlString);
        boolean anyTermMatch = false;
        while (matcher.find()) {
            if (matcher.group(1) != null) { // base
                try {
                    base = new URL(matcher.group(1));
                } catch (Exception e) {
                    log.warn("Exception URI-parsing base '" + matcher.group(1) + "' for HTML at '" + url
                             + "'. Base will be undefined and image URIs resolved relative to HTML URI");
                }

            } else if (matcher.group(2) != null) {
                tokens.add(new Token(TYPE.img, matcher.group(2)));
            } else if (matcher.group(3) != null) {
                tokens.add(new Token(TYPE.term, matcher.group(3)));
                anyTermMatch = true;
            }
        }


        if (tokens.isEmpty()) {
            log.debug("No matches for '" + url + "'");
            return Collections.emptyList();
        }
        // Note: If no matching terms were found at all, act af if there were no query terms at all
        List<WeightedImage> images = tokensToImages(
                base == null ? url : base, baseScore, tokens,
                anyTermMatch ? queryTerms : Collections.<String>emptySet(),
                maxDistance);
        Collections.sort(images);
        return images;
    }

    private static List<WeightedImage> tokensToImages(
            URL url, double baseScore, List<Token> tokens, Set<String> queryTerms, int maxDistance) {
        List<WeightedImage> images = new ArrayList<>();
        for (int i = 0 ; i < tokens.size() ; i++) {
            if (tokens.get(i).type == TYPE.img) {
                String imgSrc = tokens.get(i).content;
                try {
                    if (queryTerms.isEmpty()) {
                        images.add(new WeightedImage(resolveURL(url, imgSrc), null, baseScore*NO_TERMS_WEIGHT));
                    } else {
                        Set<String> nearTerms = new HashSet<>();
                        double weight = baseScore*(enrich(tokens, i, -1, nearTerms, maxDistance)
                                                   + enrich(tokens, i, 1, nearTerms, maxDistance));
                        if (weight > 0.000001) { // Only add images near enough to a term to get a score
                            images.add(new WeightedImage(resolveURL(url, imgSrc), nearTerms, weight));
                        }
                    }
                } catch (MalformedURLException e) {
                    log.debug("Unable to resolve image URL with base '" + url + "' and img src '" + imgSrc);
                }
            }
        }
        return images;
    }

    static URL resolveURL(URL base, String local) throws MalformedURLException {
        return new URL(base, local);
    }

    private static double enrich(List<Token> tokens, int origo, int direction, Set<String> nearTerms, int maxDistance) {
        final int origMaxDistance = maxDistance;
        double weight = 0;
        for (int i = origo + direction ; i >= 0 && i < tokens.size() && maxDistance > 0 ; i += direction) {
            switch (tokens.get(i).type) {
                case img:
                    maxDistance--;
                    break;
                case term:
                    nearTerms.add(tokens.get(i).content);
                    weight += 1d/(origMaxDistance-maxDistance+1);
                    break;
                default: throw new UnsupportedOperationException("Unknown type " + tokens.get(i).type);
            }
        }
        return weight;
    }

    /**
     * @param queryTerms can be null.
     * @return a Pattern where group(1) != null contains base href, group(2) != null contais image src and
     *         group(3) contains term match.
     */
    static Pattern createAllPattern(Set<String> queryTerms) {
        StringWriter sw = new StringWriter();
        sw.append(basePattern.pattern());
        sw.append("|");
        sw.append(imgPattern.pattern());
        if (!queryTerms.isEmpty()) {
            sw.append("|\\b("); // Word boundary
            boolean first = true;
            for (String term : queryTerms) {
                if (!term.isEmpty()) {
                    if (first) {
                        first = false;
                    } else {
                        sw.append("|");
                    }
                    sw.append(Pattern.quote(term));
                }
            }
            sw.append(")\\b"); // Word boundary
        }
        return Pattern.compile(sw.toString());
    }

    public static class Token {
        public final TYPE type;
        public final String content;

        public Token(TYPE type, String content) {
            this.type = type;
            this.content = content;
        }
    }
    public enum TYPE {img, term}

    public static class WeightedImage implements Comparable<WeightedImage> {
        private final Set<String> terms;
        private final URL image;
        /**
         * If terms is not empty, the weight is sum(1/distance(term, image)).
         * If there are not matching terms, the weight is NO_TERMS_WEIGHT.
         */
        private final double weight;

        public WeightedImage(URL image, Set<String> terms, double weight) {
            this.terms = terms;
            this.image = image;
            this.weight = weight;
        }

        public Set<String> getTerms() {
            return terms == null ? Collections.<String>emptySet() : terms;
        }

        public URL getImageURL() {
            return image;
        }

        public double getWeight() {
            return weight;
        }

        @Override
        public int compareTo(WeightedImage o) {
            return weight > o.weight ? -1 : weight > o.weight ? 1 : image.toString().compareTo(o.image.toString());
        }

        @Override
        public String toString() {
            return "WeightedImage(image=" + getImageURL() + ", weight=" + getWeight() + ", terms=" + getTerms() + ")";
        }
    }
}
