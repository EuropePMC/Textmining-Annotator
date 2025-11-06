package uk.ac.ebi.literature.textminingapi.organisms;

import monq.jfa.AbstractFaAction;
import monq.jfa.DfaRun;

/**
 * This handler is responsible for the ArticleProcessor to be aware of where it is
 * in the article.
 * @author Romain Tertiaux
 *
 */
public class LocationUpdater extends AbstractFaAction {

	private static final long serialVersionUID = -7472455670235260905L;

	public void invoke(StringBuilder out, int start, DfaRun runner)
	{
		ArticleProcessor filter = (ArticleProcessor)runner.clientData;
		
		/*
		 *  The use of "startsWith" without the last ">" allows some arguments
		 *  in the tag.
		 */
		if (out.substring(start).startsWith("<article"))
			filter.inArticle = true;
		else if (out.substring(start).equals("</article>"))
			filter.inArticle = false;
		if (out.substring(start).startsWith("<front"))
			filter.inFront = true;
		else if (out.substring(start).equals("</front>"))
			filter.inFront = false;
		else if (out.substring(start).startsWith("<body"))
			filter.inBody = true;
		else if (out.substring(start).equals("</body>"))
			filter.inBody = false;
		else if (out.substring(start).startsWith("<back"))
			filter.inBack = true;
		else if (out.substring(start).equals("</back>"))
			filter.inBack = false;
		else if (out.substring(start).startsWith("<plain"))
		{
			filter.inPlain = true;
		}
		else if (out.substring(start).equals("</plain>"))
			filter.inPlain = false;
		
		else if (filter.inFront && out.substring(start).startsWith("<title-group"))
			filter.inTitleGroup = true;
		else if (filter.inFront && out.substring(start).equals("</title-group>"))
			filter.inTitleGroup = false;
		
		else if (filter.inFront && filter.inTitleGroup && out.substring(start).equals("<article-title>"))
			filter.inTitle = true;
		else if (filter.inFront && filter.inTitleGroup && out.substring(start).equals("</article-title>"))
			filter.inTitle = false;
		
		else if (filter.inFront && out.substring(start).startsWith("<abstract"))
			filter.inAbstract = true;
		else if (filter.inFront && out.substring(start).equals("</abstract>"))
			filter.inAbstract = false;
		
		else if (filter.inBody && out.substring(start).startsWith("<SENT "))
		{
			filter.currentSentence = Integer.parseInt(out.substring(start).replaceFirst("<SENT sid=\"([0-9]*)\"[^>]*>.*", "$1"));
			if (filter.inTitle) filter.currentSentence=-1;
		}	
			else if (!filter.inFront && !filter.inBody && out.substring(start).startsWith("<SENT "))
			filter.currentSentence=-10;

	}
}
