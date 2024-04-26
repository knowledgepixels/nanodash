package com.knowledgepixels.nanodash;

import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class ErrorPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/error";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public ErrorPage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this, null));
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
