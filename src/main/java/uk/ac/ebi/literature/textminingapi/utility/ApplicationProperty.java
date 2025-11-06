package uk.ac.ebi.literature.textminingapi.utility;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationProperty {
    public static final String UTF_8 = "UTF-8";
    @Value("${ebi_host_short}")
    public String EBI_HOST_SHORT;
    @Value("${mint_host}")
    public String MINT_HOST;
    @Value("${cath_host_domain_family}")
    public String CATH_HOST_DOMAIN_FAMILY;
    @Value("${cath_host_super_family}")
    public String CATH_HOST_SUPER_FAMILY;
    @Value("${host}")
    public String HOST;
    @Value("${uniprot_blacklist_txt}")
    public String UNIPROT_BLACKLIST_TXT;
    @Value("${z_uniprot}")
    public String Z_UNIPROT;
    @Value("${chemical_blacklist_only_lowercase_150615}")
    public String CHEMICAL_BLACKLIST_ONLY_LOWERCASE_150615;
    @Value("${z_chebi}")
    public String Z_CHEBI;
    @Value("${plain}")
    public String PLAIN;
    @Value("${gene_blacklist_160504}")
    public String GENE_BLACKLIST_160504;
    @Value("${aa_blacklist}")
    public String AA_BLACKLIST;
    @Value("${taxonomy_ebi_file}")
    public String taxonomy_ebi_file;
    @Value("${taxonomy_ncbi_dir}")
    public String taxonomy_ncbi_dir;
    @Value("${pmc_popularity}")
    public String pmc_popularity;
    @Value("${doi_host}")
    public String DOI_HOST;   // example: https://api.datacite.org/works/10.5061/dryad.pk045
    @Value("${doiblacklist}")
    public String doiPrefixFilename;
    public static Integer CONN_TIMEOUT=150000;
}
