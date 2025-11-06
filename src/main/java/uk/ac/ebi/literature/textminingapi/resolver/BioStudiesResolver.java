package uk.ac.ebi.literature.textminingapi.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class BioStudiesResolver extends Resolver implements Resolvable {
    private static final Logger log = LoggerFactory.getLogger(BioStudiesResolver.class.getName());

    @Value("${search_query_accession}")
    public String search_query_accession;

    @Override
    public boolean isValid(String domain, String accno) {
        boolean ret = true;
        String query;
        try {
            query = search_query_accession + accno;
            if (this.get(query,  new HttpEntity(new HttpHeaders()), String.class).contains("\"totalHits\":0")) {
                ret = false;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            ret = false;
            Resolver.throwException(e);
        }
        return ret;
    }
}
