package uk.ac.ebi.literature.textminingapi.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.utility.ApplicationProperty;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Component
@Lazy
public class DoiResolver extends Resolver implements Resolvable {
   private static final Logger log = LoggerFactory.getLogger(DoiResolver.class.getName());
   private static Map<String, String> BlacklistDoiPrefix = new HashMap<>();

   private final ApplicationProperty applicationProperty;

   public DoiResolver(ApplicationProperty applicationProperty) {
      this.applicationProperty = applicationProperty;
   }

   @PostConstruct
   private void loadDOIPrefix() throws IOException {
      URL pURL = DoiResolver.class.getResource("/" + applicationProperty.doiPrefixFilename);
      pURL.openConnection().setConnectTimeout(ApplicationProperty.CONN_TIMEOUT);
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(pURL.openStream()))){
         String line;
         while ((line = reader.readLine()) != null) {
            if (line.indexOf("#") != 0) {
               int firstSpace = line.indexOf(" ");
               String prefix = line.substring(0, firstSpace);
               BlacklistDoiPrefix.put(prefix, "Y");
            }
         }
      } catch (Exception e) {
         log.error("Error loading DOI blacklist file", e);
      }
   }

   @Override
   public boolean isValid(String sem_type, String doi) {
      boolean ret;
      if (BlacklistDoiPrefix.containsKey(this.prefixDOI(doi))) {
         ret = false;
      } else if ("10.2210/".equals(doi.substring(0, 8))) { // exception rule for PDB data center
         ret = true;
      } else {
         ret = this.isDOIValid("doi", doi);
      }

      if (ret) {
         log.info(String.format("Datacite validation : Accession Number %s for database doi is VALID", doi));
      }else {
         log.info(String.format("Datacite validation : Accession Number %s for database doi is NOT VALID", doi));
      }

      return ret;
   }

   String prefixDOI(String doi) {
      String prefix = "";
      int bsIndex = doi.indexOf("/");
      if (bsIndex != -1) {
         prefix = doi.substring(0, bsIndex);
      }
      return prefix;
   }

   private boolean isDOIValid(String domain, String doi) {
      boolean ret = false;
      try {
         if (this.get(this.toURL(doi, applicationProperty.DOI_HOST),  new HttpEntity(new HttpHeaders()), String.class).contains("\"resource-type-id\":\"dataset\"")) {
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
