package uk.ac.ebi.literature.textminingapi.filter;

import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.organisms.TaggedElement;
import uk.ac.ebi.literature.textminingapi.utility.Taxon;

import java.util.HashSet;
import java.util.Set;

/**
 * This filter is looking for upper nodes mentions to get disambiguation clues.
 *
 * @author tertiaux
 */
@Component
public class TreeBasedFilter extends OrgSubFilter {

	@Override
	public boolean isApplicable(TaggedElement el) {
		return (el.isIntraAmbiguous() || el.isExtraAmbiguous());
	}

	@Override
	public String[] secondPass(TaggedElement el) {

		Set<String> found = new HashSet<>();

		for (String s : el.getIDs()) {

			if (new Taxon(s).getParentID() != null) {
				Taxon taxon = new Taxon(new Taxon(s).getParentID());

				int i = 0;

				/* Climb in the tree to find a mention of a parent */
				int maxLevelsClimbing = 2;
				String stopRanks = "no_rank|superkingdom";
				while (taxon != null &&
						!ap.doesScientificMentionsContains(taxon.getTaxonomyID())
						&& taxon.getRank() != null && !taxon.getRank().matches(stopRanks)
						&& i < maxLevelsClimbing) {
					taxon = new Taxon(taxon.getParentID());
					i++;
				}

				if (taxon != null && ap.doesScientificMentionsContains(taxon.getTaxonomyID())) {
					found.add(s);
				}
			}
		}

		return found.toArray(new String[0]);
	}

	@Override
	public String getFilterName() {
		return "TreeBasedFilter";
	}

}
