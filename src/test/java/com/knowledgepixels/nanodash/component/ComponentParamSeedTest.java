package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.TemplateContext;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression test for issue #271: when a new repetition group is added, the
 * shared {@code IModel} of a global-scope placeholder must keep the user's
 * edit instead of being re-seeded from the URL param by the new component's
 * constructor.
 */
class ComponentParamSeedTest {

    private static final String TEMPLATE_URI = "https://w3id.org/np/RAcws59tX-7nxdPpgl6FhRNq5KLIwJBJSZsHilqmOyT8Q";
    private static final ValueFactory vf = SimpleValueFactory.getInstance();
    private static final String POSTFIX = "myPlaceholder";

    private TemplateContext context;
    private IRI iri;

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
        context = new TemplateContext(ContextType.ASSERTION, TEMPLATE_URI, "statement", (String) null);
        iri = vf.createIRI(TEMPLATE_URI + "/" + POSTFIX);
    }

    private <T extends Serializable> IModel<T> seedSharedModel(T userValue) {
        IModel<T> model = Model.of(userValue);
        context.getComponentModels().put(iri, model);
        return model;
    }

    @Test
    void iriTextfieldItemPreservesEditedSharedModel() {
        IModel<String> shared = seedSharedModel("USER_EDITED");
        context.setParam(POSTFIX, "PARAM_DEFAULT");
        new IriTextfieldItem("id", "subj", iri, false, context);
        assertEquals("USER_EDITED", shared.getObject());
    }

    @Test
    void iriTextfieldItemSeedsNewModelFromParam() {
        context.setParam(POSTFIX, "PARAM_DEFAULT");
        new IriTextfieldItem("id", "subj", iri, false, context);
        assertEquals("PARAM_DEFAULT", context.getComponentModels().get(iri).getObject());
    }

    @Test
    void restrictedChoiceItemPreservesEditedSharedModel() {
        IModel<String> shared = seedSharedModel("USER_EDITED");
        context.setParam(POSTFIX, "PARAM_DEFAULT");
        new RestrictedChoiceItem("id", "subj", iri, false, context);
        assertEquals("USER_EDITED", shared.getObject());
    }

    @Test
    void valueTextfieldItemPreservesEditedSharedModel() {
        IModel<String> shared = seedSharedModel("USER_EDITED");
        context.setParam(POSTFIX, "PARAM_DEFAULT");
        new ValueTextfieldItem("id", "subj", iri, false, context);
        assertEquals("USER_EDITED", shared.getObject());
    }

    @Test
    void guidedChoiceItemPreservesEditedSharedModel() {
        IModel<String> shared = seedSharedModel("USER_EDITED");
        context.setParam(POSTFIX, "PARAM_DEFAULT");
        new GuidedChoiceItem("id", "subj", iri, false, context);
        assertEquals("USER_EDITED", shared.getObject());
    }

    @Test
    void agentChoiceItemPreservesEditedSharedModel() {
        IModel<String> shared = seedSharedModel("USER_EDITED");
        context.setParam(POSTFIX, "PARAM_DEFAULT");
        new AgentChoiceItem("id", "subj", iri, false, context);
        assertEquals("USER_EDITED", shared.getObject());
    }

    @Test
    void literalTextfieldItemPreservesEditedSharedModel() {
        IModel<String> shared = seedSharedModel("USER_EDITED");
        context.setParam(POSTFIX, "PARAM_DEFAULT");
        new LiteralTextfieldItem("id", iri, false, context);
        assertEquals("USER_EDITED", shared.getObject());
    }

    @Test
    void literalDateItemPreservesEditedSharedModel() {
        Date userDate = new Date(0);
        IModel<Date> shared = seedSharedModel(userDate);
        context.setParam(POSTFIX, "2024-01-01");
        new LiteralDateItem("id", iri, false, context);
        assertEquals(userDate, shared.getObject());
    }

    @Test
    void literalDateTimeItemPreservesEditedSharedModel() {
        ZonedDateTime userDateTime = ZonedDateTime.parse("2020-01-01T00:00:00Z");
        IModel<ZonedDateTime> shared = seedSharedModel(userDateTime);
        context.setParam(POSTFIX, "2024-01-01T00:00:00Z");
        new LiteralDateTimeItem("id", iri, false, context);
        assertEquals(userDateTime, shared.getObject());
    }

}
