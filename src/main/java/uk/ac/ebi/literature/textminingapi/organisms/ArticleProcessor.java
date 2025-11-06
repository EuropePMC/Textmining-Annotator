package uk.ac.ebi.literature.textminingapi.organisms;

import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.literature.textminingapi.filter.OrgSubFilter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ArticleProcessor is the class which manages to apply different filter to an input article, and which gives the result.
 *
 * @author Romain Tertiaux
 */
public class ArticleProcessor {

	private static final Logger log = LoggerFactory.getLogger(ArticleProcessor.class.getName());
	public boolean inArticle = false;
	public boolean inFront = false;
	public boolean inTitleGroup = false;
	public boolean inTitle = false;
	public boolean inAbstract = false;
	public boolean inBody = false;
	public boolean inBack = false;
	public boolean inPlain = false;
	public int currentSentence = -1;
	String articleString;
	private final List<OrgSubFilter> filtersList = new ArrayList<>();
	// Data used by filters
	private final Set<String> scientificMentions = new HashSet<>();

	/**
	 * Constructor
	 *
	 * @param inArticle String The annotated full text
	 * @throws FileNotFoundException
	 */
	public ArticleProcessor(String inArticle) throws FileNotFoundException {

		articleString = inArticle;
	}

	/**
	 * Add a filter to the list
	 *
	 * @param inFilter
	 */
	public void registerFilter(OrgSubFilter inFilter) {
		filtersList.add(inFilter);
		inFilter.setArticleProcessor(this);
	}

	/**
	 * Indicates whether the article is being read at a point which should contain taxonomy tags
	 *
	 * @return
	 */
	public boolean locationShouldBeTagged() {
		return ((!inArticle && inPlain) || (inArticle && (inTitle || inAbstract || inBody)));
	}

	/**
	 * Indicates whether the article is being read at a point whose taxonomy tags should be counted as relevant mentions to the article
	 *
	 * @return
	 */
	public boolean locationShouldBeCollected() {
		return ((!inArticle && inPlain) || (inArticle && (inTitle || inAbstract || inBody)));
	}

	/**
	 * Add a element to the list collecting scientific mentions
	 *
	 * @param sentence
	 * @param taxID
	 */
	public void notifySpeciesMention(int sentence, String taxID) {
		scientificMentions.add(taxID);
	}

	/**
	 * Collect the mentions, then apply the filters to the article
	 *
	 * @return
	 * @throws ReSyntaxException
	 * @throws CompileDfaException
	 * @throws IOException
	 */
	public String process() throws ReSyntaxException, CompileDfaException, IOException {
		Nfa SPnfa = new Nfa(Nfa.NOTHING);

		SPnfa = SPnfa.or("<z:species ids=\"([^\"]*)\"[^>]*>[^<]*</z:species>", this.new ElementFirstPass());
		SPnfa = SPnfa.or("<article[^>]*>|</article>|<plain>|</plain>|<front>|</front>|<body>|</body>|<back>|</back>|<sub-article>|</sub-article>|<response>|<response>|<SENT sid=\"[0-9]*\"[^>]*>|</SENT>|<p>|</p>|<article-title>|</article-title>|<abstract>|</abstract>", new LocationUpdater());

		Dfa SPdfa = SPnfa.compile(DfaRun.UNMATCHED_DROP);

		DfaRun SPr = new DfaRun(SPdfa);
		SPr.clientData = this;
		SPr.filter(articleString);
		SPnfa = new Nfa(Nfa.NOTHING);

		SPnfa = SPnfa.or("(([^<> ]* [^<> ]* )?<z:species ids=\"([^\"]*)\"[^>]>[^<]*</z:species>([^<> ]* [^<> ]* )?)|(<z:species ids=\"([^\"]*)\"[^>]*>[^<]*</z:species>)", this.new ElementSecondPass());

		SPdfa = SPnfa.compile(DfaRun.UNMATCHED_COPY);

		SPr = new DfaRun(SPdfa);
		SPr.clientData = this;

		return SPr.filter(articleString);


	}

	/**
	 * Applies filter (first pass) to the given tag
	 *
	 * @param el
	 */
	public void appliesFirstPassFilters(TaggedElement el) {
		for (OrgSubFilter f : filtersList) {
			f.firstPass(el);
		}
	}

	/**
	 * Applies filters (second pass) to the given tag
	 *
	 * @param el
	 * @return
	 */
	public String appliesFilters(TaggedElement el) {
		// Ambiguity with another kind of concept (ex : Cancer, genus / disease)
		if (el.isExtraAmbiguous() || (!el.isExtraAmbiguous() && !el.isIntraAmbiguous())) // second case : "et al.", etc
		{
			boolean falsepositive = true;

			for (OrgSubFilter f : filtersList) {
				if (f.isApplicable(el)) {
					if (f.isApplicable(el) && el.isSolvableWith(f)) {
						falsepositive = false;
					}

				}
			}

			if (falsepositive) {
				el.setIDs(new String[0]); // this is used for the very few cases where the term is extra ambiguous and intra ambiguous
				return el.getTerm();
			} else {
				return el.toString();
			}

		}

		if (el.isIntraAmbiguous()) {

			String[] ids = null;

			for (OrgSubFilter f : filtersList) {
				if (f.isApplicable(el)) {
					ids = f.secondPass(el);

					if (ids != null && ids.length == 1) {
						el.setIDs(ids);
					}
				}
			}


			return el.toString();
		} else {
			return el.toString();
		}

	}

	/**
	 * Indicates whether a taxonomy ID has been found in the article or not
	 *
	 * @param candidateChildren
	 * @return
	 */
	public boolean doesScientificMentionsContains(String candidateChildren) {
		return scientificMentions.contains(candidateChildren);
	}

	/**
	 * Monq Action for the first pass of the filter : collecting data
	 *
	 * @author Romain Tertiaux
	 */
	public class ElementFirstPass extends AbstractFaAction {

		private static final long serialVersionUID = 1091976024208319026L;

		@Override
		public void invoke(StringBuilder out, int start, DfaRun runner) {
			ArticleProcessor articleProcessor = (ArticleProcessor) runner.clientData;
			TaggedElement element = new WhatIzItTaggedElement(out.substring(start));
			articleProcessor.appliesFirstPassFilters(element);
		}


	}

	/**
	 * Monq action for the second pass of the filter : fixing tags
	 *
	 * @author Romain Tertiaux
	 */
	public class ElementSecondPass extends AbstractFaAction {

		private static final long serialVersionUID = -4321617563608057005L;

		@Override
		public void invoke(StringBuilder out, int start, DfaRun runner) {
			ArticleProcessor ap = (ArticleProcessor) runner.clientData;

			TaggedElement el = new WhatIzItTaggedElement(out.substring(start));
			String ele;

			if (el.isExtraAmbiguous() || el.isIntraAmbiguous()) {
				ele = ap.appliesFilters(el);
			} else {
				ele = el.toString();
			}

			int tagStart = out.indexOf(el.getStartDelimiter(), start);

			out.delete(tagStart, out.indexOf(el.getEndDelimiter(), tagStart) + el.getEndDelimiter().length());

			out.insert(tagStart, ele);

		}


	}

}
