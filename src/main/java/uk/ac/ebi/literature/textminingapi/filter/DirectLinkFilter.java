package uk.ac.ebi.literature.textminingapi.filter;

import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.organisms.TaggedElement;
import uk.ac.ebi.literature.textminingapi.utility.Taxon;

/**
 * A filter which deals with genuses / subgenuses ambiguities
 *
 * @author tertiaux
 * @author zshafique
 */
@Component
public class DirectLinkFilter extends OrgSubFilter {

    @Override
    public boolean isApplicable(TaggedElement el) {
        return (el.isIntraAmbiguous() && !el.isExtraAmbiguous());
    }

    @Override
    public String[] secondPass(TaggedElement el) {

        String[] ids = null;

        if (el.isIntraAmbiguous()) // only applies to intra taxonomy ambiguities
        {
            ids = el.getIDs();

            if (ids.length == 2 && (new Taxon(ids[1]).isDirectSubnodeOf(ids[0]) || new Taxon(ids[0]).isDirectSubnodeOf(ids[1]))) {
                if (new Taxon(ids[1]).isDirectSubnodeOf(ids[0])) {
                    ids = new String[]{ids[1]};
                } else {
                    ids = new String[]{ids[0]};
                }
            }

        }
        return ids;
    }

    @Override
    public String getFilterName() {
        return "DirectLinkFilter";
    }

}
