package uk.ac.ebi.literature.textminingapi.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import uk.ac.ebi.literature.textminingapi.pojo.MLTextObject;

@Component
public class MLTextObjectValidatorImpl implements MLTextObjectValidator {

	@Override
	public boolean validate(MLTextObject obj, AtomicReference<List<String>> errorMessage) {
		
		boolean valid = true;
		
		List<String> errors = new ArrayList<String>();
		
        if (obj==null) {
        	errors.add("Message can not be empty");
        	valid = false;
        }
        
        if ((obj!= null) && (StringUtils.isEmpty(obj.getFtId()) || StringUtils.isEmpty(obj.getUser()))) {
        	errors.add("ftId and user of message can not be empty");
        	valid = false;
        }
        
        if ((obj!= null) && (StringUtils.isEmpty(obj.getFilename()) || StringUtils.isEmpty(obj.getUrl()))) {
        	errors.add("File name and url of message can not be empty");
        	valid = false;
        }
        
        if ((obj!= null) && StringUtils.isEmpty(obj.getStatus())) {
        	errors.add("status of message can not be empty");
        	valid = false;
        }
		
        errorMessage.set(errors);
		return valid;
	}

}
