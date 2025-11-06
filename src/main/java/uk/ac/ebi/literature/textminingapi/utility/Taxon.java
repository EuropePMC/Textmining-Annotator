package uk.ac.ebi.literature.textminingapi.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Represents a node of the NCBI Taxonomy tree. This class relies on dump files from the NCBI Taxonomy. Two dump files format can be used : - The NCBI dump files - The EBI dump file The first one is used by default. If the files are not found, then the program attempt to use the EBI file. If you want to use the EBI file, use the static method loadFromEBIFilesIfNeeded() before any instanciation. The paths of text files of the databases have default values, but any program should first change these default values Taxon.setNCBIDumpPath(...); Taxon.setEBIDumpFile(...); Taxon.loadMapIfNeeded(); // eventually, or it will done when the first Taxon will be instantiated
 *
 * @author Romain Tertiaux
 * @since April 2009
 */
@Component
public class Taxon {
	private static final Logger log = LoggerFactory.getLogger(Taxon.class.getName());
	// These are default values, any program should first redefine them with the static methods set...Path
	private static Map<String, TaxonInfo> generalInfoMap = null;
	private static Map<String, HashSet<String>> childrenMap = null;
	private static HashMap<String, Integer> popularityMap = null;
	// Path of the NCBItaxonomy dump file in the EBI format
	private static String TAXONOMY_DB;
	// Directory of the NCBI taxonomy dump file in the EBI format
	private static String NCBI_DIR;
	// Directory of the popularity table text file
	private static String POPULARITY_FILE;
	private String taxonomyID;
	private String rank;
	private String parentID;
	private String speciesName;

	/**
	 * Constructor. The taxonomy table will be loaded when the first instance of any taxon will be created.
	 *
	 * @param inID Taxonomy ID of the taxon
	 */
	public Taxon(String inID) {
		loadMapIfNeeded();

		taxonomyID = inID;
		this.getInformationFromID();
	}

	public Taxon() {
	}

	/**
	 * Load the map from the EBI dump file.
	 *
	 */
	public static void loadMapFromEBIFileIfNeeded(boolean withNames) throws IOException {
		if (generalInfoMap != null) {

			generalInfoMap = new Hashtable<>();
			log.info("Loading the species table from " + TAXONOMY_DB + "...");

			try(BufferedReader in = new BufferedReader(new InputStreamReader(Taxon.class.getResourceAsStream(TAXONOMY_DB)))) {

				String line;
				String readId;
				String parentId;
				String specieName;
				String rank;

				while ((line = in.readLine()) != null) {

					while (line != null && !line.startsWith("ID")) {
						line = in.readLine();
					}

					if (line != null) {

						readId = line.replaceFirst("ID(?:[ ]*): ([0-9]*)", "$1");
						line = in.readLine();
						parentId = line.replaceFirst("PARENT ID(?:[ ]*): ([0-9]*)", "$1");
						line = in.readLine();
						rank = line.replaceFirst("RANK(?:[ ]*): ([a-zA-Z ]*)", "$1");

						while (line != null && !line.startsWith("SCIENTIFIC NAME")) {
							line = in.readLine();
						}

						if (line != null) {
							specieName = line.replaceFirst("SCIENTIFIC NAME(?:[ ]*): ([a-zA-Z ]*)", "$1");

							TaxonInfo temp = new TaxonInfo(rank, parentId);
							if (withNames) {
								temp.setName(specieName);
							}

							generalInfoMap.put(readId, temp);

						}

					}


				}
			}

			log.info(generalInfoMap.size() + " elements loaded.");

		}
	}

	/**
	 * Returns all the taxonomy IDs which are in the HashMap
	 *
	 */
	public static Set<String> getAllTaxIDs() {
		return generalInfoMap.keySet();
	}

	/**
	 * Default method to load the taxonomy HashMap.
	 * Tries to load it with the NCBI files. If it fails, then it will try to use the EBI file.
	 */
	public static void loadMapIfNeeded() {
		if (generalInfoMap == null) {
			try {
				loadMapFromNCBIFilesIfNeeded(false);
			} catch (IOException e) {
				try {
					generalInfoMap = null;
					loadMapFromEBIFileIfNeeded(false);
				} catch (IOException e1) {
					log.error("Both NCBI and EBI dumps for the taxonomy are unavailable.");
					System.exit(-1);
				}
			}
		}
	}

	/**
	 * Load the map from THE NCBI taxonomy dump files.
	 *
	 */
	public static void loadMapFromNCBIFilesIfNeeded(boolean withNames) throws IOException {
		if (generalInfoMap == null) {

			String line;
			String[] fields;

			log.info("Loading the species table from the NCBI dump files...");

			generalInfoMap = new Hashtable<>();

			try(BufferedReader in  = new BufferedReader(new InputStreamReader(Taxon.class.getResourceAsStream(NCBI_DIR + "nodes.dmp")))) {

				while ((line = in.readLine()) != null) {
					fields = line.split("\t\\|\t");
					generalInfoMap.put(fields[0], new TaxonInfo(fields[2], fields[1]));
				}
			}
			/**
			 *  Process the names, if asked
			 */

			if (withNames) {

				try(BufferedReader in = new BufferedReader(new InputStreamReader(Taxon.class.getResourceAsStream(NCBI_DIR + "names.dmp")))) {

					while ((line = in.readLine()) != null) {
						fields = line.split("\t\\|\t");

						if (fields[3].startsWith("scientific name") && generalInfoMap.get(fields[0]).getName() == null) {
							generalInfoMap.get(fields[0]).setName(fields[1]);
						}
					}
				}
			}

			log.info(generalInfoMap.size() + " elements loaded.");

		}
	}

