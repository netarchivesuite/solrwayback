package dk.kb.netarchivesuite.solrwayback.image;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

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

    
}
