/* **
 * This code has been designed/implemented and is maintained by:
 *
 * Miguel Arregui (miguel.arregui@gmail.com)
 *
 * Any comments and/or feedback are welcome and encouraged.
 *
 * Started on:     1 November 2005.
 * Last reviewed:  2 December 2005.
 * This is a server accessible via the DistFilter command
 * or as a stand alone program. It will read the input and
 * remove from it the <z:uniprot> tags when the values of
 * the parameters "abbrscore" and "fd" match either the
 * following rules:
 * <p>
 * 1.- abbrscore == -1
 * 2.- abbrscore == 0 && fd >= threshold
 * <p>
 * To invoke it as a stand alone programme:
 * <p>
 * echo "text" | marie.bnc.BncFilter threshold
 * cat  file   | marie.bnc.BncFilter threshold
 * <p>
 * To invoke it as a remote server:
 * <p>
 * echo "text" | DistFilter svr=bncfilter
 * cat  file   | DistFilter svr=bncfilter
 * <p>
 * (in this case the thrshold is defined in the proper config file).
 * <p>
 * To start a new EBIMedFilter filter server:
 * <p>
 * java marie.bnc.BncFilter -p port threshold
 * <p>
 * (if port == 0 the proper config file will be parsed).
 */


package uk.ac.ebi.literature.textminingapi.filter;


import monq.jfa.*;
import monq.net.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.utility.ApplicationProperty;

import javax.annotation.PostConstruct;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ALL")
@Component
@Lazy
public class BncFilter extends Filter {
    private static final Logger log = LoggerFactory.getLogger(BncFilter.class.getName());
    private static final TcpServer svr = null;
    private static Dfa dfa = null;
    private final ApplicationProperty applicationProperty;

    /* **** z:uniprot **** */
    private AbstractFaAction action_start_z = new AbstractFaAction() {
        private static final long serialVersionUID = 8923926298962406789L;

        @Override
        public void invoke(StringBuilder yytext, int start, DfaRun runner)
                throws CallbackException {
            BncFilterData data = (BncFilterData) runner.clientData;
            runner.collect = true;
            data.attrsPopulate(yytext, start);
            String fb = data.attrsGet("fb");
            data.fb = (fb != null) ? Integer.parseInt(fb) : 0;
            String abbrScore = data.attrsGet("abbrscore");
            data.abbrScore = (abbrScore != null) ? Integer.parseInt(abbrScore) : 0;
            data.startZ1 = start;
            data.endZ1 = yytext.length();
        }
    };
    private AbstractFaAction action_end_z = new AbstractFaAction() {
        private static final long serialVersionUID = 3761026516751598829L;

        @Override
        public void invoke(StringBuilder yytext, int start, DfaRun runner)
                throws CallbackException {
            BncFilterData data = (BncFilterData) runner.clientData;
            if (!data.check()) { // remove z:uniprot tags
                yytext.setLength(start);                 // remove closing tag
                yytext.delete(data.startZ1, data.endZ1); // remove opening tag
            }
            runner.collect = false;
        }
    };

    public BncFilter(ApplicationProperty applicationProperty) {
        this.applicationProperty = applicationProperty;
    }

    @PostConstruct
    public void init() {
        try {
            Nfa nfa = new
                    Nfa(Xml.STag(applicationProperty.Z_UNIPROT), action_start_z)
                    .or(Xml.STag(applicationProperty.Z_CHEBI), action_start_z)
                    .or(Xml.ETag(applicationProperty.Z_UNIPROT), action_end_z)
                    .or(Xml.ETag(applicationProperty.Z_CHEBI), action_end_z);
            dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        } catch (Exception e) {
            log.error("EBIMedFilter Kaput!", e);
        }
    }

    @Override
    public void run() throws Exception {
        PrintStream outpw;
        try {
            outpw = new PrintStream(out, true, ApplicationProperty.UTF_8);
        } catch (UnsupportedEncodingException e) {
            outpw = new PrintStream(out);
        }
        ReaderCharSource charSource = null;
        try {
            charSource = new ReaderCharSource(in, ApplicationProperty.UTF_8);
        } catch (UnsupportedEncodingException e) {
            charSource = new ReaderCharSource(in);
        }

        try {
            DfaRun dfaRun = new DfaRun(dfa);
            int threshold = 160;
            dfaRun.clientData = new BncFilterData(threshold);
            dfaRun.setIn(charSource);
            dfaRun.filter(outpw);
        } catch (Exception e) {
            log.info("error in BncFilter: ", e);
            throw e;
        }
    }

    protected class BncFilterData {
        public int startZ1;
        public int endZ1;
        public int fb;
        public int abbrScore;
        protected int threshold;
        protected Map attrs;

        public BncFilterData(int threshold) {
            this.threshold = threshold;
            attrs = new HashMap();
        }

        public boolean check() {
            return (abbrScore > 0) ? true : (abbrScore < 0) ? false : (fb <= threshold);
        }

        public void attrsPopulate(StringBuilder text, int start) {
            attrs.clear();
            Xml.splitElement(attrs, text, start);
        }

        public String attrsGet(String key) {
            return (String) attrs.get(key);
        }
    }
}