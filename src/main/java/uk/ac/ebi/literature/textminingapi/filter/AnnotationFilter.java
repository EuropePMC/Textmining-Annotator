package uk.ac.ebi.literature.textminingapi.filter;

import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.ac.ebi.literature.textminingapi.parser.MwtParser;
import uk.ac.ebi.literature.textminingapi.parser.MwtParser.MwtAtts;
import uk.ac.ebi.literature.textminingapi.resolver.*;
import uk.ac.ebi.literature.textminingapi.utility.ApplicationProperty;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Lazy
public class AnnotationFilter extends Filter {

	private static final Logger log = LoggerFactory.getLogger(AnnotationFilter.class.getName());
	private final ApplicationProperty applicationProperty;
	private final DoiResolver dr;
	private final AccResolver ar;
	private final NcbiResolver nr;
	private final BioStudiesResolver bioStudiesr;
	private final HpaResolver hpar;
	private final ResponseCodeResolver responseCoder;
	private final HipsciResolver hipscir;
	private Dfa dfa_entity;
	private final Map<String, String> cachedValidations = new HashMap<>();
	private final Map<String, Integer> numOfAccInBoundary = new HashMap<>();
	@Value("${cached}")
	private String predefFilename;

	/**
	 * This processes an accession number noval: refseq, refsnp, context: eudract
	 * offline: pfam, online (+ offline): the rest
	 */
	private AbstractFaAction procEntity = new AbstractFaAction() {
		/**
		 *
		 */
		private static final long serialVersionUID = 3904925534560869886L;

		@Override
		public void invoke(StringBuilder yytext, int start, DfaRun runner) {
			try {
				Map<String, String> map = Xml.splitElement(yytext, start);
				MwtAtts m = new MwtParser().parse(map);
				if (m.getValMethod() != null && (!"".equalsIgnoreCase(m.getValMethod()))) {
					String textBeforeEntity = AnnotationFilter.this.getTextBeforeEntity(yytext, start, m.getWsize());

					boolean isValid = false;
					if (AnnotationFilter.this.isAccInBlacklist(m.getContent())) {
						isValid = false;
					} else if ("noval".equals(m.getValMethod())) {
						isValid = true;
					} else if ("contextOnly".equals(m.getValMethod())) {
						if (AnnotationFilter.this.isAnySameTypeBefore(m.getDb()) || AnnotationFilter.this.isInContext(textBeforeEntity, m.getCtx())) {
							isValid = true;
						}
					} else if ("cachedWithContext".equals(m.getValMethod())) {
						if ((AnnotationFilter.this.isAnySameTypeBefore(m.getDb()) || AnnotationFilter.this.isInContext(textBeforeEntity, m.getCtx()))
								&& AnnotationFilter.this.isIdValidInCache(m.getDb(), m.getContent(), m.getDomain())) {
							isValid = true;
						}
					} else if ("onlineWithContext".equals(m.getValMethod())) {
						if ((AnnotationFilter.this.isAnySameTypeBefore(m.getDb()) || AnnotationFilter.this.isInContext(textBeforeEntity, m.getCtx()))
								&& AnnotationFilter.this.isOnlineValid(m.getDb(), m.getContent(), m.getDomain())) {
							isValid = true;
						}
					} else if ("context".equals(m.getValMethod())) {
						if (AnnotationFilter.this.isInContext(textBeforeEntity, m.getCtx())) {
							isValid = true;
						}
					} else if ("cached".equals(m.getValMethod())) {
						if (AnnotationFilter.this.isIdValidInCache(m.getDb(), m.getContent(), m.getDomain())) {
							isValid = true;
						}
					} else if ("online".equals(m.getValMethod())) {
						if (AnnotationFilter.this.isOnlineValid(m.getDb(), m.getContent(), m.getDomain())) {
							isValid = true;
						}
					}

					String secOrSent = "";
					if (runner.clientData != null) {
						secOrSent = runner.clientData.toString();
					}

					if (isValid && AnnotationFilter.this.isInValidSection(secOrSent, m.getSec())) {
						String tagged = "<" + m.getTagName() + " db=\"" + m.getDb() + "\"" +
								" ids=\"" + m.getContent() + "\">" + m.getContent() + "</" + m.getTagName() +
								">";
						yytext.replace(start, yytext.length(), tagged);
						numOfAccInBoundary.put(m.getDb(), 1);
					} else { // not valid
						yytext.replace(start, yytext.length(), m.getContent());
					}
				}
			} catch(OutOfMemoryError e){
				log.info("out of memory exception occured in Accession filter step", e);
				Resolver.throwException(new Exception("out of memory exception occured in Accession filter step"));
			} catch (Exception e) {
				log.info("context", e);
				Resolver.throwException(e);
			}
		}
	};

