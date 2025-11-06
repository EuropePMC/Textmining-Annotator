package uk.ac.ebi.literature.textminingapi.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.utility.ApplicationProperty;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;

@Component
public class HpaResolver extends Resolver {
	private static final Logger log = LoggerFactory.getLogger(HpaResolver.class.getName());

	@Value("${hpresolver_host}")
	private String hpresolver_host;

	public boolean isGZipped(InputStream in) {
		if (!((InputStream) in).markSupported()) {
			in = new BufferedInputStream((InputStream) in);
		}

		((InputStream) in).mark(2);
		boolean var1 = false;

		int magic;
		try {
			magic = ((InputStream) in).read() & 255 | ((InputStream) in).read() << 8 & '\uff00';
			((InputStream) in).reset();
		} catch (IOException var3) {
			return false;
		}

		return magic == 35615;
	}

	@Override
	public boolean isValid(String domain, String accno) {
		boolean ret = false;
		BufferedReader in = null;
		InputStream inputStream;

		try {
			String query = hpresolver_host + URLEncoder.encode(accno, "UTF-8") + "?format=tsv";
			URL url = new URL(query);
			url.openConnection().setConnectTimeout(ApplicationProperty.CONN_TIMEOUT);
			inputStream = url.openStream();
			if (this.isGZipped(inputStream)) {
				inputStream = new GZIPInputStream(url.openStream());
			}

			in = new BufferedReader(new InputStreamReader(inputStream));

			int numLines = 0;
			while (in.readLine() != null) {
				numLines++;
			}

			ret = numLines > 1;

		} catch (Exception e) {
			ret = false;
			log.error(e.getMessage());
			Resolver.throwException(e);
		}finally{
			if (in != null) {
				try {
					in.close();
				} catch (IOException e1) {
					log.error(e1.getMessage());
				}
			}
		}
		return ret;
	}
}
