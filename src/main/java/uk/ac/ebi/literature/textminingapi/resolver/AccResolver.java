package uk.ac.ebi.literature.textminingapi.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.utility.ApplicationProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AccResolver extends Resolver implements Resolvable {
    private static final Logger log = LoggerFactory.getLogger(AccResolver.class.getName());
    private final ApplicationProperty applicationProperty;

    public AccResolver(ApplicationProperty applicationProperty) {
        this.applicationProperty = applicationProperty;
    }

    @Override
    public boolean isValid(String domain, String accno) {

        boolean ret= false;
        if (domain.matches("cath|mint")) {
            ret = this.isOtherAccValid(domain, accno);
        } else {
            ret = this.isAccValid(domain, accno);
        }
        return ret;
    }

    private boolean isAccValid(String domain, String accno) {
        boolean ret = true;
        if ("efo".equals(domain)) {
            accno = this.extractNumbers(accno);
        } else if ("reactome".equals(domain)) {
            accno = this.extractNumbers(accno);
        }

        String query;


        try {
            if (domain.equalsIgnoreCase("biomodels")) {
                query = applicationProperty.EBI_HOST_SHORT + domain + "?query=id:\"" + accno + "\" OR submissionid:\"" + accno + "\"";
            } else if (domain.equalsIgnoreCase("omim")) {
                query = applicationProperty.EBI_HOST_SHORT + domain + "?query=id:\"" + accno + "\"";
            } else {
                query = applicationProperty.EBI_HOST_SHORT + domain + "?query=acc:\"" + accno + "\" OR id:\"" + accno + "\"";
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_XML));
            if (this.get(this.toURL(query, applicationProperty.HOST),  new HttpEntity(headers), String.class).contains("<hitCount>0</hitCount>")) {
                ret = false;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            ret = false;
            Resolver.throwException(e);
        }
        return ret;
    }

    private boolean isOtherAccValid(String domain, String accno) {
        URL url = null;
        boolean isOtherValid = false;

        try {
            if (domain.equalsIgnoreCase("cath")) {
                url = accno.contains(".") ? new URL(applicationProperty.CATH_HOST_SUPER_FAMILY + accno) : new URL(applicationProperty.CATH_HOST_DOMAIN_FAMILY + accno);
            } else if (domain.equalsIgnoreCase("mint")) {
                url = new URL(applicationProperty.MINT_HOST + accno);
            }

            assert url != null;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/xml");
            conn.setConnectTimeout(ApplicationProperty.CONN_TIMEOUT);
            if (domain.equalsIgnoreCase("cath") && (accno.toUpperCase().contains("HPA") || accno.toUpperCase().contains("CAB"))) {
                conn.setRequestProperty("content-type", "application/gzip");
            }

            if (conn.getResponseCode() == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    String line;

                    while ((line = in.readLine()) != null) {
                        if (domain.equalsIgnoreCase("cath")) {
                            isOtherValid = line.contains("\"success\":true");
                        } else if (domain.equalsIgnoreCase("mint")) {
                            isOtherValid = line.contains(accno);
                        }
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                    isOtherValid = false;
                    Resolver.throwException(e);
                }
            } else {
                isOtherValid = false;
            }


            conn.disconnect();

        } catch (Exception e) {
            isOtherValid = false;
            log.info(String.format("External validation: Accession Number %s for database %s is NOT VALID", accno, domain));
            Resolver.throwException(e);
        }

        return isOtherValid;
    }

    private String extractNumbers(String accno) {
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(accno);
        if (m.find()) {
            return m.group();
        }
        return accno;
    }


}

