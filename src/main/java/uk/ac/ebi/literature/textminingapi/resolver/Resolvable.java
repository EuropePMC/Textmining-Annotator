package uk.ac.ebi.literature.textminingapi.resolver;

import org.springframework.stereotype.Component;

@Component
public interface Resolvable {
	public boolean isValid(String sem_type, String id ) throws Exception;
}
