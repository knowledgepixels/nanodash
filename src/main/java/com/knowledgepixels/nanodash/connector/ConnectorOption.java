package com.knowledgepixels.nanodash.connector;

public enum ConnectorOption {

    // TODO Publish/retrieve all this info also via nanopubs at some point:

    SPECTAXON("Identification of a specimen with a taxon",
            null,
            "https://w3id.org/np/RAzUQLB8nr99kcm5Av4KXFYOfjPamrBFRAsO7WNu7Gg2c",
            "https://w3id.org/np/RAvHuSAQNPeo-kzp3krcSiXQyyTusDoDmcOv9yI4G30_4"),

    ORGORG("Association between organisms",
            "e.g. an observation that <em>a particular individual grass snake (Natrix natrix Linnaeus, 1758) ate a particular individual of a tree frog (Hyla arborea (Linnaeus, 1758)</em>",
            "http://purl.org/np/RAMUbJ6gvqXzVP5-7VzaSW6CAu15OwJR5FtnC1ENqkZDI",
            "http://purl.org/np/RAafGz8UV6Nz4m8wyCLV4eJ_bdNfXKpGie0FH6TQZFa84",
            "http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
            ConnectorOption.BIODIV_PR_TEMPLATE_OPTIONS),

    TAXONTAXON("Association between taxa",
            "e.g. <em>The wolf (Canis lupus Linnaeus, 1758) preys on white-tailed deer (Odocoileus virginianus (Zimmermann, 1780))</em>",
            "http://purl.org/np/RAh16oLqLJKo8I8R2CebR1n8Dwv95KL_H-azFfGt2FGW0",
            "http://purl.org/np/RALX2suiPKea3pm65RjS97EL6k9iY1Jew_mb30hO5Zjv0",
            "http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
            ConnectorOption.BIODIV_PR_TEMPLATE_OPTIONS),

    TAXONENV("Association between taxa and environments",
            "e.g. <em>The wolf (Canis lupus Linnaeus, 1758) occurs in forest habitats</em>",
            "http://purl.org/np/RAwy5ZloUugunk3gafYppW6MfZGQXD554XgHfCAFHH08k",
            "http://purl.org/np/RATlLG_xH-woxWfvHYC-7LyV5F0V6Sq4EbDWS86h6CrUI",
            "http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
            ConnectorOption.BIODIV_PR_TEMPLATE_OPTIONS),

    ORGENV("Association between organisms and environments",
            "e.g. <em>A particular badger (Meles meles (Linnaeus, 1758)) was observed to inhabit a city</em>",
            "http://purl.org/np/RA3eSKxteBFrsGXdQhllmJa71Od71rPd4wJ8ik7fIMWsc",
            "http://purl.org/np/RAWxvypUU6OabNGlBz9jDGLnftqRXdokzI2oemf9vQRGY",
            "http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
            ConnectorOption.BIODIV_PR_TEMPLATE_OPTIONS),

    TAXONNAMES("Association between taxon names",
            "e.g. <em>Ursus meles Linnaeus, 1758 is a synonym of Meles meles (Linnaeus, 1758)</em>",
            "http://purl.org/np/RAf9CyiP5zzCWN-J0Ts5k7IrZY52CagaIwM-zRSBmhrC8",
            "http://purl.org/np/RAIz2ACUDvk3OAcXc-OjYSuLglUZu-fsJXrC4UtoAF7k4",
            "http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
            ConnectorOption.BIODIV_PR_TEMPLATE_OPTIONS),

    ORGNS("Association between organisms and nucleotide sequences",
            "e.g. <em>The nucleotide sequence MT149719 was found in an organism of the species Doryrhina camerunensis (Eisentraut, 1956)</em>",
            "http://purl.org/np/RACPgaoRptG1W-IZpNk6r-MBSMgcdZlaMrAtli9GkmKaE",
            "http://purl.org/np/RAyOMh3jJ2PWrgis-My3-QbN9yoBXVBulweANHdvJPhak",
            "http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
            ConnectorOption.BIODIV_PR_TEMPLATE_OPTIONS),

    TAXONNS("Association between taxa and nucleotide sequences",
            "e.g. <em>The nucleotide sequence GU682758 can be used to identify the species Araneus diadematus Clerck, 1757</em>",
            "http://purl.org/np/RA1ooazhkXacK_3jibfpfdwqJWNO0yLoN1nrlpuHHi_uM",
            "http://purl.org/np/RA5lyV7V98AAm6BM_s8gv17eRRFZsJqvp2TqCGV6QD3gk",
            "http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
            ConnectorOption.BIODIV_PR_TEMPLATE_OPTIONS),

    SPECPUB("Declaring a specimen being discussed in a publication",
            null,
            "https://w3id.org/np/RA_rnly1RAPPSMqgy0qTlEhDCRqt6Fx3NOQECCUBt5mQQ",
            "https://w3id.org/np/RAA7G5E1wwYCaQsu092uJOYqiBaSRM__6ltpJ6nzwR5vE"),

    BIOLINKREL("Expressing a biological relation between two entities",
            null,
            "http://purl.org/np/RAjvwGtEUz07hIGUZ_kHpW8R1TSeUhbWH8NdpC0MIvv_A",
            "http://purl.org/np/RAPeBcr6fuTot6fU5bKyspkFD_5RX9tLXcCIs1UkEEUUo",
            "http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
            ConnectorOption.BIODIV_PR_TEMPLATE_OPTIONS),

