package uk.ac.ebi.literature.textminingapi.filter;

import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.organisms.TaggedElement;
import uk.ac.ebi.literature.textminingapi.utility.Taxon;

import java.util.HashSet;
import java.util.Set;

/**
 * A filter which uses the taxonomy tree, looking for the subnodes of candidates.
 *
 * @author Romain Tertiaux
 */
@Component
public class DescTreeBasedFilter extends OrgSubFilter {

	@Override
	public boolean isApplicable(TaggedElement el) {
		return (el.isIntraAmbiguous() || el.isExtraAmbiguous());

	}

	@Override
	public String[] secondPass(TaggedElement el) {

		String[] ids = el.getIDs();
		Set<String> found = new HashSet<>();

		for (String candidate : ids) {

			if (Taxon.getChildren(candidate) != null) {

				for (String candidateChildren : Taxon.getChildren(candidate)) {


					if (ap.doesScientificMentionsContains(candidateChildren)) {
						found.add(candidate);
					}

				}
			}

		}

		return found.toArray(new String[0]);
	}

	@Override
	public String getFilterName() {
		return "DescTreeBasedFilter";
	}

}
