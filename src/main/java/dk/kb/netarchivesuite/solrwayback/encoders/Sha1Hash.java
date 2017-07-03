package dk.kb.netarchivesuite.solrwayback.encoders;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
public class Sha1Hash {

    public static String createSha1(File file) throws Exception  {
        InputStream fis = null;
        try{
            fis = new FileInputStream(file);     
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            int n = 0;
            byte[] buffer = new byte[8192];
            while (n != -1) {
                n = fis.read(buffer);
                if (n > 0) {
                    digest.update(buffer, 0, n);
                }
            }
            String hash = "sha1:" + Base32Encoder.encode( digest.digest());
            return hash;
        }
        catch(Exception e){            
            throw e;
        }
        finally{
            if (fis!=null){
                fis.close();
            }
        }
    }    
        
    public static String createSha1(InputStream fis) throws Exception  {    
      try{        
          MessageDigest digest = MessageDigest.getInstance("SHA-1");
          int n = 0;
          byte[] buffer = new byte[8192];
          while (n != -1) {
              n = fis.read(buffer);
              if (n > 0) {
                  digest.update(buffer, 0, n);
              }
          }
          String hash = "sha1:" + Base32Encoder.encode( digest.digest());
          return hash;
      }
      catch(Exception e){            
          throw e;
      }
      finally{
          if (fis!=null){
              fis.close();
          }
      }
  }    
}
