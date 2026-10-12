package dk.kb.netarchivesuite.solrwayback.image;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import dk.kb.images.hash.PdqHasher;
import dk.kb.netarchivesuite.solrwayback.util.InputStreamUtils;

public class ImageUtils {

    
    public static BufferedImage getImageFromBinary(byte[] bytes) throws Exception{

        InputStream in = new ByteArrayInputStream(bytes);
        InputStream maybeDecompress = InputStreamUtils.maybeDecompress(in);        
        BufferedImage image = ImageIO.read(maybeDecompress);        
        return image;        
    }
    
    public static BufferedImage getImageFromBinary(InputStream bytes) throws Exception{

        InputStream maybeDecompress = InputStreamUtils.maybeDecompress(bytes);
        BufferedImage image = ImageIO.read(maybeDecompress);
        return image;
    }

    //TODO not sure this is the best/fastest way to do this in Java. For animated images, this will only return the first image
   public static BufferedImage resizeImage(BufferedImage originalImage, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        double scale = determineImageScale(sourceWidth, sourceHeight, targetWidth, targetHeight);
        Image scaledInstance = originalImage.getScaledInstance((int) (sourceWidth * scale), (int) (sourceHeight * scale), Image.SCALE_SMOOTH);
        BufferedImage b_img = new BufferedImage(scaledInstance.getWidth(null), scaledInstance.getHeight(null), BufferedImage.TYPE_INT_RGB);
        b_img.getGraphics().drawImage(scaledInstance, 0, 0, null);
        return b_img;
    }

    private static double determineImageScale(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        double scalex = (double) targetWidth / sourceWidth;
        double scaley = (double) targetHeight / sourceHeight;
        return Math.min(scalex, scaley);

    }
    
    /**
     * Builds a Solr OR query string that searches all 64 band fields for
     * near-duplicate images similar to the given query image.
     *
     * <p>Each candidate document in the index has, at ingest time, had ALL
     * 8 of its own dihedral variants hashed and banded, and stored under
     * {@code image_pdq_<variantName>_band_<n>}. Because a true near-duplicate
     * candidate is related to the query image by some unknown, arbitrary
     * element of the dihedral group (some rotation/flip), there is no reason
     * to expect the candidate's "{@code rotate90}"-named field to be the one
     * that aligns with the query's own "{@code rotate90}" hash: the relative
     * transform between query and candidate can shuffle which of the
     * candidate's 8 stored variants lines up with any given query variant.
     * Matching field name to field name (query's {@code rotate90} against
     * candidate's {@code rotate90}, etc.) therefore only reliably succeeds
     * for an exact self-match (identity transform) and misses real
     * near-duplicates at a non-trivial relative rotation/flip.
     *
     * <p>The fix is to only hash the query image in its own, single, native
     * orientation ({@code dihedralHashes[0]}, i.e. "original") and probe that
     * one hash's 8 bands against ALL 8 of the candidate's variant-named band
     * fields at each band position. This is mathematically sufficient: if
     * query Q and a candidate C are related by some dihedral transform g0
     * (C = Q transformed by g0), then C's stored hash for variant g0⁻¹ is
     * exactly equal to Q's own original hash (since hashing C via g0⁻¹
     * composes with g0 to the identity transform on Q). g0⁻¹ is itself one
     * of the 8 dihedral group elements, so it is guaranteed to be one of the
     * 8 variants C has indexed under its own band fields. The band for that
     * stored variant will therefore match the query's original-hash band
     * exactly, at whichever band position is error-free. Using all 8 of the
     * candidate's variant names per band keeps the query at 8 bands × 8
     * variant-name field families = 64 clauses total — the same cost as
     * before — while actually covering every possible relative orientation.
     *
     * <p>Example output fragment:
     * <pre>
     *   image_pdq_original_band_0:fca62ca1 OR
     *   image_pdq_rotate90_band_0:fca62ca1 OR ... OR
     *   image_pdq_flipMinus1_band_0:fca62ca1 OR
     *   image_pdq_original_band_1:631bcb7a OR ... OR
     *   image_pdq_flipMinus1_band_7:631bcb7a
     * </pre>
     *
     * <p>The resulting query should be used as a coarse candidate filter.
     * Results must be post-filtered by computing the full Hamming distance
     * between the candidate's stored {@code image_pdq_hash} and the query's
     * dihedral hashes to remove false positives. See
     * {@link PdqHasher#hammingDistance(String, String)} and the PDQ
     * similarity threshold of ≤ 31 (out of 256).
     *
     * @param queryImage the query image to search for near-duplicates of
     * @return ImageDihedralHashesAndQuery  that contains all 8 dihedral pdq-hashes (for the post-filter Hamming step) and a solr query that matches all 64 variations.
     */
    public static ImageDihedralHashesAndQuery buildPdqBandQuery(BufferedImage queryImage) {
        ImageDihedralHashesAndQuery  imageDihedralHashesAndQuery= new ImageDihedralHashesAndQuery();
        String[] dihedralHashes = PdqHasher.getAllDihedralHashes(queryImage);
        // Only the query image's own ("original") orientation is needed to
        // build the search query. Every candidate document has already
        // indexed bands for all 8 of ITS OWN dihedral variants, so whichever
        // relative rotation/flip relates the query to a true near-duplicate,
        // one of the candidate's 8 stored variants will align exactly with
        // the query's own hash. See the method javadoc for the full argument.
        String[] bands = PdqHasher.splitIntoBands(dihedralHashes[0]);
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (int b = 0; b < bands.length; b++) {
            for (String dihedralName : PdqHasher.DIHEDRAL_NAMES) {
                if (!first) query.append(" OR ");
                query.append("image_pdq_").append(dihedralName)
                     .append("_band_").append(b)
                     .append(":").append(bands[b]);
                first = false;
            }
        }
        imageDihedralHashesAndQuery.setDihedralPdqHashes(dihedralHashes);
        imageDihedralHashesAndQuery.setDihedralQueryString(query.toString());
        return imageDihedralHashesAndQuery;
    }
    
}