package uk.ac.ebi.literature.textminingapi.filter;

import monq.jfa.*;
import monq.jfa.ctx.ContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.ac.ebi.literature.textminingapi.utility.ApplicationProperty;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@Lazy
public class ChEBIFilter extends Filter {

	private static final Logger log = LoggerFactory.getLogger(ChEBIFilter.class.getName());
	private static Dfa dfa = null;
	private static Map<String, String> chemicalBlackList = null;
	private static Map<String, String> aaBlackList = null;
	private static AbstractFaAction get_chebi = new AbstractFaAction() {
		private static final long serialVersionUID = 6161700087394851439L;

		@Override
		public void invoke(StringBuilder yytext, int start, DfaRun runner) {
			try {

				Map<String, String> map = Xml.splitElement(yytext, start);
				String text = map.get(Xml.CONTENT);
				boolean in_blacklist = false;
				boolean is_aa = false;

				if (ChEBIFilter.chemicalBlackList.get(text.toLowerCase().trim()) != null) {
					in_blacklist = true;
				}

				if (ChEBIFilter.aaBlackList.get(text.toLowerCase().trim()) != null) {
					is_aa = true;
				}

				if (in_blacklist) {
					yytext.replace(start, yytext.length(), "" + text);
				} else if (is_aa) {
					yytext.replace(start, yytext.length(), "" + text);
				} else if (text.trim().length() < 3) {
					yytext.replace(start, yytext.length(), "" + text);
				}

			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
	};
	private final ApplicationProperty applicationProperty;

	public ChEBIFilter(ApplicationProperty applicationProperty) {
		this.applicationProperty = applicationProperty;
	}

	@PostConstruct
	public void init() throws Exception {
		try {
			Nfa nfa = new Nfa(Nfa.NOTHING);

			ContextManager cmgr = new ContextManager(nfa);
			cmgr.addXml(applicationProperty.PLAIN);

			nfa.or(Xml.GoofedElement(applicationProperty.Z_CHEBI), get_chebi);

			dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
			String line;
			try(InputStream is = ChEBIFilter.class.getResourceAsStream(applicationProperty.CHEMICAL_BLACKLIST_ONLY_LOWERCASE_150615);
			BufferedReader bf = new BufferedReader(new InputStreamReader(is))) {
				chemicalBlackList = new HashMap<>();

				while ((line = bf.readLine()) != null) {
					chemicalBlackList.put(line, "Y");
				}
			}
			try(InputStream is = ChEBIFilter.class.getResourceAsStream(applicationProperty.AA_BLACKLIST);
				BufferedReader bf = new BufferedReader(new InputStreamReader(is))) {


				aaBlackList = new HashMap<>();
				while ((line = bf.readLine()) != null) {
					aaBlackList.put(line, "Y");
				}
			}

		} catch (Exception e) {
			log.error("ChEBIFilter error!", e);
			throw e;
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
			dfaRun.clientData = ContextManager.createStackProvider();
			dfaRun.setIn(charSource);
			dfaRun.filter(outpw);
		} catch (Exception e) {
			log.error("ChEBIFilter error!", e);
			throw e;
		}
	}

}
