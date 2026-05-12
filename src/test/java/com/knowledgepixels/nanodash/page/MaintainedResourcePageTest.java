package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import org.apache.wicket.model.IModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class MaintainedResourcePageTest {

    // Guards #456: instance state must not embed live singleton references, otherwise
    // the page-store snapshot drifts from the repository between renders.
    @Test
    void doesNotHoldDirectMaintainedResourceOrSpaceInstanceField() {
        for (Field f : MaintainedResourcePage.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            assertNotEquals(MaintainedResource.class, f.getType(),
                    "MaintainedResourcePage must not hold a direct MaintainedResource field: " + f.getName());
            assertNotEquals(Space.class, f.getType(),
                    "MaintainedResourcePage must not hold a direct Space field: " + f.getName());
        }
    }

    @Test
    void holdsResourceIdAndResourceModelFields() throws NoSuchFieldException {
        Field idField = MaintainedResourcePage.class.getDeclaredField("resourceId");
        assertEquals(String.class, idField.getType());

        Field modelField = MaintainedResourcePage.class.getDeclaredField("resourceModel");
        assertEquals(IModel.class, modelField.getType());
    }

}
