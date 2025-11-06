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
public class GeneFilter extends Filter {
	private static final Logger log = LoggerFactory.getLogger(GeneFilter.class.getName());
	private static Dfa dfa = null;
	private static Map<String, String> geneBlackList = null;
	private final ApplicationProperty applicationProperty;
	private AbstractFaAction get_gene = new AbstractFaAction() {
		private static final long serialVersionUID = 7357180318320088118L;

		@Override
		public void invoke(StringBuilder yytext, int start, DfaRun runner) {
			try {
				Map<String, String> map = Xml.splitElement(yytext, start);
				String text = map.get(Xml.CONTENT);
				boolean in_blacklist = false;
				if (GeneFilter.geneBlackList.get(text.trim()) != null) {
					in_blacklist = true;
				}

				if (in_blacklist) {
					yytext.replace(start, yytext.length(), "" + text);
				} else if (text.trim().length() < 3) {
					yytext.replace(start, yytext.length(), "" + text);
				}
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
	};

	public GeneFilter(ApplicationProperty applicationProperty) {
		this.applicationProperty = applicationProperty;
	}

	@PostConstruct
	public void init() {
		try {
			Nfa nfa = new Nfa(Nfa.NOTHING);
			nfa.or(Xml.GoofedElement(applicationProperty.Z_UNIPROT), get_gene);
			dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
			try(InputStream is = GeneFilter.class.getResourceAsStream(applicationProperty.GENE_BLACKLIST_160504);
			BufferedReader bf = new BufferedReader(new InputStreamReader(is))) {
				geneBlackList = new HashMap<>();
				String line;

				while ((line = bf.readLine()) != null) {
					geneBlackList.put(line.trim(), "Y");
				}
			}

		} catch (Exception e) {
			log.error("GeneFilter error!" + e);
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
			log.error("GeneFilter error!", e);
			throw e;
		}
	}

}
