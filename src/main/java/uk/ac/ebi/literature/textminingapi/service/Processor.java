package uk.ac.ebi.literature.textminingapi.service;

@FunctionalInterface
public interface Processor<I, O> {

	O process(I input) throws Exception;

}
