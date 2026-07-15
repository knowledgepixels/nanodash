package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.chat.ChatMessage;
import com.knowledgepixels.nanodash.chat.ClaudeChatService;
import com.knowledgepixels.nanodash.chat.ClaudeSession;
import com.knowledgepixels.nanodash.page.ClaudeChatPage;
import com.knowledgepixels.nanodash.page.NanodashPage;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * The Claude Code chat panel: message list, input form, and the poll that
 * streams responses and executes queued open_page navigations. Used docked
 * (collapsible, on every page) and as the body of the full-page
 * {@link ClaudeChatPage}. Conversation state lives in
 * {@link ClaudeChatService}, so it survives page navigation.
 * See docs/claude-code-chat.md.
 */
public class ClaudeChatPanel extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeChatPanel.class);

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    private static final Parser markdownParser = Parser.builder().build();
    private static final HtmlRenderer markdownRenderer = HtmlRenderer.builder().build();

    private static String renderMarkdown(String markdown) {
        return Utils.sanitizeHtml(markdownRenderer.render(markdownParser.parse(markdown)));
    }

    private final IModel<String> inputModel = Model.of("");
    private final boolean docked;
    private int lastRenderedState = -1;

    /**
     * Creates the chat panel.
     *
     * @param id     the Wicket markup ID
     * @param docked true for the collapsible corner panel, false for the
     *               full-page version (always expanded)
     */
    public ClaudeChatPanel(String id, boolean docked) {
        super(id);
        this.docked = docked;
        setOutputMarkupPlaceholderTag(true);

        getSession().bind();
        final String sessionKey = getSession().getId();

        add(new AttributeModifier("class", (IModel<String>) () -> docked
                ? "claude-chat-dock " + (isExpanded() ? "chat-expanded" : "chat-collapsed")
                : "claude-chat-full"));

        AjaxLink<Void> expandLink = new AjaxLink<>("expand") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                NanodashSession.get().setClaudeChatDockExpanded(true);
                target.add(ClaudeChatPanel.this);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(docked && !isExpanded());
            }

        };
        add(expandLink);

        WebMarkupContainer body = new WebMarkupContainer("body") {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(isExpanded());
            }

        };
        add(body);

        WebMarkupContainer header = new WebMarkupContainer("header");
        header.setVisible(docked);
        body.add(header);
        header.add(new BookmarkablePageLink<Void>("fullview", ClaudeChatPage.class));
        header.add(new AjaxLink<Void>("collapse") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                NanodashSession.get().setClaudeChatDockExpanded(false);
                target.add(ClaudeChatPanel.this);
            }

        });

        WebMarkupContainer chatbox = new WebMarkupContainer("chatbox");
        chatbox.setOutputMarkupId(true);
        body.add(chatbox);

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

        chatbox.add(new AjaxLink<Void>("interrupt") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ClaudeSession s = ClaudeChatService.get().getSession(sessionKey);
                if (s != null) s.interrupt();
                target.add(chatbox);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                ClaudeSession s = ClaudeChatService.get().getSession(sessionKey);
                setVisible(s != null && s.isBusy());
            }

        });

        // Poll for new messages and queued navigations; only re-render (and
        // re-scroll) the chat box when something actually changed.
        body.add(new AbstractAjaxTimerBehavior(POLL_INTERVAL) {

            @Override
            protected void onTimer(AjaxRequestTarget target) {
                ClaudeSession s = ClaudeChatService.get().getSession(sessionKey);
                String path = s == null ? null : s.pollNavigation();
                if (path != null) {
                    // Path is validated by the open_page tool: in-app, no quotes or backslashes.
                    target.appendJavaScript("window.location = '" + path + "';");
                    return;
                }
                int state = s == null ? 0 : s.getMessages().size() * 2 + (s.isBusy() ? 1 : 0);
                if (state != lastRenderedState) {
                    lastRenderedState = state;
                    target.add(chatbox);
                    appendScrollToBottom(target, chatbox);
                }
            }

        });

        Form<Void> form = new Form<>("form");
        body.add(form);
        TextArea<String> input = new TextArea<>("input", inputModel);
        input.setOutputMarkupId(true);
        form.add(input);
        form.add(new AjaxButton("send") {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                String text = inputModel.getObject();
                if (text == null || text.isBlank()) return;
                try {
                    ClaudeChatService.get().getOrCreateSession(sessionKey).sendUserMessage(text.trim(), getCurrentPagePath());
                    inputModel.setObject("");
                } catch (IOException ex) {
                    logger.error("Could not start Claude Code process", ex);
                    error("Could not start the Claude Code process: " + ex.getMessage());
                }
                target.add(chatbox, input);
                appendScrollToBottom(target, chatbox);
            }

        });

        form.add(new AjaxLink<Void>("reset") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ClaudeChatService.get().closeSession(sessionKey);
                target.add(chatbox);
            }

        });
    }

    private boolean isExpanded() {
        return !docked || NanodashSession.get().isClaudeChatDockExpanded();
    }

    /**
     * The in-app path of the page this panel is on (e.g. "/explore?id=..."),
     * sent along with each chat message so Claude knows what the user is
     * looking at.
     *
     * @return the path, or null if not on a Nanodash page
     */
    private String getCurrentPagePath() {
        if (!(getPage() instanceof NanodashPage page)) return null;
        String params = Utils.getPageParametersAsString(page.getPageParameters());
        return page.getMountPath() + (params.isEmpty() ? "" : "?" + params);
    }

    private static void appendScrollToBottom(AjaxRequestTarget target, WebMarkupContainer chatbox) {
        target.appendJavaScript("var e = document.getElementById('" + chatbox.getMarkupId() + "'); if (e) e.scrollTop = e.scrollHeight;");
    }

}
