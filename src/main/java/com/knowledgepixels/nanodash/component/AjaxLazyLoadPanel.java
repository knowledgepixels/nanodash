package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

public abstract class AjaxLazyLoadPanel<T extends Component> extends org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel<T> {

    public AjaxLazyLoadPanel(String id) {
        super(id);
    }

    public AjaxLazyLoadPanel(final String id, final IModel<?> model) {
        super(id, model);
    }

}
