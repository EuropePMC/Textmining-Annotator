package uk.ac.ebi.literature.textminingapi.filter;

import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.organisms.TaggedElement;
import uk.ac.ebi.literature.textminingapi.utility.Taxon;

/**
 * This filter doesn't use the context to disambiguate, but a global measure of popularity.
 * The measure of popularity is loaded from a serialized HashMap.
 * It should come from the Entrez Records, linking NCBI Taxonomy to PMC articles.
 *
 * @author Romain Tertiaux
 */
@Component
public class PMCPopularityFilter extends OrgSubFilter {

	@Override
	public boolean isApplicable(TaggedElement el) {
		return (el.isIntraAmbiguous() && !el.isExtraAmbiguous());
	}

	/**
	 * The output of this filter is always one ID
	 *
	 * @param el
	 * @return
	 */
	@Override
	public String[] secondPass(TaggedElement el) {
		return new String[]{Taxon.mostPopular(el.getIDs())};
	}

	@Override
	public String getFilterName() {
		return "PMCPopularityFilter";
	}


}
