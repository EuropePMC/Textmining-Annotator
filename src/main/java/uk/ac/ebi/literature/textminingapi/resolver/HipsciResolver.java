package uk.ac.ebi.literature.textminingapi.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class HipsciResolver extends Resolver {

	private static final Logger log = LoggerFactory.getLogger(HipsciResolver.class.getName());
	@Value("${api_cell_line}")
	public String api_cell_line;

	@Override
	public boolean isValid(String domain, String accno) {
		boolean ret = false;
		try {
			if (this.get(api_cell_line + accno,  new HttpEntity(new HttpHeaders()), String.class).contains("\"found\":true")) {
				ret = true;
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			ret = false;
			Resolver.throwException(e);
		}
		return ret;
	}
	
}
