package com.knowledgepixels.nanodash.connector.base;

public enum ConnectorOption {

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
			ConnectorOption.BIODIV_PR_TEMPLATE_OPTIONS);


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
