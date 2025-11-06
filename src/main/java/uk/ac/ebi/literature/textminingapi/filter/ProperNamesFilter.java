package uk.ac.ebi.literature.textminingapi.filter;

import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.organisms.TaggedElement;

/**
 * OrgSubFilter which get rid of some false positives (proper names followed by "et al.")
 *
 * @author Romain Tertiaux
 */
@Component
public class ProperNamesFilter extends OrgSubFilter {

	@Override
	public boolean isApplicable(TaggedElement el) {
		return (el.getNextWords().startsWith("et al"));
	}


	@Override
	public String[] secondPass(TaggedElement el) {
		if (el.getNextWords().startsWith("et al")) {
			return new String[]{};
		} else {
			return el.getIDs();
		}
	}


	@Override
	public String getFilterName() {
		return "ProperNamesFilter";
	}

}
