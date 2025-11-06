/* **
 * This code has been designed/implemented and is maintained by:
 * 
 * Miguel Arregui (miguel.arregui@gmail.com)
 * 
 * Any comments and/or feedback are welcome and encouraged. 
 * 
 * Started on:     1 November 2005.
 * Last reviewed:  2 December 2005.
 */


/**
 * This is a server accessible via the DistFilter command
 * or as a stand alone program. It will read the input and 
 * remove from it the <z:chebi> tags when the values of 
 * the parameters "abbrscore" and "fd" match either the
 * following rules: 
 * 
 * 1.- abbrscore == -1
 * 2.- abbrscore == 0 && fd >= threshold
 * 
 * To invoke it as a stand alone programme: 
 * 
 *   echo "text" | marie.bnc.BncFilter threshold
 *   cat  file   | marie.bnc.BncFilter threshold  
 * 
 * To invoke it as a remote server: 
 * 
 *   echo "text" | DistFilter svr=bncfilter
 *   cat  file   | DistFilter svr=bncfilter
 * 
 *   (in this case the thrshold is defined in the proper config file).
 * 
 * To start a new EBIMedFilter filter server:
 * 
 *   java marie.bnc.BncFilter -p port threshold
 * 
 *   (if port == 0 the proper config file will be parsed).
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@Lazy
public class ChEBIBncFilter extends Filter {
	private static final Logger log = LoggerFactory.getLogger(ChEBIBncFilter.class.getName());
	private static final TcpServer svr = null;
	private static Dfa dfa = null;
	private static AbstractFaAction action_start_z = new AbstractFaAction() {
		private static final long serialVersionUID = -7632522279949542351L;

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
	private static AbstractFaAction action_end_z = new AbstractFaAction() {
		private static final long serialVersionUID = -7484550386805325673L;

		@Override
		public void invoke(StringBuilder yytext, int start, DfaRun runner)
				throws CallbackException {
			BncFilterData data = (BncFilterData) runner.clientData;
			if (!data.check()) { // remove z:chebi tags
				yytext.setLength(start);                 // remove closing tag
				yytext.delete(data.startZ1, data.endZ1); // remove opening tag
			}
			runner.collect = false;
		}
	};
	private final ApplicationProperty applicationProperty;

	public ChEBIBncFilter(ApplicationProperty applicationProperty) {
		this.applicationProperty = applicationProperty;
	}

	@PostConstruct
	public void init() {
		try {
			Nfa nfa = new
					Nfa(Xml.STag(applicationProperty.Z_CHEBI), action_start_z)
					.or(Xml.ETag(applicationProperty.Z_CHEBI), action_end_z);
			dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
		} catch (Exception e) {
			throw new Error("EBIMedFilter Kaput!", e);
		}
	}

	@Override
	public void run() throws Exception {
		PrintStream outpw;
		outpw = new PrintStream(out, true, StandardCharsets.UTF_8);
		ReaderCharSource charSource;
		try {
			charSource = new ReaderCharSource(in, ApplicationProperty.UTF_8);
		} catch (UnsupportedEncodingException e) {
			charSource = new ReaderCharSource(in);
		}

		try {
			DfaRun dfaRun = new DfaRun(dfa);
			int threshold = 250;
			dfaRun.clientData = new BncFilterData(threshold);
			dfaRun.setIn(charSource);
			dfaRun.filter(outpw);
		} catch (Exception e) {
			log.info("error in chEBIBncFilter: ", e);
			throw e;
		}
	}

	protected static class BncFilterData {
		public int startZ1;
		public int endZ1;
		public int fb;
		public int abbrScore;
		protected int threshold;
		protected Map<String, String> attrs;

		public BncFilterData(int threshold) {
			this.threshold = threshold;
			attrs = new HashMap<>();
		}

		public boolean check() {
			return abbrScore > 0 || (abbrScore >= 0 && (fb <= threshold));
		}

		public void attrsPopulate(StringBuilder text, int start) {
			attrs.clear();
			Xml.splitElement(attrs, text, start);
		}

		public String attrsGet(String key) {
			var s = attrs.get(key);
			return s;
		}
	}
}