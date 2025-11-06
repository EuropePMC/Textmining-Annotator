package uk.ac.ebi.literature.textminingapi.filter;

import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.organisms.TaggedElement;

import java.util.HashSet;
import java.util.Set;

/**
 * The "basic" filter, which uses scientific names mentions in the article
 * to disambiguate short names (mainly) and some common names.
 *
 * @author Romain Tertiaux
 */
@Component
public class ScientificNamesFilter extends OrgSubFilter {

	@Override
	public boolean isApplicable(TaggedElement el) {
		return ((el.isIntraAmbiguous() || el.isExtraAmbiguous()) && !el.getClassName().equals("scientific_name"));
	}

	@Override
	public String[] secondPass(TaggedElement el) {

		String[] ids = el.getIDs();

		Set<String> found = new HashSet<>();

		for (String s : ids) {
			if (ap.doesScientificMentionsContains(s)) {
				found.add(s);
			}
		}
		return found.toArray(new String[0]);
	}

	@Override
	public String getFilterName() {
		return "ScientificNamesFilter";
	}

}
