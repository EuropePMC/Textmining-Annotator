package uk.ac.ebi.literature.textminingapi.organisms;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.filter.OrgSubFilter;
import uk.ac.ebi.literature.textminingapi.utility.Taxon;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Represents a term tagged as a taxon. The method toString() gives a XML representation of the object.
 *
 * @author Romain Tertiaux
 */
@Component
public class TaggedElement {
	private static final Logger log = LoggerFactory.getLogger(TaggedElement.class.getName());
	static public String extraAmbiguousNames = "";
	protected String[] ids;
	protected String term;
	protected String previousWords;
	protected String nextWords;
	protected String className;
	String all;
	@Value("${extra_amb_list}")
	private String extra_amb_list;
	private boolean changed = false;


	protected TaggedElement() {

	}

	/**
	 * Constructor
	 *
	 * @param element The complete XML code of the tag, from the opening tag to the closing one.
	 */
	public TaggedElement(String element) {

		all = element;

		ids = element.replaceFirst(".*id=\"species:([^\"]*)\".*", "$1").split("\\|species:");
		term = element.replaceFirst(".*<e id=\"[^\"]*\"[^>]*>([^<]*)</e>.*", "$1");
		className = element.replaceFirst(".*classname=\"([^\"]*)\".*", "$1");

		previousWords = element.replaceFirst("([^ ]* [^ ]*).*", "$1");
		nextWords = element.replaceFirst(".*</e>([^ ]* [^ ]*)", "$1");

		if (extraAmbiguousNames == null) {
			this.loadExtraAmbiguousNames();
		}

	}

	/**
	 * Loads the list of extra-taxonomy ambiguous names, as "cancer", "spot"...
	 */
	public void loadExtraAmbiguousNames() {
		try(BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(extra_amb_list)));) {
			log.info("Loading the extra ambiguous names list...");

			String line;
			while ((line = in.readLine()) != null) {
				extraAmbiguousNames = extraAmbiguousNames + line + "|";
			}

			extraAmbiguousNames = extraAmbiguousNames.substring(0, extraAmbiguousNames.length() - 2);

		} catch (FileNotFoundException e) {
			log.error(extra_amb_list + " not found.");
			System.exit(-1);
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	/**
	 * Indicates if the given filter could help to solve the intra-taxonomy ambiguity of the element.
	 *
	 * @param filter
	 * @return
	 */
	public boolean isSolvableWith(OrgSubFilter filter) {
		if (filter.secondPass(this).length == 1) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Indicates if the term has been tagged as a scientific name (ex : "Escherichia coli").
	 *
	 * @return
	 */
	public boolean isScientific() {
		return (className.equals("scientific_name"));
	}

	/**
	 * Indicates if the term has been tagged as a scientific short name (ex : "E. coli").
	 *
	 * @return
	 */
	public boolean isShortName() {
		return (className.equals("scientific_short_name") || className.equals("generated_short_name"));
	}

	/**
	 * Indicates if the term has been tagged as intra-ambiguous, which means that there is an ambiguity for the term inside the taxonomy.
	 *
	 * @return
	 */
	public boolean isIntraAmbiguous() {
		return (ids.length > 1);
	}

	public boolean isFalsePositive() {
		return (ids.length == 0);
	}

	/**
	 * Indicates if the term is part of the extra ambiguous names list.
	 *
	 * @return
	 */
	public boolean isExtraAmbiguous() {
		return (term.matches(extraAmbiguousNames));
	}

	@Deprecated
	public boolean hasChanged() {
		return changed;
	}

	/**
	 * Gives a XML representation of the tag
	 */
	@Override
	public String toString() {
		/* Untagged term */
		if (ids.length == 0) {
			return term;
		} else {
			StringBuilder temp = new StringBuilder("<e id=\"");

			for (String a : ids) {
				temp.append("species:" + a + "|");
			}

			temp.deleteCharAt(temp.toString().length() - 1);

			temp.append("\" classname=\"" + className + "\" ");

			if (ids.length == 1) {
				temp.append("rank=\"" + new Taxon(ids[0]).getRank().replaceAll(" ", "_") + "\" ");
			}

			temp.append(">" + term + "</e>");

			return temp.toString();

		}
	}

	/**
	 * Gets the term which have been tagged ("E. coli", for example)
	 */
	public String getTerm() {
		return term;
	}

	/**
	 * Gets the type of the term ("scientific_name", "common_name"...)
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Gets the two words found before the term
	 */
	public String getPreviousWords() {
		return previousWords;
	}

	/**
	 * Gets the next words found before the term
	 */
	public String getNextWords() {
		return nextWords;
	}

	/**
	 * Gets the first ID stored in the element
	 */
	public String getID() {
		return ids[0];
	}

	/**
	 * Gets all the IDs stored in the element
	 */
	public String[] getIDs() {
		return ids;
	}

	/**
	 * Enables to change the IDs of the element
	 *
	 * @param inIDs
	 */
	public void setIDs(String[] inIDs) {
		if (!inIDs.equals(ids)) {
			changed = true;
			ids = inIDs;
		}
	}

	public String getStartDelimiter() {
		return "<e ";
	}

	public String getEndDelimiter() {
		return "</e>";
	}

}
