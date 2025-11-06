package uk.ac.ebi.literature.textminingapi.utility;

import monq.jfa.Xml;

import java.util.Map;

public class TaggerUtils {

    public static String reEmbedContent(String taggedF, StringBuilder yytext, Map<String, String> map, int start) {
        int contentBegins = yytext.indexOf(map.get(Xml.CONTENT), start);
        int contentLength = map.get(Xml.CONTENT).length();
        return yytext.substring(start, contentBegins) + taggedF
                + yytext.substring(contentBegins + contentLength);
    }
}
