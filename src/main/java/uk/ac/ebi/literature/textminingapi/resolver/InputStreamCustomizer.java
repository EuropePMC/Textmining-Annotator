package uk.ac.ebi.literature.textminingapi.resolver;

import java.io.InputStream;

@FunctionalInterface
public interface InputStreamCustomizer {

	InputStream cutomize(InputStream is);

}
