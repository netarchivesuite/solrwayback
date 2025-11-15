package dk.kb.netarchivesuite.solrwayback.util;

import dk.kb.netarchivesuite.solrwayback.image.ImageUtils;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link dk.kb.netarchivesuite.solrwayback.image.ImageUtils}.
 */
public class ImageUtilsTest {

    /**
     * Create a solid-colored BufferedImage for use in tests.
     *
     * @param width  image width in pixels
     * @param height image height in pixels
     * @param fill   fill color
     * @return created BufferedImage
     */
    private BufferedImage createTestImage(int width, int height, Color fill) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setPaint(fill);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        return img;
    }

    /**
     * Write the provided image as a PNG and return the raw bytes.
     *
     * @param img image to encode
     * @return PNG-encoded bytes
     * @throws Exception on write failure
     */
    private byte[] writeImageToBytes(BufferedImage img) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean ok = ImageIO.write(img, "png", baos);
        if (!ok) throw new IllegalStateException("Could not write image as png");
        return baos.toByteArray();
    }

    /**
     * Verify that a PNG image can be read from a byte array.
     */
    @Test
    public void testGetImageFromBinary_byteArray() throws Exception {
        BufferedImage original = createTestImage(100, 50, Color.BLUE);
        byte[] png = writeImageToBytes(original);

        BufferedImage read = ImageUtils.getImageFromBinary(png);
        assertNotNull("Image should be read from byte[]", read);
        assertEquals("Width should match", 100, read.getWidth());
        assertEquals("Height should match", 50, read.getHeight());
    }

    /**
     * Verify that a PNG image can be read from an InputStream.
     */
    @Test
    public void testGetImageFromBinary_inputStream() throws Exception {
        BufferedImage original = createTestImage(64, 32, Color.RED);
        byte[] png = writeImageToBytes(original);

        InputStream in = new ByteArrayInputStream(png);
        BufferedImage read = ImageUtils.getImageFromBinary(in);
        assertNotNull("Image should be read from InputStream", read);
        assertEquals(64, read.getWidth());
        assertEquals(32, read.getHeight());
    }

    /**
     * Verify that a gzipped PNG InputStream is accepted and decoded correctly.
     */
    @Test
    public void testGetImageFromBinary_gzippedInputStream() throws Exception {
        BufferedImage original = createTestImage(20, 10, Color.GREEN);
        byte[] png = writeImageToBytes(original);

        // gzip compress
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(png);
        }
        byte[] gzipped = baos.toByteArray();

        InputStream in = new ByteArrayInputStream(gzipped);
        BufferedImage read = ImageUtils.getImageFromBinary(in);
        assertNotNull("Image should be read from gzipped InputStream", read);
        assertEquals(20, read.getWidth());
        assertEquals(10, read.getHeight());
    }

    /**
     * Verify resizing preserves aspect ratio when downscaling and upscaling.
     */
    @Test
    public void testResizeImage_downscaleAndUpscale() {
        BufferedImage original = createTestImage(200, 100, Color.MAGENTA);

        // Downscale: target 50x50 -> expected 50x25 (preserve aspect)
        BufferedImage down = ImageUtils.resizeImage(original, 200, 100, 50, 50);
        assertNotNull(down);
        assertEquals(50, down.getWidth());
        assertEquals(25, down.getHeight());

        // Upscale: original 50x25 -> target 200x200 -> expected 200x100
        BufferedImage small = createTestImage(50, 25, Color.BLACK);
        BufferedImage up = ImageUtils.resizeImage(small, 50, 25, 200, 200);
        assertNotNull(up);
        assertEquals(200, up.getWidth());
        assertEquals(100, up.getHeight());
    }
}
