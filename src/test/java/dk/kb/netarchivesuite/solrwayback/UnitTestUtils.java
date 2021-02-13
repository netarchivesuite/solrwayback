package dk.kb.netarchivesuite.solrwayback;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

public class UnitTestUtils {
  
  /**
   * Multi protocol resource loader. Primary attempt is direct file, secondary is classpath resolved to File.
   *
   * @param resource a generic resource.
   * @return a File pointing to the resource.
   */
  public static File getFile(String resource) throws IOException {
      File directFile = new File(resource);
      if (directFile.exists()) {
          return directFile;
      }
      URL classLoader = Thread.currentThread().getContextClassLoader().getResource(resource);
      if (classLoader == null) {
          throw new FileNotFoundException("Unable to locate '" + resource + "' as direct File or on classpath");
      }
      String fromURL = classLoader.getFile();
      if (fromURL == null || fromURL.isEmpty()) {
          throw new FileNotFoundException("Unable to convert URL '" + fromURL + "' to File");
      }
      return new File(fromURL);
  }

    public static String loadUTF8(String resource) throws IOException {
        File source = getFile(resource);
        if (source == null) {
            return null;
        }
        InputStream in = new FileInputStream(source);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        pipe(in, bos);
        in.close();
        return bos.toString("utf-8");
    }

    public static void saveUTF8(String content, File location) throws IOException {        
        FileOutputStream out = new FileOutputStream(location);
        Writer writer = new OutputStreamWriter(out);
        writer.write(content);        
        writer.flush();
        writer.close();
        out.close();
    }

    private static void pipe(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }

}
