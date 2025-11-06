package uk.ac.ebi.literature.textminingapi.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * Created by jee on 27/06/17.
 */
@Component
public abstract class Resolver extends BaseRetryableApiClient implements Resolvable {
	private static final Logger log = LoggerFactory.getLogger(Resolver.class.getName());
	static {
		fixForPKIXError();
	}
	
    public String normalizeID(String db, String id) {
        int dotIndex;
        dotIndex = id.indexOf(".");
        if (dotIndex != -1 && (!"doi".equals(db)) && (!"cath".equals(db))) id = id.substring(0, dotIndex);
        if (id.endsWith(")")) id = id.substring(0, id.length() - 1);
        return id.toUpperCase();
    }
    
    protected String toURL(String params, String host) throws IOException {
      	  String request="https://"+host+"/"+params;
  	       return request;
     }
    
    /**
	 * Fix for Exception in thread "main" javax.net.ssl.SSLHandshakeException:
	 * sun.security.validator.ValidatorException: PKIX path building failed:
	 * sun.security.provider.certpath.SunCertPathBuilderException: unable to find
	 * valid certification path to requested target
	 */
    private static void fixForPKIXError() {
		try {
			/* Start of Fix */
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) { }
				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) { }

			} };

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			/* End of the fix*/
		}catch(Exception e) {
			System.err.println("Verified host error");
			//AccResolver.logOutput("Failed to ignore the certificate validation  " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Throwable> void throwException(Throwable exception) throws T
	{
		throw (T) exception;
	}

}