	public static String mostPopular(String[] candidates) {
		loadPopularityTableFromTextFile();

		String courId = candidates[0];
		int courPop = 0;

		for (String id : candidates) {
			if (!popularityMap.containsKey(id)) {
				continue;
			} else if (popularityMap.get(id) > courPop) {
				courId = id;
				courPop = popularityMap.get(id);
			}
		}

		return courId;

	}

	public static void loadPopularityTableFromTextFile() {
		if (popularityMap == null) {
			popularityMap = new HashMap<>();

			log.info("Loading popularity table from text file...");
			String line;

			try(InputStream input = Taxon.class.getResourceAsStream(POPULARITY_FILE);
				BufferedReader in = new BufferedReader(new InputStreamReader(input));){

				String[] cols;
				while ((line = in.readLine()) != null) {
					cols = line.split("\t");

					if (cols.length == 3) {
						if (Integer.parseInt(cols[1]) > 0) {
							popularityMap.put(cols[0], Integer.parseInt(cols[1]));
						}
					}
				}

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				log.error(e1.getMessage());
			}

		}
	}

	/**
	 * Gets the current path of the EBI dump file
	 *
	 */
	public static String getEBIDumpFile() {
		return TAXONOMY_DB;
	}

	/**
	 * Enables to change the directory where the class will look for the EBI dump file
	 *
	 */
	public static void setEBIDumpFile(String taxonomy_db) {
		TAXONOMY_DB = taxonomy_db;
	}

	/**
	 * Gets the current path of the NCBI dump files
	 */
	public static String getNCBIDumpPath() {
		return NCBI_DIR;
	}

	/**
	 * Enables to change the directory where the class will look for the NCBI dump files
	 */
	public static void setNCBIDumpPath(String ncbi_dir) {
		NCBI_DIR = ncbi_dir;
	}

	public static String getPopularityFile() {
		return POPULARITY_FILE;
	}

	public static void setPopularityFile(String inPopularityFile) {
		POPULARITY_FILE = inPopularityFile;
	}

	/**
	 * static method to access the Hashtable without making a new object
	 *
	 */
	public static String getParentNodeIDOf(String id) {
		if (generalInfoMap.containsKey(id)) {
			return generalInfoMap.get(id).getParentID();
		} else {
			return null;
		}
	}

	/**
	 * Get the set of all the direct subnodes IDs of the given taxonomy ID
	 *
	 */
	public static HashSet<String> getChildren(String id) {
		makeChildrenTableIfNeeded();

		return childrenMap.get(id);
	}

	/**
	 * Generate the children table if it has not already be done.
	 */
	public static void makeChildrenTableIfNeeded() {
		// Make the children table if needed
		if (childrenMap == null) {
			childrenMap = new HashMap<>();

			log.info("Generating the taxon children table...");

			String taxonID;

			for (String current : getAllTaxIDs()) {
				taxonID = getParentNodeIDOf(current);

				if (!childrenMap.containsKey(taxonID)) {
					childrenMap.put(taxonID, new HashSet<>());
				}
				childrenMap.get(taxonID).add(current);
			}

			log.info("Done.");

		}
	}

	/**
	 * Gets the name, rank, parent of the taxon from its ID, using the pre-loaded HashMap.
	 */
	public void getInformationFromID() {
		TaxonInfo temp = generalInfoMap.get(taxonomyID);

		if (temp != null) {
			if (temp.getName() != null) {
				speciesName = temp.getName();
			}
			rank = temp.getRank();
			parentID = temp.getParentID();
		}
	}

	/**
	 * Indicates whether the taxon is a subnode of the given taxonomy ID or not
	 *
	 */
	public boolean isDirectSubnodeOf(String inTaxId) {
		boolean res = false;

		if (parentID != null && parentID.equals(inTaxId)) {
			res = true;
		}

		return res;
	}

	/**
	 * Indicates whether the taxon is the parent of the given taxonomy ID or not
	 *
	 */
	public boolean isDirectParentOf(String inTaxId) {
		boolean res = false;

		Taxon supposedSon = new Taxon(inTaxId);

		if (supposedSon.parentID != null && supposedSon.parentID.equals(taxonomyID)) {
			res = true;
		}


		return res;

	}

	/**
	 * Gets the Taxonomy ID of the taxon
	 */
	public String getTaxonomyID() {
		return taxonomyID;
	}

	/**
	 * Gets the rank of the taxon, as a string ("species", "genus"...)
	 */
	public String getRank() {
		return (rank == null) ? "none" : rank;
	}

	/**
	 * Gets the Taxonomy ID of the parent node of the taxon
	 */
	public String getParentID() {
		return parentID;
	}

	/**
	 * Gets the scientific name of the taxon, if the HashMap has been loaded with the correct option. @see Taxon#loadMapIfNeeded(boolean)
	 */
	public String getTaxonName() {
		return speciesName;
	}


}


