package com.knowledgepixels.nanodash.page;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DownloadDocPageTest {

    @Nested
    @DisplayName("CONTENT_TYPE_MAP and EXTENSION_MAP")
    class FormatMapsTest {

        @Test
        @DisplayName("CONTENT_TYPE_MAP should contain all supported formats")
        void contentTypeMapComplete() throws Exception {
            var field = DownloadDocPage.class.getDeclaredField("CONTENT_TYPE_MAP");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) field.get(null);

            assertEquals("text/html; charset=utf-8", map.get("html"));
            assertEquals("application/rtf", map.get("rtf"));
            assertEquals("application/pdf", map.get("pdf"));
            assertEquals(3, map.size());
        }

        @Test
        @DisplayName("EXTENSION_MAP should have matching entries for all formats")
        void extensionMapComplete() throws Exception {
            var field = DownloadDocPage.class.getDeclaredField("EXTENSION_MAP");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) field.get(null);

            assertEquals(".html", map.get("html"));
            assertEquals(".rtf", map.get("rtf"));
            assertEquals(".pdf", map.get("pdf"));
            assertEquals(3, map.size());
        }
    }

    @Nested
    @DisplayName("MOUNT_PATH constant")
    class MountPathTest {

        @Test
        @DisplayName("MOUNT_PATH should be /download-doc")
        void mountPath() {
            assertEquals("/download-doc", DownloadDocPage.MOUNT_PATH);
        }
    }

}
