package uk.ac.ebi.literature.textminingapi.filter;

import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.organisms.ArticleProcessor;
import uk.ac.ebi.literature.textminingapi.organisms.TaggedElement;

@Component
public abstract class OrgSubFilter {

	ArticleProcessor ap;

	/**
	 * Points out whether the filter can be called with the given element.
	 * Allows to specify some constraints on the tagged element in order to have the OrgSubFilter working
	 *
	 * @param el
	 * @return
	 */
	public boolean isApplicable(TaggedElement el) {
		return true;
	}

	/**
	 * First pass process of the filter.
	 * Basically, should only do read-only operations.
	 *
	 * @param el
	 */
	public void firstPass(TaggedElement el) {

		if (el.isScientific() && !el.isIntraAmbiguous() && ap.locationShouldBeCollected()) {
			ap.notifySpeciesMention(0, el.getID());
		}
	}

	/**
	 * Second pass process of the filter.
	 * Filters the IDs of the given element.
	 *
	 * @param el TaggedElement The element
	 * @return the new list of IDs of the element.
	 */
	public String[] secondPass(TaggedElement el) {
		return el.getIDs();
	}

	/**
	 * Sets the {@link ArticleProcessor} which owns the filter.
	 *
	 * @param inAp
	 */
	public void setArticleProcessor(ArticleProcessor inAp) {
		ap = inAp;
	}

	/**
	 * Gives the name of the filter, for logging purposes.
	 *
	 * @return
	 */
	public String getFilterName() {
		return "Abstract OrgSubFilter (this string should never been displayed)";
	}

}
