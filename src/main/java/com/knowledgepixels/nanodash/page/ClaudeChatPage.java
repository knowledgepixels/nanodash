package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.chat.ChatMessage;
import com.knowledgepixels.nanodash.chat.ClaudeChatService;
import com.knowledgepixels.nanodash.chat.ClaudeSession;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Chat page backed by the user's local Claude Code instance.
 * Only functional when the feature is enabled; see docs/claude-code-chat.md.
 */
public class ClaudeChatPage extends NanodashPage {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeChatPage.class);

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/claudechat";

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    private static final Parser markdownParser = Parser.builder().build();
    private static final HtmlRenderer markdownRenderer = HtmlRenderer.builder().build();

    private static String renderMarkdown(String markdown) {
        return Utils.sanitizeHtml(markdownRenderer.render(markdownParser.parse(markdown)));
    }

    private final IModel<String> inputModel = Model.of("");

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
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

        WebMarkupContainer chat = new WebMarkupContainer("chat");
        chat.setVisible(enabled);
        add(chat);

        getSession().bind();
        final String sessionKey = getSession().getId();

        WebMarkupContainer chatbox = new WebMarkupContainer("chatbox");
        chatbox.setOutputMarkupId(true);
        chatbox.add(new AjaxSelfUpdatingTimerBehavior(POLL_INTERVAL) {

            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                ClaudeSession s = ClaudeChatService.get().getSession(sessionKey);
                String path = s == null ? null : s.pollNavigation();
                if (path != null) {
                    // Path is validated by the open_page tool: in-app, no quotes or backslashes.
                    target.appendJavaScript("window.location = '" + path + "';");
                }
            }

        });
        chat.add(chatbox);

        chatbox.add(new ListView<ChatMessage>("messages", (IModel<List<ChatMessage>>) () -> {
            ClaudeSession s = ClaudeChatService.get().getSession(sessionKey);
            return s == null ? List.of() : s.getMessages();
        }) {

            @Override
            protected void populateItem(ListItem<ChatMessage> item) {
                ChatMessage message = item.getModelObject();
                if (message.getKind() == ChatMessage.Kind.ASSISTANT) {
                    item.add(new Label("message", renderMarkdown(message.getText())).setEscapeModelStrings(false));
                } else {
                    item.add(new Label("message", message.getText()));
                }
                item.add(new AttributeModifier("class", "chat-msg chat-msg-" + message.getKind().toString().toLowerCase()));
            }

        });

        chatbox.add(new Label("status", (IModel<String>) () -> {
            ClaudeSession s = ClaudeChatService.get().getSession(sessionKey);
            return s != null && s.isBusy() ? "Claude is working..." : "";
        }));

        Form<Void> form = new Form<>("form");
        chat.add(form);
        TextArea<String> input = new TextArea<>("input", inputModel);
        form.add(input);
        form.add(new AjaxButton("send") {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                String text = inputModel.getObject();
                if (text == null || text.isBlank()) return;
                try {
                    ClaudeChatService.get().getOrCreateSession(sessionKey).sendUserMessage(text.trim());
                    inputModel.setObject("");
                } catch (IOException ex) {
                    logger.error("Could not start Claude Code process", ex);
                    error("Could not start the Claude Code process: " + ex.getMessage());
                }
                target.add(chatbox, input);
            }

        });
        input.setOutputMarkupId(true);

        chat.add(new AjaxLink<Void>("reset") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ClaudeChatService.get().closeSession(sessionKey);
                target.add(chatbox);
            }

        });
    }

}
