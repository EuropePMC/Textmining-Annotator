package uk.ac.ebi.literature.textminingapi.organisms;

import uk.ac.ebi.literature.textminingapi.utility.Taxon;

/**
 * Represents a term tagged as a taxon. The method toString() gives a XML representation of the object.
 * @author  Romain Tertiaux
 */
public class WhatIzItTaggedElement extends TaggedElement
{

	/**
	 * Constructor
	 * @param element The complete XML code of the tag, from the opening tag to the closing one.
	 */
	//
	public WhatIzItTaggedElement(String element)
	{
		all=element;

		ids = element.replaceFirst(".*ids=\"([^\"]*)\".*", "$1").split(",");
		term = element.replaceFirst(".*<z:species[^>]*>([^<]*)</z:species>.*", "$1");
		className = element.replaceFirst(".*classname=\"([^\"]*)\".*", "$1");

		previousWords = element.replaceFirst("([^ ]* [^ ]*).*", "$1");
		nextWords = element.replaceFirst(".*</z:species>([^ ]* [^ ]*)", "$1");
		if (extraAmbiguousNames==null) {
			loadExtraAmbiguousNames();
		}

	}

	/**
	 * Gives a XML representation of the tag 
	 */
	@Override
	public String toString()
	{
		/* Untagged term */
		if (ids.length==0)
		{
			return term;
		}
		else
		{
			StringBuilder temp = new StringBuilder("<z:species ids=\"");

			for (String a : ids)
			{
				temp.append(a+",");
			}

			temp.deleteCharAt(temp.toString().length()-1);
	
			temp.append("\" classname=\"" + className + "\"");
						
			if (ids.length==1) {
				temp.append(" rank=\""+new Taxon(ids[0]).getRank().replaceAll(" ", "_")+"\"");
			}
			
			temp.append(">" + term +"</z:species>");

			return temp.toString();

		}
	}

	@Override
	public String getStartDelimiter() { return "<z:species";}
	
	@Override
	public String getEndDelimiter() { return "</z:species>";}

}
