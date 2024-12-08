package com.knowledgepixels.nanodash.connector.base;

import java.io.Serializable;
import java.util.HashMap;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;


public class ConnectorNanopubType implements Serializable {

	private static final long serialVersionUID = 1L;

	private static HashMap<String,ConnectorNanopubType> map = new HashMap<>();

	public static ConnectorNanopubType get(String id) {
		return map.get(id);
	}

	static {
		String biodivPrTemplateOptions = "http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8 " +
				"http://purl.org/np/RAdWRjsWVzTY3c65A5cnMZZxEeGjLzlyrSpyoALTCj8z0 " +
				"http://purl.org/np/RA5bvGiV9W8MYiHxUtkki0U2Zi8j1FBQ8d5ElwJXJoYrY ";
				//"http://purl.org/np/RA-4AE_X28pW3dkmCNNP06QSnsxqMiduN7gX3HxSciX5k"

		new ConnectorNanopubType("superpattern",
			"Fully-formal Advanced Statements (Super-Pattern)",
			"http://purl.org/np/RAy3tITXPlULFLXxAxek39GLqLdqKkNM5hIzUOZxxjMmI",
			"http://purl.org/np/RAu49Eu8w-jwQw6hZ2ZgYOYqmF9HpqxZgItA4oy4N8M-4");
		new ConnectorNanopubType("linkflowsrel",
			"Simple Scientific Relations between Individuals",
			"http://purl.org/np/RAsz-9JwiOPQufQ5AGSNepkPG0hkWWYutBDWtsMRgKaaU",
			"http://purl.org/np/RAQjB6Dc9lrIxjnFNCXYa6rfDHo5e1WiPSu33WdCEWTNY");
		new ConnectorNanopubType("crel",
			"Simple Scientific Relations based on Classes",
			"http://purl.org/np/RAQ9wpGlnll9o034hWI9tGZw6oTBvcN9azFS7hzdESY0I",
			"http://purl.org/np/RAkX1V_9VIscbvJ6Nz4BKUdgywO0UWgBQlzJUEvdMYG-M");
		new ConnectorNanopubType("aida",
			"Semi-formal Statements Based on English Sentences (AIDA)",
			"http://purl.org/np/RAdc8fxS-WgxHFUtTFWOKYJc1ICkDwBH11_f1sBZeQwBY",
			"http://purl.org/np/RAa5RbYolIrUNlBoAUY5HUmGr-ci6G1pX6lWiNMkZMcYs");
		new ConnectorNanopubType("classdef",
			"Class Definitions",
			"http://purl.org/np/RAcWWF8kSXfwZ77XdC59IyH1MJ24wFp-dDeXkPjKHAXM0",
			"http://purl.org/np/RAj26TjulpgBHXJGe0OwZZZ-cZCJ9WE7ICug5EW2tGH7s");
		new ConnectorNanopubType("inddef",
			"Definitions of Individuals",
			"http://purl.org/np/RAL9L_HSXsRpyC9KcxUGejL3qDiWF6Jeoihh09NYdCR7c",
			"http://purl.org/np/RAJRFjNqKKBlxOhij8XnuqOYfjyjruF2jGZgLO2myu9O0");
		new ConnectorNanopubType("ml",
			"Evaluation results of a Machine Learning experiment",
			"http://purl.org/np/RAGEv4a9uT48mgnIlWqAvL-XnupfNYojIUmszJDa50xoc",
			"http://purl.org/np/RAQV0grGVX22NUdKxBijorX0vy06-hdDY3BpScIeQzhx0");
		new ConnectorNanopubType("biorel",
			"Simple Biological Relation",
			"http://purl.org/np/RAEbbFIMF_kwp2rY1NqhhKHBiIXXL4_UgTC1hyd6l-cJs",
			"http://purl.org/np/RASe3c2pjmN9TvAoxD_CZ-qBVhVMUk9dSMRHOOVxdXbqM");
		// covered in ConnectorOption:
		new ConnectorNanopubType("taxontaxon",
			"Association between taxa",
			"http://purl.org/np/RAh16oLqLJKo8I8R2CebR1n8Dwv95KL_H-azFfGt2FGW0",
			"http://purl.org/np/RALX2suiPKea3pm65RjS97EL6k9iY1Jew_mb30hO5Zjv0",
			"http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
			biodivPrTemplateOptions);
		new ConnectorNanopubType("taxonenv",
			"Association between taxa and environments",
			"http://purl.org/np/RAwy5ZloUugunk3gafYppW6MfZGQXD554XgHfCAFHH08k",
			"http://purl.org/np/RATlLG_xH-woxWfvHYC-7LyV5F0V6Sq4EbDWS86h6CrUI",
			"http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
			biodivPrTemplateOptions);
		new ConnectorNanopubType("taxongene",
			"Association between taxa and genes",
			"http://purl.org/np/RAQcn-NHXcqWvEsguTCnFspUqCBBbzQis2KafZf3IDMpk",
			"http://purl.org/np/RArCW84nAUo14Z3GSoIppZKxoIpdJ0V12_ytDMsDTsVmk",
			"http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
			biodivPrTemplateOptions);
		// covered in ConnectorOption:
		new ConnectorNanopubType("orgorg",
			"Association between organisms",
			"http://purl.org/np/RAMUbJ6gvqXzVP5-7VzaSW6CAu15OwJR5FtnC1ENqkZDI",
			"http://purl.org/np/RAafGz8UV6Nz4m8wyCLV4eJ_bdNfXKpGie0FH6TQZFa84",
			"http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
			biodivPrTemplateOptions);
		new ConnectorNanopubType("orgenv",
			"Association between organisms and environments",
			"http://purl.org/np/RA3eSKxteBFrsGXdQhllmJa71Od71rPd4wJ8ik7fIMWsc",
			"http://purl.org/np/RAWxvypUU6OabNGlBz9jDGLnftqRXdokzI2oemf9vQRGY",
			"http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8");
		new ConnectorNanopubType("taxonnames",
			"Association between taxon names (nomenclature)",
			"http://purl.org/np/RAf9CyiP5zzCWN-J0Ts5k7IrZY52CagaIwM-zRSBmhrC8",
			"http://purl.org/np/RAIz2ACUDvk3OAcXc-OjYSuLglUZu-fsJXrC4UtoAF7k4",
			"http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
			biodivPrTemplateOptions);
		new ConnectorNanopubType("orgns",
			"Association between organisms and nucleotide sequences",
			"http://purl.org/np/RACPgaoRptG1W-IZpNk6r-MBSMgcdZlaMrAtli9GkmKaE",
			"http://purl.org/np/RAyOMh3jJ2PWrgis-My3-QbN9yoBXVBulweANHdvJPhak",
			"http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
			biodivPrTemplateOptions);
		new ConnectorNanopubType("taxonns",
			"Association between taxa and nucleotide sequences",
			"http://purl.org/np/RA1ooazhkXacK_3jibfpfdwqJWNO0yLoN1nrlpuHHi_uM",
			"http://purl.org/np/RA5lyV7V98AAm6BM_s8gv17eRRFZsJqvp2TqCGV6QD3gk",
			"http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8");
		new ConnectorNanopubType("biolinkrel",
			"Expressing a biological relation between two entities",
			"http://purl.org/np/RAjvwGtEUz07hIGUZ_kHpW8R1TSeUhbWH8NdpC0MIvv_A",
			"http://purl.org/np/RAPeBcr6fuTot6fU5bKyspkFD_5RX9tLXcCIs1UkEEUUo",
			"http://purl.org/np/RAnt6U2Z3hEI2XWgoiAO7gY4WPMYeGhqdxAE-kwbZJhW8",
			biodivPrTemplateOptions);
		new ConnectorNanopubType("eqrel",
			"Mapping two equivalent or related resource identifiers",
			"http://purl.org/np/RAiBrF-il77MccafOpqR5pZgdlBFOreh9TAeKBga-Gxsk",
			"http://purl.org/np/RAEdHUFvCt1jOsW14rU12X2n42iQS0IshN-j4syY2-IuI");
		// covered in ConnectorOption:
		new ConnectorNanopubType("spectaxon",
			"Identifying a specimn with a taxon name",
			"https://w3id.org/np/RAzUQLB8nr99kcm5Av4KXFYOfjPamrBFRAsO7WNu7Gg2c",
			"https://w3id.org/np/RAvHuSAQNPeo-kzp3krcSiXQyyTusDoDmcOv9yI4G30_4");
		new ConnectorNanopubType("specpub",
				"Identifying a specimn with a taxon name",
				"https://w3id.org/np/RA_rnly1RAPPSMqgy0qTlEhDCRqt6Fx3NOQECCUBt5mQQ",
				"https://w3id.org/np/RAA7G5E1wwYCaQsu092uJOYqiBaSRM__6ltpJ6nzwR5vE");
		new ConnectorNanopubType("taxondef",
				"Defining a new biological taxon",
				"https://w3id.org/np/RAa7sIgQ77jJL4HYYGVH8sNu1YEKCH3F-CHqHrgbWWGS4",
				"https://w3id.org/np/RALQ-2tdTMpPks7m9fRMc5Lmia32Zn3_oPCmfdnB1CRAg");
		new ConnectorNanopubType("reaction",
			// This is still experimental and not yet used
			"A reaction or comment on a paper or nanopublication",
			"http://purl.org/np/RANWGVogb5j_VQ6A4nabA34_-zkZTRYNYtItRJXGf2TVQ",
			"http://purl.org/np/RAxjCU5pZDoGox98Hb36mquM9Bc1xv0qz4P19p2avnNPI",
			"http://purl.org/np/RANwQa4ICWS5SOjw7gp99nBpXBasapwtZF1fIM3H2gYTM");
	}

	private String id;
	private String title = null;
	private Template template = null;
	private String exampleId = null;
	private String prTemplateId = "http://purl.org/np/RA4LGtuOqTIMqVAkjnfBXk1YDcAPNadP5CGiaJiBkdHCQ";
	private String prTemplateOptions = null;

	private ConnectorNanopubType(String id, String title, String templateId, String exampleId) {
		this.id = id;
		this.title = title;
		this.template = TemplateData.get().getTemplate(templateId);
		this.exampleId = exampleId;
		map.put(id, this);
	}

	private ConnectorNanopubType(String id, String title, String templateId, String exampleId, String prTemplateId) {
		this(id, title, templateId, exampleId);
		this.prTemplateId = prTemplateId;
	}

	private ConnectorNanopubType(String id, String title, String templateId, String exampleId, String prTemplateId, String prTemplateOptions) {
		this(id, title, templateId, exampleId, prTemplateId);
		this.prTemplateOptions = prTemplateOptions;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public Template getTemplate() {
		return template;
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
