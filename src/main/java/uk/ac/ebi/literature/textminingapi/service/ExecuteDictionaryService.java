package uk.ac.ebi.literature.textminingapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.literature.textminingapi.filter.*;
import uk.ac.ebi.literature.textminingapi.loader.LoadDictionary;
import uk.ac.ebi.literature.textminingapi.resolver.InputStreamCustomizer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExecuteDictionaryService {
	private static final Logger log = LoggerFactory.getLogger(ExecuteDictionaryService.class);

	private Processor<InputStream, InputStream> processor;
	private final LoadDictionary dictionary;
	private final FileService fileService;
	private final AnnotationFilter annotationFilter;
	private final BncFilter bncFilter;
	private final ChEBIBncFilter chEBIBncFilter;
	private final ChEBIFilter chEBIFilter;
	private final GeneFilter geneFilter;
	private final OrganismsFilter organismsFilter;

	public ExecuteDictionaryService(LoadDictionary dictionary, FileService fileService, AnnotationFilter annotationFilter, BncFilter bncFilter, ChEBIBncFilter chEBIBncFilter, ChEBIFilter chEBIFilter, GeneFilter geneFilter, OrganismsFilter organismsFilter) {
		this.dictionary = dictionary;
		this.fileService = fileService;
		this.annotationFilter = annotationFilter;
		this.bncFilter = bncFilter;
		this.chEBIBncFilter = chEBIBncFilter;
		this.chEBIFilter = chEBIFilter;
		this.geneFilter = geneFilter;
		this.organismsFilter = organismsFilter;
	}

	public void executeAllDictionaries(String inputFile, String outputFile, String ftID) throws Exception {
		try {
			log.info("{" + ftID + "} Running Dictionaries on file  {" + inputFile + "}");
			InputStream input = fileService.getInputStream(inputFile);

			List<BaseDictFilter> baseDictFilters = this.getBaseDictFilters(ftID);

			processor = new PipelineProcessor(baseDictFilters);
			InputStream finalOutput = processor.process(input);
			log.info("{" + ftID + "} Finished Running Dictionaries on file  {" + inputFile + "}");
			fileService.write(this.removePlainTag(finalOutput), outputFile);
		} catch (Exception e) {
			log.error("Error in executeAllDictionaries : " + e);
			throw e;
		}
	}

	private final List<BaseDictFilter> getBaseDictFilters(String ftId) {
		BaseDictFilter accDict = new BaseDictFilter(ftId,"Accession", dictionary.getAccessionDictionary());
		BaseDictFilter resourceDictFilter = new BaseDictFilter(ftId,"Resource", dictionary.getResourceDictionary(),
				Map.of("Annotation", annotationFilter));
		BaseDictFilter swissportDictFilter = new BaseDictFilter(ftId,"Gene", dictionary.getSwissPortDictionary(),
				Map.of("BNC", bncFilter, "Gene", geneFilter));
		BaseDictFilter orgDict = new BaseDictFilter(ftId,"Organisms", dictionary.getOrganisamDictionary(),
				Map.of("Organisms", organismsFilter));
		orgDict.setInputStreamCustomizer(new OrganismsCustomizer());

		BaseDictFilter experimantalDictFilter = new BaseDictFilter(ftId,"Experimental",
				dictionary.getExperimentalMethodDictionary());
		BaseDictFilter goDict = new BaseDictFilter(ftId,"GO", dictionary.getGoDictionary());
		BaseDictFilter disDict = new BaseDictFilter(ftId,"Diseases", dictionary.getDiseaseDictionary());
		Map<String, Filter> chemicalFilters = new LinkedHashMap<>();
		chemicalFilters.put("Chemicals BNC", chEBIBncFilter);
		chemicalFilters.put("Chemicals", chEBIFilter);
		BaseDictFilter chemicalDict = new BaseDictFilter(ftId,"Chemicals", dictionary.getChemicalDictionary(),
				chemicalFilters);

		return List.of(accDict, resourceDictFilter, swissportDictFilter, orgDict, experimantalDictFilter, goDict,disDict, chemicalDict);
	}

	private InputStream appendPlainTag(InputStream inputStream) {
		String text = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining(System.lineSeparator()));
		String s = text.contains("<z:") ? (new StringBuilder("<plain>").append(text).append("</plain>").toString())
				: text.toString();
		return new ByteArrayInputStream(s.getBytes());
	}

	private InputStream removePlainTag(InputStream inputStream) {
		String text = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining(System.lineSeparator()));
		return new ByteArrayInputStream(text.replaceAll("<plain>", "").replaceAll("</plain>", "").getBytes());
	}

	private class OrganismsCustomizer implements InputStreamCustomizer {
		@Override
		public InputStream cutomize(InputStream is) {
			return ExecuteDictionaryService.this.appendPlainTag(is);
		}
	}

}
