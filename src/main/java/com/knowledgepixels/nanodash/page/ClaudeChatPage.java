package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.chat.ClaudeChatService;
import com.knowledgepixels.nanodash.component.ClaudeChatPanel;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Full-page view of the Claude Code chat; the same panel is available docked
 * on every page. Only functional when the feature is enabled; see
 * docs/claude-code-chat.md.
 */
public class ClaudeChatPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/claudechat";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * The full-page chat has no use for the docked panel on top of itself.
     */
    @Override
    protected boolean hasClaudeChatDock() {
        return false;
    }

    /**
     * Constructor for ClaudeChatPage.
     *
     * @param parameters Page parameters
     */
    public ClaudeChatPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, null));

        boolean enabled = ClaudeChatService.get().isEnabled();
        add(new Label("disabledNote",
                "This feature is switched off. It is meant for locally running instances: "
                        + "enable it with claudeChatEnabled: true in the preferences file (or NANODASH_CLAUDE_CHAT_ENABLED=true) "
                        + "on an instance where the Claude Code CLI is installed and logged in.")
                .setVisible(!enabled));

        add(new ClaudeChatPanel("chatpanel", false).setVisible(enabled));
    }

}
