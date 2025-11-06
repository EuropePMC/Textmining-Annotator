package uk.ac.ebi.literature.textminingapi.service;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import monq.jfa.ReaderCharSource;
import monq.net.ServiceUnavailException;
import monq.stuff.EncodingDetector;
import uk.ac.ebi.literature.textminingapi.resolver.InputStreamCustomizer;

public class PipelineProcessor implements Processor<InputStream, InputStream> {

	private List<BaseDictFilter> baseDictFilters;

	public PipelineProcessor(List<BaseDictFilter> baseDictFilters) {
		this.baseDictFilters = baseDictFilters;
	}

	@Override
	public InputStream process(InputStream input) throws Exception {
		for (BaseDictFilter baseDictFilter : baseDictFilters) {
			ReaderCharSource readerCharSource = getReaderCharSource(input);
			InputStream processed = baseDictFilter.run(readerCharSource);
			InputStreamCustomizer customizer = baseDictFilter.getInputStreamCustomizer();
			if (customizer != null)
				processed = customizer.cutomize(processed);
			processed = baseDictFilter.runfilter(processed);
			input.close();
			input = processed;
		}
		return input;
	}


	private final ReaderCharSource getReaderCharSource(InputStream in)
			throws ServiceUnavailException {
		Reader rin;
		if (!in.markSupported()) {
			in = new BufferedInputStream(in);
		}
		String enc = null;
		try {
			enc = EncodingDetector.detect(in, 1000, "UTF-8");
			rin = new InputStreamReader(in, enc);
		} catch (UnsupportedEncodingException e) {
			throw new ServiceUnavailException("unsupported encoding `" + enc + "' found in file", e);
		} catch (IOException e) {
			throw new ServiceUnavailException("problems reading encoding", e);
		}

		ReaderCharSource rc = new ReaderCharSource(rin);
		return rc;
	}

}
