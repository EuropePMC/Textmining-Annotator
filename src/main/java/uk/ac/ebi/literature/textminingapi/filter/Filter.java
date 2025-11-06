package uk.ac.ebi.literature.textminingapi.filter;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class Filter {
    public InputStream in = null;
    public OutputStream out = null;

    public void setIn(InputStream in) {
        this.in = in;
    }

    public void setOut(OutputStream out) {
        this.out = out;
    }

    public abstract void run() throws Exception;
}
