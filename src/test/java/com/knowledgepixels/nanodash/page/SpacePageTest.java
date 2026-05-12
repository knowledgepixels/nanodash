package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import org.apache.wicket.model.IModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class SpacePageTest {

    // Guards #456: instance state must not embed live singleton references, otherwise
    // the page-store snapshot drifts from the repository between renders.
    @Test
    void doesNotHoldDirectSpaceOrMaintainedResourceInstanceField() {
        for (Field f : SpacePage.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            assertNotEquals(Space.class, f.getType(),
                    "SpacePage must not hold a direct Space field: " + f.getName());
            assertNotEquals(MaintainedResource.class, f.getType(),
                    "SpacePage must not hold a direct MaintainedResource field: " + f.getName());
        }
    }

    @Test
    void holdsSpaceIdAndSpaceModelFields() throws NoSuchFieldException {
        Field idField = SpacePage.class.getDeclaredField("spaceId");
        assertEquals(String.class, idField.getType());

        Field modelField = SpacePage.class.getDeclaredField("spaceModel");
        assertEquals(IModel.class, modelField.getType());
    }

}