	public AnnotationFilter(ApplicationProperty applicationProperty, DoiResolver dr, AccResolver ar, NcbiResolver nr, BioStudiesResolver bioStudiesr, HpaResolver hpar, ResponseCodeResolver responseCoder, HipsciResolver hipscir) {
		this.applicationProperty = applicationProperty;
		this.dr = dr;
		this.ar = ar;
		this.nr = nr;
		this.bioStudiesr = bioStudiesr;
		this.hpar = hpar;
		this.responseCoder = responseCoder;
		this.hipscir = hipscir;
	}

	private boolean isAnySameTypeBefore(String db) { // Can I use this for a range?
		return numOfAccInBoundary.containsKey(db);
	}

	private boolean isInContext(String textBeforeEntity, String context) {
		Pattern p = Pattern.compile(context);
		Matcher m = p.matcher(textBeforeEntity);
		return m.find();
	}


	private String getTextBeforeEntity(StringBuilder yytext, int start, Integer windowSize) {
		int prevStart = Math.max((start - windowSize), 0); // if start-windowSize <0 always return 0
		return yytext.substring(prevStart, start);
	}


	private boolean isInValidSection(String secOrSent, String blacklistSectionValues) {
		if (blacklistSectionValues != null && !blacklistSectionValues.equals("")) {
			String[] blackListSections = blacklistSectionValues.split(",");
			List<String> sections = Arrays.asList(secOrSent.split(","));

			for (String blackListSection : blackListSections) {
				if (sections.contains(blackListSection)) {
					return false;
				}
			}
		}
		return true;
	}

	boolean isIdValidInCache(String db, String id, String domain) {
		id = ar.normalizeID(db, id);
		boolean isValid = false;
		if (cachedValidations.containsKey(domain + id)) {
			String res = cachedValidations.get(domain + id);
			if (res.contains(" valid " + domain)) {
				isValid = true;
			}
		}
		return isValid;
	}

	/**
	 * pdb and uniprot is case-insensitive, but ENA is upper-case
	 */
	boolean isOnlineValid(String db, String id, String domain) {
		if ("doi".equals(db)) {
			return dr.isValid("doi", id);
		} else if ("refseq".equals(db)) {
			return nr.isValid("nucleotide", id);
		} else if ("refsnp".equals(db)) {
			return nr.isValid("snp", id);
		} else if ("eva".equals(db)) {
			return nr.isValid("snp", id);
		} else if ("geo".equals(db)) {
			return nr.isValid("gds", id);
		} else if ("gca".equals(db)) {
			return ar.isValid("genome_assembly", id);
		} else if ("biostudies".equalsIgnoreCase(db)) {
			return bioStudiesr.isValid(domain, id);
		} else if ("hpa".equalsIgnoreCase(db)) {
			return hpar.isValid("hpa", id);
		} else if (db.matches("ebisc|rrid|empiar|nct|complexportal|uniparc|ensembl")) {
			return responseCoder.isValid(db, id);
		} else if ("hipsci".equalsIgnoreCase(db)) {
			return hipscir.isValid(db, id);
		} else if ("gen".equalsIgnoreCase(db) && id.matches("^GCA_.+")) {
			return ar.isValid(domain, id);
		} else {
			id = ar.normalizeID(db, id);
			return ar.isValid(domain, id);
		}

	}

	private boolean isAccInBlacklist(String content) throws Exception {
		InputStream is = AnnotationFilter.class.getResourceAsStream(applicationProperty.UNIPROT_BLACKLIST_TXT);
		try(BufferedReader bf = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = bf.readLine()) != null) {
				if (line.equalsIgnoreCase(content)) {
					log.info("Accession number " + content + " is not valid because it is in the blacklist");
					return true;
				}
			}
		}
		return false;
	}

	@PostConstruct
	public void init() throws Exception {
		try {
			this.loadPredefinedResults();
			Nfa anfa = new Nfa(Nfa.NOTHING);
			anfa.or(Xml.GoofedElement("z:acc"), procEntity);
			dfa_entity = anfa.compile(DfaRun.UNMATCHED_COPY);
		} catch (Exception e) {
			log.info("context", e);
			throw e;
		}
	}

	/**
	 * Read the stored list of predefined results and fill a cache Note that nothing
	 * enforces that the file defines a MAP (there could be more than entry for the
	 * same KEY).But this code will just overwrite earlier entries with later ones
	 * if this happens.
	 *
	 * @throws IOException
	 */
	private void loadPredefinedResults() throws IOException {
		URL url = AnnotationFilter.class.getResource("/" + predefFilename);
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))){

		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.startsWith("#")) {
				int firstSpace = line.indexOf(" ");
				int secondSpace = line.indexOf(" ", firstSpace + 1);
				String accNo = line.substring(0, firstSpace);
				String db = line.substring(firstSpace + 1, secondSpace);
				cachedValidations.put(db + accNo, line);
			}
		}
		}
	}


	@Override
	public void run() throws Exception {
		DfaRun dfaRun;
		dfaRun = new DfaRun(dfa_entity);

		dfaRun.setIn(new ReaderCharSource(in));
		PrintStream outpw = new PrintStream(out);

		try {
			dfaRun.filter(outpw);
		} catch (IOException e) {
			log.info("context", e);
			throw e;
		}
	}
}
