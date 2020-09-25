package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;

public class HtmlParserUrlRewriterFromWarcTest {
    
    
    /*
     * Integration test class to parse HTML from a warc-file.
     * Warc-files can not be in reposity so change path to a local warc-file
     *  
     */
    public static void main(String []args) {
        try {
        String warcFile="/media/teg/1TB_SSD/solrwayback_package_3.2/indexing/warcs/denoffentlige-00000.warc";
        long offset=2691693;
        
        ArcEntry arc=ArcParserFileResolver.getArcEntry(warcFile, offset);
        String html = arc.getBinaryContentAsStringUnCompressed();
               
        
        ParseResult rewritten = HtmlParserUrlRewriter.replaceLinks(
                html, "http://example.com/somefolder/", "2020-04-30T13:07:00",
                RewriteTestHelper.createOXResolver(true));

        
        //See the replaced HTML
        System.out.println(rewritten.getReplaced());
                    
        }
        catch(Exception e) {
         e.printStackTrace();            
        }
        
        
    }

}
