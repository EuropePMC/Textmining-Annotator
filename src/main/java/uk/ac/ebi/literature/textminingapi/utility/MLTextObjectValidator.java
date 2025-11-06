package uk.ac.ebi.literature.textminingapi.utility;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.ebi.literature.textminingapi.pojo.MLTextObject;

public interface MLTextObjectValidator {
	
	boolean validate(MLTextObject obj, AtomicReference<List<String>> errorMessages);
}
