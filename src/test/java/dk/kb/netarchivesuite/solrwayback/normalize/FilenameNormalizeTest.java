package dk.kb.netarchivesuite.solrwayback.normalize;

import org.junit.Test;

import static dk.kb.netarchivesuite.solrwayback.export.StreamingRawZipExport.normalizeFilename;
import static org.junit.Assert.assertEquals;

public class FilenameNormalizeTest {

    @Test
    public void testUnderscores(){
        String test = "test__filename_with___cuncurrent_underscores_.ext";

        String result = normalizeFilename(test);

        assertEquals("test_filename_with_cuncurrent_underscores.ext", result);
    }

    @Test
    public void testBadCharacters(){
        String test = "test%file&name.with.extra.punctuation&what.ext";
        String result = normalizeFilename(test);
        assertEquals("testfilenamewithextrapunctuationwhat.ext", result);
    }


    @Test
    public void testLongName(){
        String test = "1234567890_thisfilenameiswaytolongforaproperfilename_howdoesthemethodhandleme_iwonderificandestroysomesystemsbybeingsoboringlyuglylong" +
                "omgthisisonlyhalfthelengthofwhatineedtobetodestroysomesystemshowonearthdoibecomeaslongasneededmaybeitwillhelpifijustrambleonandonandonforsomewords.ext";
        String result = normalizeFilename(test);
        assertEquals(255, result.length());
    }
}
