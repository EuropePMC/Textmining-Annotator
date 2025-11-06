package uk.ac.ebi.literature.textminingapi.utility;


/**
 * This class is only used to store the taxon information in the hashtable of the Taxon class
 * @author  Romain Tertiaux
 */
public class TaxonInfo {

	private final int parentID;
	private final String rank;
	private String name=null;
	
	public TaxonInfo(String inRank, String inID)
	{
		parentID=Integer.parseInt(inID);
		rank=inRank;
	}

	public String getParentID() {
		return parentID+"";
	}

	public String getRank() {
		return rank;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String inName) {
		name=inName;
	}
	
	
}