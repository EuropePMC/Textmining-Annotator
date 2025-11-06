package uk.ac.ebi.literature.textminingapi.filter;

import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.ac.ebi.literature.textminingapi.organisms.ArticleProcessor;
import uk.ac.ebi.literature.textminingapi.organisms.TaggedElement;
import uk.ac.ebi.literature.textminingapi.resolver.Resolver;
import uk.ac.ebi.literature.textminingapi.utility.ApplicationProperty;
import uk.ac.ebi.literature.textminingapi.utility.Taxon;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.PrintStream;

/**
 * The service implementing the organisms filter. The program takes XML articles in the input stream, annotated with the research version of the species tagger
 *
 * @author Romain Tertiaux
 * @author zshafique
 */
@Service
@Lazy
public class OrganismsFilter extends Filter {
	private static final Logger log = LoggerFactory.getLogger(OrganismsFilter.class.getName());
	final Taxon taxon;
	final TaggedElement taggedElement;
	final ApplicationProperty applicationProperty;

	public OrganismsFilter(Taxon taxon, TaggedElement taggedElement, ApplicationProperty applicationProperty) {
		this.taxon = taxon;
		this.taggedElement = taggedElement;
		this.applicationProperty = applicationProperty;
	}

	@PostConstruct
	public void init() throws IOException {
		Taxon.setEBIDumpFile(applicationProperty.taxonomy_ebi_file);
		Taxon.setNCBIDumpPath(applicationProperty.taxonomy_ncbi_dir);
		Taxon.setPopularityFile(applicationProperty.pmc_popularity);
		Taxon.loadMapFromNCBIFilesIfNeeded(false);
		Taxon.makeChildrenTableIfNeeded();
		Taxon.loadPopularityTableFromTextFile();
		taggedElement.loadExtraAmbiguousNames();
	}

	@Override
	public void run() throws Exception {
		try {

			Nfa SPnfa = new Nfa(Nfa.NOTHING);
			SPnfa = SPnfa.or(Xml.GoofedElement(applicationProperty.PLAIN), this.new WindowDetector());

			Dfa SPdfa = SPnfa.compile(DfaRun.UNMATCHED_COPY);
			DfaRun SPr = new DfaRun(SPdfa);
			SPr.setIn(new ByteCharSource(in));
			SPr.filter(new PrintStream(out));
		} catch (Exception e) {
			log.error(e.getMessage());
			throw e;
		}
	}

	/**
	 * Monq class which is aimed to split articles or plain texts
	 *
	 * @author tertiaux
	 */
	public class WindowDetector extends AbstractFaAction {

		private static final long serialVersionUID = 7535177016959802023L;

		@Override
		public void invoke(StringBuilder out, int start, DfaRun runner) {
			try {
				ArticleProcessor ap = new ArticleProcessor(out.substring(start));
				ap.registerFilter(new DirectLinkFilter());
				ap.registerFilter(new ProperNamesFilter());
				ap.registerFilter(new ScientificNamesFilter());
				ap.registerFilter(new DescTreeBasedFilter());
				ap.registerFilter(new TreeBasedFilter());
				ap.registerFilter(new PMCPopularityFilter());

				out.delete(start, start + out.substring(start).length()).append(ap.process());
			} catch (Exception e) {
				log.error(e.getMessage());
				Resolver.throwException(e);
			}
		}


	}
}
