package uk.ac.ebi.literature.textminingapi.service;

import monq.jfa.DfaRun;
import monq.jfa.ReaderCharSource;
import monq.programs.DictFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.literature.textminingapi.filter.Filter;
import uk.ac.ebi.literature.textminingapi.resolver.InputStreamCustomizer;
import uk.ac.ebi.literature.textminingapi.resolver.Resolver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BaseDictFilter {

	private static final Logger log = LoggerFactory.getLogger(BaseDictFilter.class);
	private static final String utf8 = StandardCharsets.UTF_8.name();

	private String dictName;
	private DictFilter dictFilter;
	private Map<String, Filter> filters;
	private InputStreamCustomizer customizer;
	private String ftId;

	public BaseDictFilter(String ftId, String dictName, DictFilter dictFilter) {
		this.ftId=ftId;
		this.dictName = dictName;
		this.dictFilter = dictFilter;
		this.filters = Map.of();
	}

	public BaseDictFilter(String ftId, String dictName, DictFilter dictFilter, Map<String, Filter> filters) {
		this.ftId=ftId;
		this.dictName = dictName;
		this.dictFilter = dictFilter;
		this.filters = (filters == null) ? Map.of() : filters;
	}

	public void setInputStreamCustomizer(InputStreamCustomizer customizer) {
		this.customizer = customizer;
	}

	public InputStreamCustomizer getInputStreamCustomizer() {
		return this.customizer;
	}

	public InputStream run(ReaderCharSource readerCharSource) throws Exception {
		log.info("{" + ftId + "} Running Dictionary {" + dictName + "}");
		InputStream returnStream = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String utf8 = StandardCharsets.UTF_8.name();
		PrintStream ps = new PrintStream(baos, true, utf8);
		DfaRun r = dictFilter.createRun();
		r.setIn(readerCharSource);
		r.filter(ps);
		returnStream = new ByteArrayInputStream(baos.toString(utf8).getBytes());
		log.info("{" + ftId + "} Finished Running Dictionary {" + dictName + "}");
		return returnStream;
	}

	public InputStream runfilter(InputStream in) throws Exception {
		try{
			for (String filterName : filters.keySet()) {
				log.info("{" + ftId + "} Running Filter {" + filterName + "}");
				Filter filter = filters.get(filterName);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				filter.setOut(baos);
				filter.setIn(in);
				filter.run();
				InputStream processed = new ByteArrayInputStream(baos.toString(utf8).getBytes());
				in.close();
				in = processed;
				log.info("{" + ftId + "} Finished Running Filter {" + filterName + "}");
			}
			//due to our resources limitation, we had to catch outofmemory issues to not disrupt our application process
		}catch(OutOfMemoryError e){
			log.info("out of memory exception occured in filter step", e);
			Resolver.throwException(new Exception("out of memory exception occured in filter step"));
		}catch (Exception e){
			throw e;
		}
		return in;
	}

}
