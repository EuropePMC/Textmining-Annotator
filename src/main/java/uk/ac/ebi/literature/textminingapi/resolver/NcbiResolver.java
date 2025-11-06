package uk.ac.ebi.literature.textminingapi.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;

@Component
public class NcbiResolver extends Resolver implements Resolvable {

    private static final Logger log = LoggerFactory.getLogger(NcbiResolver.class.getName());
    @Value("${ncbi_host}")
    private String host;

    @Override
    public boolean isValid(String domain, String accno) {
        return this.isAccValid(domain, accno);
    }


    private boolean isAccValid(String domain, String accno) {
        boolean ret = true;
        BufferedReader in = null;

        try {
            String query = "entrez/eutils/esearch.fcgi?api_key=59fdb1e97d6c82c703206a006ad284447208&db=" + domain + "&term=" + accno;
            if (this.get(this.toURL(query, host),  new HttpEntity(new HttpHeaders()), String.class).contains("<Count>0</Count>")) {
                ret = false;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            ret = false;
            Resolver.throwException(e);
    } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }

        return ret;
    }
}
