package uk.ac.ebi.literature.textminingapi.loader;

import monq.jfa.CompileDfaException;
import monq.jfa.ReSyntaxException;
import monq.programs.DictFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Component
public class LoadDictionary {

	private static final Logger log = LoggerFactory.getLogger(LoadDictionary.class);
	private static final String elementType = "plain";
	@Value("${dictionary.or}")
	private String organisamDic;
	@Value("${dictionary.di}")
	private String diseaseDic;
	@Value("${dictionary.re}")
	private String resourceDic;
	@Value("${dictionary.ac}")
	private String accessionDic;
	@Value("${dictionary.sp}")
	private String swissportDic;
	@Value("${dictionary.met}")
	private String experimentalMethodDic;
	@Value("${dictionary.go}")
	private String goDic;
	@Value("${dictionary.ch}")
	private String chemicalDic;
	@Value("${dictionary-input-type}")
	private String inputType;
	private DictFilter diseaseDictionary;
	private DictFilter chemicalDictionary;
	private DictFilter organisamDictionary;
	private DictFilter resourceDictionary;
	private DictFilter accessionDictionary;
	private DictFilter swissPortDictionary;
	private DictFilter experimentalMethodDictionary;
	private DictFilter goDictionary;

	@Value("${dictionary.loading.enable}")
	private String enabledLoading;

	@PostConstruct
	public void init() throws FileNotFoundException, IOException, ReSyntaxException, CompileDfaException {
		if (enabledLoading.equals("true")) {
			log.info("dictionaries loading started");
			accessionDictionary = loadDictionary(accessionDic);
			diseaseDictionary = loadDictionary(diseaseDic);
			chemicalDictionary = loadDictionary(chemicalDic);
			organisamDictionary = loadDictionary(organisamDic);
			resourceDictionary = loadDictionary(resourceDic);
			swissPortDictionary = loadDictionary(swissportDic);
			experimentalMethodDictionary = loadDictionary(experimentalMethodDic);
			goDictionary = loadDictionary(goDic);
			log.info("dictionaries have been loaded");
		}
	}

	public DictFilter loadDictionary(String dictionary)
			throws FileNotFoundException, IOException, ReSyntaxException, CompileDfaException {
		InputStream mwtFile;
		mwtFile = getClass().getResourceAsStream(dictionary);

		DictFilter dict = new DictFilter(new InputStreamReader(mwtFile), inputType, elementType, true);
		mwtFile.close();
		return dict;
	}

	public DictFilter getDiseaseDictionary() {
		return diseaseDictionary;
	}

	public DictFilter getChemicalDictionary() {
		return chemicalDictionary;
	}

	public DictFilter getOrganisamDictionary() {
		return organisamDictionary;
	}

	public DictFilter getResourceDictionary() {
		return resourceDictionary;
	}

	public DictFilter getAccessionDictionary() {
		return accessionDictionary;
	}

	public DictFilter getSwissPortDictionary() {
		return swissPortDictionary;
	}

	public DictFilter getExperimentalMethodDictionary() {
		return experimentalMethodDictionary;
	}

	public DictFilter getGoDictionary() {
		return goDictionary;
	}

}
