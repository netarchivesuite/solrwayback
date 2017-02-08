package dk.kb.netarchivesuite.solrwayback;

import java.io.File;
import dk.kb.netarchivesuite.solrwayback.encoders.Sha1Hash;

public class SHA1Test {
     
    public static void main(String[] args) throws Exception {

        String filePath = "/home/teg/Desktop/dsc/karl_ove/01005A_Matematik1-Lect20120917-PolynomierDel2-720p.mp4";
    //    String filePath = "/home/teg/Desktop/dsc/karl_ove/image2.jpg";
        File file = new File(filePath);
                           
       String hash= Sha1Hash.createSha1(file);
       System.out.println(hash);
    }        
}
