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
     * <p>The query image is hashed into all 8 dihedral variants (rotations
     * and mirrors) in a single pipeline pass. Each of the 8 hashes is then
     * split into 8 bands of 8 characters using
     * {@link PdqHasher#splitIntoBands(String)}, producing 64 Solr field
     * probes in total (8 dihedral variants × 8 bands).
     *
     * <p>Example output fragment:
     * <pre>
     *   image_pdq_original_band_0:fca62ca1 OR
     *   image_pdq_original_band_1:631bcb7a OR ... OR
     *   image_pdq_flipMinus1_band_7:d823e31c
     * </pre>
     *
     * <p>The resulting query should be used as a coarse candidate filter.
     * Results must be post-filtered by computing the full Hamming distance
     * between the candidate's stored {@code image_pdq_hash} and the query
     * hash to remove false positives. See
     * {@link PdqHasher#hammingDistance(String, String)} and the PDQ
     * similarity threshold of ≤ 31 (out of 256).
     *
     * @param queryImage the query image to search for near-duplicates of
     * @return ImageDihedralHashesAndQuery  that contains all 8 dihedral pdq-hashes and solr query search for to match all 64 variations.
     */
    public static ImageDihedralHashesAndQuery buildPdqBandQuery(BufferedImage queryImage) {
        ImageDihedralHashesAndQuery  imageDihedralHashesAndQuery= new ImageDihedralHashesAndQuery(); 
        String[] dihedralHashes = PdqHasher.getAllDihedralHashes(queryImage);
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (int d = 0; d < dihedralHashes.length; d++) {
            String dihedralName = PdqHasher.DIHEDRAL_NAMES[d];
            String[] bands = PdqHasher.splitIntoBands(dihedralHashes[d]);
            for (int b = 0; b < bands.length; b++) {
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
