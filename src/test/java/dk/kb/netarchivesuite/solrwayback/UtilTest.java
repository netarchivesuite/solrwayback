package dk.kb.netarchivesuite.solrwayback;

public class UtilTest {

    public static void main(String[] args){
        String before = "http://www.gyldendal.dk/wcsstore/GYLDENDAL/upload\\products\\andet\\borges_140.jpg";
        System.out.println(before);
        String after= before.replaceAll("[\\\\]", "/");
        System.out.println(after);
        
        
    }
    
}
