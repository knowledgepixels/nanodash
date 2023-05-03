package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;


public class ErrorPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public ErrorPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
	}

    @Override
    protected void configureResponse(WebResponse response) {
        super.configureResponse(response);
    }
 
    @Override
    public boolean isVersioned() {
        return false;
    }
 
    @Override
    public boolean isErrorPage() {
        return true;
    }

}