    EQREL("Mapping two equivalent or related resource identifiers",
            null,
            "http://purl.org/np/RAiBrF-il77MccafOpqR5pZgdlBFOreh9TAeKBga-Gxsk",
            "http://purl.org/np/RAEdHUFvCt1jOsW14rU12X2n42iQS0IshN-j4syY2-IuI"),

    TAXONDEF("Taxon Definition",
            null,
            "https://w3id.org/np/RAa7sIgQ77jJL4HYYGVH8sNu1YEKCH3F-CHqHrgbWWGS4",
            "https://w3id.org/np/RALQ-2tdTMpPks7m9fRMc5Lmia32Zn3_oPCmfdnB1CRAg"),

    CLASSDEF("Class Definition",
            "e.g. <em>operant research</em> as a subclass of <em>research</em>",
            "http://purl.org/np/RAcWWF8kSXfwZ77XdC59IyH1MJ24wFp-dDeXkPjKHAXM0",
            "http://purl.org/np/RAj26TjulpgBHXJGe0OwZZZ-cZCJ9WE7ICug5EW2tGH7s"),

    INDDEF("Definition of Individual",
            "e.g. <em>Pluto</em> as an instance of the class <em>dwarf planet</em>",
            "http://purl.org/np/RAL9L_HSXsRpyC9KcxUGejL3qDiWF6Jeoihh09NYdCR7c",
            "http://purl.org/np/RAJRFjNqKKBlxOhij8XnuqOYfjyjruF2jGZgLO2myu9O0"),

    SUPERPATTERN("Fully-formal Advanced Statements (Super-Pattern)",
            "e.g. <em>whenever a person has a headache then this is mostly caused by a dehydration of that person</em>",
            "http://purl.org/np/RAy3tITXPlULFLXxAxek39GLqLdqKkNM5hIzUOZxxjMmI",
            "http://purl.org/np/RAu49Eu8w-jwQw6hZ2ZgYOYqmF9HpqxZgItA4oy4N8M-4"),

    LINKFLOWSREL("Simple Scientific Relations between Individuals",
            "e.g. <em>the invention of the telephone was necessary for the Internet</em>",
            "http://purl.org/np/RAsz-9JwiOPQufQ5AGSNepkPG0hkWWYutBDWtsMRgKaaU",
            "http://purl.org/np/RAQjB6Dc9lrIxjnFNCXYa6rfDHo5e1WiPSu33WdCEWTNY"),

    CREL("Simple Scientific Relations based on Classes",
            "e.g. <em>instances of smoking tend to cause instances of lung cancer</em>",
            "http://purl.org/np/RAQ9wpGlnll9o034hWI9tGZw6oTBvcN9azFS7hzdESY0I",
            "http://purl.org/np/RAkX1V_9VIscbvJ6Nz4BKUdgywO0UWgBQlzJUEvdMYG-M"),

    AIDA("Semi-formal Statements Based on English Sentences (AIDA)",
            "e.g. <em>\"Teenagers reply on average faster to emails than adults.\" is about: teenager, email, adult</em>",
            "http://purl.org/np/RAdc8fxS-WgxHFUtTFWOKYJc1ICkDwBH11_f1sBZeQwBY",
            "http://purl.org/np/RAa5RbYolIrUNlBoAUY5HUmGr-ci6G1pX6lWiNMkZMcYs"),

    ML("Evaluation results of a Machine Learning experiment",
            "e.g. <em>we ran a Random Forest classifier to detect texts about animals and achieved an F-Score of 86%</em>",
            "http://purl.org/np/RAGEv4a9uT48mgnIlWqAvL-XnupfNYojIUmszJDa50xoc",
            "http://purl.org/np/RAQV0grGVX22NUdKxBijorX0vy06-hdDY3BpScIeQzhx0"),

    BIOREL("Simple Biological Relation",
            "e.g. <em>we ran a Random Forest classifier to detect texts about animals and achieved an F-Score of 86%</em>",
            "http://purl.org/np/RAEbbFIMF_kwp2rY1NqhhKHBiIXXL4_UgTC1hyd6l-cJs",
            "http://purl.org/np/RASe3c2pjmN9TvAoxD_CZ-qBVhVMUk9dSMRHOOVxdXbqM");


    private static final String BIODIV_PR_TEMPLATE_OPTIONS =
            "http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8 " +
            "http://purl.org/np/RAdWRjsWVzTY3c65A5cnMZZxEeGjLzlyrSpyoALTCj8z0 " +
            "http://purl.org/np/RA5bvGiV9W8MYiHxUtkki0U2Zi8j1FBQ8d5ElwJXJoYrY ";
    //"http://purl.org/np/RA-4AE_X28pW3dkmCNNP06QSnsxqMiduN7gX3HxSciX5k"

    private String name;
    private String explanation;
    private String templateId;
    private String exampleId;
    private String prTemplateId;
    private String prTemplateOptions = null;

    private ConnectorOption(String name, String explanation, String templateId, String exampleId, String prTemplateId, String prTemplateOptions) {
        this.name = name;
        this.explanation = explanation;
        this.templateId = templateId;
        this.exampleId = exampleId;
        this.prTemplateId = prTemplateId;
        this.prTemplateOptions = prTemplateOptions;
    }

    private ConnectorOption(String name, String explanation, String templateId, String exampleId) {
        this(name, explanation, templateId, exampleId, "http://purl.org/np/RA4LGtuOqTIMqVAkjnfBXk1YDcAPNadP5CGiaJiBkdHCQ", null);
    }

    public String getName() {
        return name;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getExampleId() {
        return exampleId;
    }

    public String getPrTemplateId() {
        return prTemplateId;
    }

    public String getPrTemplateOptions() {
        return prTemplateOptions;
    }

}
