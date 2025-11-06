package uk.ac.ebi.literature.textminingapi.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ResponseCodeResolver extends Resolver {
	public static final String EMPIAR_API_ENTRY = "https://www.ebi.ac.uk/pdbe/emdb/empiar/api/entry/";
	public static final String EBISC_ORG = "https://cells.ebisc.org/";
	public static final String ORG_RESOLVER = "https://scicrunch.org/resolver/";
	public static final String CT_2_SHOW = "https://clinicaltrials.gov/ct2/show/";
	public static final String WS_COMPLEX = "https://www.ebi.ac.uk/intact/complex-ws/complex/";
	public static final String UNIPROT = "https://www.uniprot.org/uniparc/";
	public static final String ARCHIVE_ID = "http://rest.ensembl.org/archive/id/";
	private static final Logger log = LoggerFactory.getLogger(ResponseCodeResolver.class.getName());

	@Override
	public boolean isValid(String domain, String accno) {
		boolean ret = false;
		try {
			String url = null;
			if (domain.equalsIgnoreCase("empiar")) {
				url = EMPIAR_API_ENTRY + accno;
			} else if (domain.equalsIgnoreCase("ebisc")) {
				url = EBISC_ORG + accno;
			} else if (domain.equalsIgnoreCase("rrid")) {
				url = ORG_RESOLVER + accno;
			} else if (domain.equalsIgnoreCase("nct")) {
				url = CT_2_SHOW + accno;
			} else if (domain.equalsIgnoreCase("complexportal")) {
				url = WS_COMPLEX + accno;
			} else if (domain.equalsIgnoreCase("uniparc")) {
				url = UNIPROT + accno + ".xml";
			} else if (domain.equalsIgnoreCase("ensembl")) {
				url = ARCHIVE_ID + accno;
			}
			if (HttpStatus.OK.equals(this.getResponse(url,  new HttpEntity(new HttpHeaders()), String.class))) {
				ret = true;
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			Resolver.throwException(e);
		}
		return ret;
	}
}
