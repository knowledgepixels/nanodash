/* Emoji wrapping — no jQuery dependency, runs on DOMContentLoaded and
   also called from updateElements() for AJAX-loaded content. */
function wrapLeadingEmoji() {
  document.querySelectorAll("h1, h2, h3, h4, h5, h6").forEach(function (el) {
    if (el.querySelector(".emoji")) return;
    var walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
    var node = walker.nextNode();
    if (!node) return;
    var match = node.textContent.match(/^\s*((?:\p{Extended_Pictographic}|[\u{13000}-\u{1342F}])\uFE0F?)/u);
    if (!match) return;
    var span = document.createElement("span");
    span.className = "emoji";
    span.textContent = match[1].replace(/\uFE0F/g, "");
    node.textContent = node.textContent.slice(match[0].indexOf(match[1]) + match[1].length);
    node.parentNode.insertBefore(span, node);
  });
}
/* Strip the U+FE0F variation selector from EVERY emoji inside result-table body
   cells, so the ✅/⚠️ key-approval annotations (and any other in-cell emoji)
   render in the monochrome Noto Emoji font that leads our font stacks instead of
   the system color font. No wrapper element or class is added, so they keep the
   cell's own text color and size. Idempotent: once stripped the replace is a
   no-op, so re-running after Wicket AJAX is safe. */
var EMOJI_PATTERN = "(?:\\p{Extended_Pictographic}|[\\u{13000}-\\u{1342F}])\\uFE0F?";
function wrapCellEmoji() {
  document.querySelectorAll(".result-table td").forEach(function (cell) {
    var re = new RegExp(EMOJI_PATTERN, "u");
    var walker = document.createTreeWalker(cell, NodeFilter.SHOW_TEXT);
    var nodes = [];
    var node;
    while ((node = walker.nextNode())) {
      if (re.test(node.textContent)) nodes.push(node);
    }
    nodes.forEach(function (n) {
      var stripped = n.textContent.replace(/\uFE0F/g, "");
      if (stripped !== n.textContent) n.textContent = stripped;
    });
  });
}
/* Friendly date rendering — turns <time class="friendly-date" datetime="..."> into a
   relative form ("10 minutes ago") in the viewer's local timezone, with the absolute
   date-time in the tooltip. Falls back silently to the server-rendered text if the value
   does not parse. No jQuery dependency; safe to call repeatedly (idempotent). */
function friendlyRelative(date, absDateFallback) {
  var diffSec = Math.round((date.getTime() - Date.now()) / 1000); // negative = past
  if (Math.abs(diffSec) < 45) return "just now";
  if (typeof Intl === "undefined" || !Intl.RelativeTimeFormat) return absDateFallback;
  var rtf = new Intl.RelativeTimeFormat(undefined, { numeric: "auto" });
  var min = Math.round(diffSec / 60);
  if (Math.abs(min) < 60) return rtf.format(min, "minute");
  var hr = Math.round(diffSec / 3600);
  if (Math.abs(hr) < 24) return rtf.format(hr, "hour");
  var day = Math.round(diffSec / 86400);
  if (Math.abs(day) < 7) return rtf.format(day, "day");
  return absDateFallback; // older than a week → absolute date
}

function renderFriendlyDates(root) {
  var scope = root || document;
  scope.querySelectorAll("time.friendly-date[datetime]").forEach(function (el) {
    if (el.dataset.friendlyRendered === "1") return;
    var d = new Date(el.getAttribute("datetime"));
    if (isNaN(d.getTime())) return; // unparseable → leave server-rendered text as-is
    el.dataset.friendlyRendered = "1";
    // Full, pretty tooltip: weekday + full date + time with seconds and timezone,
    // e.g. "Thursday, 16 April 2026 at 10:27:12 CEST".
    var absFull = d.toLocaleString(undefined, { dateStyle: "full", timeStyle: "long" });
    var absDate = d.toLocaleDateString(undefined, { dateStyle: "medium" });
    el.setAttribute("title", absFull);
    el.textContent = friendlyRelative(d, absDate);
  });
}

/* The address of the page as it is worth sending to somebody else. Two things Wicket
   puts there belong to the current visit only and are left out:
   - the counter for the page instance it is serving, at the front of the query string
     as a parameter with no value: ".../space?3&id=...". Any other valueless number
     goes the same way; nanodash's own parameters all have names.
   - the session id, which Wicket writes into the path as ";jsessionid=..." when the
     visitor has cookies disabled: ".../space;jsessionid=79B384...?id=...". Sending
     that on would hand the recipient a live session. */
function shareableUrl() {
  var url = window.location.href.split("#")[0];
  var queryStart = url.indexOf("?");
  var path = (queryStart === -1 ? url : url.slice(0, queryStart))
      .replace(/;jsessionid=[^/]*/gi, "");
  if (queryStart === -1) return path;
  var params = url.slice(queryStart + 1).split("&").filter(function (param) {
    return !/^[0-9]+$/.test(param);
  });
  return path + (params.length ? "?" + params.join("&") : "");
}

/* Section anchors — every view display of a page carries a fragment identifier
   (server-side, see ViewAnchors): on its wrapping .listview element where ViewList
   renders it, and on the panel itself (.view-section) on the pages that build their
   view panels directly. Either way a single section can be linked to:
   ".../space?id=...#messages". Here we make that linkable from the page: a "#" handle
   next to each section title that both navigates to the section and copies the full
   link. Idempotent, so it can re-run after Wicket AJAX has added more sections. */
function addSectionAnchors(root) {
  var scope = root || document;
  scope.querySelectorAll(".view-group > .listview[id], .view-section[id]").forEach(function (section) {
    // A section without a real fragment identifier gets no handle: a "#" whose href is
    // the bare "#" would look like a section link and jump to the top of the page.
    if (!section.id) return;
    // The panel's own title row; the fallback catches title markup we don't know about.
    var heading = section.querySelector(".paneltitlerow > h3, .paneltitlerow > h4, .paneltitlerow > h5, .view-header-titlerow > h3")
        || section.querySelector("h3, h4, h5");
    if (!heading || heading.querySelector(".section-anchor")) return;
    var link = document.createElement("a");
    link.className = "section-anchor";
    link.href = "#" + section.id;
    link.title = "Link to this section";
    link.setAttribute("aria-label", "Link to this section");
    link.textContent = "#";
    link.addEventListener("click", function () {
      // The href already moves the browser to the section; additionally put the full
      // link on the clipboard, which is what one actually wants it for.
      if (!navigator.clipboard) return;
      var url = shareableUrl() + "#" + section.id;
      navigator.clipboard.writeText(url).then(function () {
        showToast("Link to section copied to clipboard!");
      }, function () { /* clipboard denied: the plain link still works */ });
    });
    heading.appendChild(link);
  });
}

/* Scrolling to a section that isn't there yet. Most view displays load over Ajax after
   the initial render, so at the moment the browser handles the fragment its target
   often does not exist. We therefore keep re-scrolling to it as sections arrive, until
   the page settles (ANCHOR_SETTLE_MS) or the user scrolls somewhere themselves. */
var ANCHOR_SETTLE_MS = 15000;
var anchorTarget = null;
var anchorDeadline = 0;

function startAnchorTracking() {
  var hash = window.location.hash.slice(1);
  if (!hash) {
    anchorTarget = null;
    return;
  }
  try {
    anchorTarget = decodeURIComponent(hash);
  } catch (e) {
    anchorTarget = hash;
  }
  anchorDeadline = Date.now() + ANCHOR_SETTLE_MS;
  scrollToAnchor();
}

function scrollToAnchor() {
  if (!anchorTarget) return;
  if (Date.now() > anchorDeadline) {
    anchorTarget = null;
    return;
  }
  var el = document.getElementById(anchorTarget);
  if (el) el.scrollIntoView();
}

/* Any deliberate scrolling by the user wins over the pending anchor. */
["wheel", "touchmove", "keydown"].forEach(function (evt) {
  window.addEventListener(evt, function () { anchorTarget = null; }, { passive: true });
});
window.addEventListener("hashchange", startAnchorTracking);

/* Publishing takes a moment — the nanopublication is signed and sent to the registry — and
   the form is submitted the plain way, so the page stays as it is until the server answers.
   The button says what is going on for that while, and stops taking clicks: a second one
   would submit the form again and publish twice. Returns false so the click itself does not
   submit on top of the submit below. */
function startPublishing(button) {
  if (button.disabled) return false;
  var form = button.form;
  button.textContent = "Publishing";
  button.insertBefore(makeSpinner(), button.firstChild);
  button.classList.add("publishing");
  // Every button of the form, so the preview button cannot be used to leave mid-publish.
  form.querySelectorAll("button, input[type=submit]").forEach(function (b) { b.disabled = true; });
  form.submit();
  return false;
}

function makeSpinner() {
  var spinner = document.createElement("span");
  spinner.className = "refresh-spinner";
  return spinner;
}

/* The client-side half of the "an update is happening" indicator. Ajax updates started
   from inside a view panel — filtering, paging, sorting — get the same spinner in the
   panel's left gutter that the server puts there while a view loads or refreshes
   (LoadingResultPanel / RefreshingResultPanel), so every kind of update looks alike.
   Shown only after a delay: these round trips normally take tens of milliseconds, and a
   spinner flashing on every keystroke would be worse than none. */
var UPDATE_SPINNER_DELAY_MS = 250;
/* Backstop for a call that never reports completion, so a spinner cannot get stuck. */
var UPDATE_SPINNER_MAX_MS = 30000;
var updatingPanels = new Map();

function isVisible(el) {
  return !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length);
}

/* The view panel an Ajax call was triggered from, or null for calls that belong to no
   single panel — the page-wide lazy-load and refresh-poll timers among them, which is
   why they never light up every panel on the page. */
function findUpdatingPanel(attributes) {
  var id = attributes && attributes.c;
  if (!id || typeof id !== "string") return null;
  var el = document.getElementById(id);
  if (!el) return null;
  var panel = el.closest('[class*="col-"]');
  // A view panel is a column with a title row; anything else (a page-level column, a
  // form) is left alone, since the gutter position is meaningless there.
  return panel && panel.querySelector(".paneltitlerow") ? panel : null;
}

function showUpdateSpinner(panel) {
  var state = updatingPanels.get(panel);
  if (!state || state.spinner || !panel.isConnected) return;
  // A spinner the server already put there (the view is loading or refreshing) says the
  // same thing; a second one would only be noise, and removing it later is not ours to do.
  var existing = panel.querySelector(".refresh-spinner");
  if (existing && isVisible(existing)) return;
  // Right after the title, where the view's own spinner goes; the title row's layout keeps
  // it clear of the title icon and of the filter and menu on the right.
  var titleRow = panel.querySelector(".paneltitlerow");
  var title = titleRow ? titleRow.querySelector("h4") : null;
  if (!titleRow) return;
  var spinner = makeSpinner();
  spinner.title = "Updating...";
  panel.classList.add("view-refreshing");
  titleRow.insertBefore(spinner, title ? title.nextSibling : titleRow.firstChild);
  state.spinner = spinner;
}

function hideUpdateSpinner(panel) {
  var state = updatingPanels.get(panel);
  updatingPanels.delete(panel);
  if (!state) return;
  if (state.showTimer) clearTimeout(state.showTimer);
  if (state.maxTimer) clearTimeout(state.maxTimer);
  if (!state.spinner) return;
  state.spinner.remove();
  // Only ours to take back: the class may equally have come from the server-rendered
  // loading state, which removes it itself.
  if (!panel.querySelector(".refresh-spinner")) {
    panel.classList.remove("view-refreshing");
  }
}

function onUpdateStart(panel) {
  var state = updatingPanels.get(panel);
  if (state) {
    state.count++;
    return;
  }
  state = {count: 1, spinner: null, showTimer: null, maxTimer: null};
  updatingPanels.set(panel, state);
  state.showTimer = setTimeout(function () { showUpdateSpinner(panel); }, UPDATE_SPINNER_DELAY_MS);
  state.maxTimer = setTimeout(function () { hideUpdateSpinner(panel); }, UPDATE_SPINNER_MAX_MS);
}

function onUpdateEnd(panel) {
  var state = updatingPanels.get(panel);
  if (!state) return;
  state.count--;
  if (state.count <= 0) hideUpdateSpinner(panel);
}

function trackAjaxUpdates() {
  if (typeof Wicket === "undefined" || !Wicket.Event) return;
  Wicket.Event.subscribe("/ajax/call/before", function (jqEvent, attributes) {
    var panel = findUpdatingPanel(attributes);
    if (panel) onUpdateStart(panel);
  });
  Wicket.Event.subscribe("/ajax/call/complete", function (jqEvent, attributes) {
    var panel = findUpdatingPanel(attributes);
    if (panel) onUpdateEnd(panel);
  });
}

document.addEventListener("DOMContentLoaded", function() {
  wrapLeadingEmoji();
  wrapCellEmoji();
  renderFriendlyDates();
  addSectionAnchors();
  startAnchorTracking();
  trackAjaxUpdates();
  // Re-run after Wicket AJAX calls complete (dynamically loaded content)
  if (typeof Wicket !== "undefined" && Wicket.Event) {
    Wicket.Event.subscribe("/ajax/call/complete", function() {
      wrapLeadingEmoji();
      wrapCellEmoji();
      renderFriendlyDates();
      addSectionAnchors();
      scrollToAnchor();
    });
  }
});

function getMaxWidth(el, type, limit) {
  max = 0;
  $(el).find(type).each(function () {
    w = parseInt($(this).width());
    if (w > max && w < limit) {
      max = w;
    }
  });
  return max;
}

$(window).on('load', updateElements);

function updateElements() {
  wrapLeadingEmoji();
  wrapCellEmoji();
  renderFriendlyDates();
  addSectionAnchors();
  adjustValueWidths();
  setCollapseOverflow();
  collapseNanopubAssertions();
  scrollToAnchor();
};

/* Kendo's date and time pickers only open their popup from the button beside the field, so
   clicking the field itself does nothing visible. Clicking a date or time field looks like it
   should offer the calendar or the clock, so here it does. Delegated from the document, so
   pickers that arrive with a Wicket AJAX response are covered too. */
$(document).on('click', 'input.k-input-inner', function () {
  var field = $(this);
  var picker = field.data('kendoDatePicker') || field.data('kendoTimePicker') || field.data('kendoDateTimePicker');
  if (!picker || typeof picker.open !== 'function') return;
  if (picker.popup && picker.popup.visible()) return;
  picker.open();
});

$(document).on('mouseenter', '.tooltip, .expltooltip', function () {
  var tip = $(this).children('.tooltiptext, .expltooltiptext');
  if (!tip.length) return;
  if (window.innerWidth > 768) return;
  var parent = this.getBoundingClientRect();
  var el = tip[0];
  el.style.position = 'fixed';
  el.style.left = '5px';
  el.style.right = '5px';
  el.style.top = Math.min(parent.bottom + 2, window.innerHeight - 200) + 'px';
  el.style.width = 'auto';
  el.style.maxWidth = 'none';
  el.style.minWidth = '0';
});

$(document).on('mouseenter', '.actionmenu', function () {
  var content = $(this).children('.actionmenu-content');
  if (!content.length) return;
  var el = content[0];
  el.style.left = '';
  el.style.right = '';
  el.style.position = '';
  el.style.top = '';
  var rect = this.getBoundingClientRect();
  var spaceRight = window.innerWidth - rect.left;
  var spaceLeft = rect.right;
  // The chevron sits at the right of its row, so prefer opening leftward
  // (right-aligned); only fall back to opening rightward when the left lacks room.
  if (spaceLeft >= 250) {
    el.style.left = 'auto';
    el.style.right = '0';
  } else if (spaceRight >= 250) {
    el.style.left = '0';
    el.style.right = 'auto';
  } else {
    el.style.position = 'fixed';
    el.style.left = '5px';
    el.style.right = '5px';
    el.style.top = rect.bottom + 'px';
  }
});

// A submenu flyout (e.g. the calendar menu's groups) opens to the right of its
// parent menu; flip it leftward when the right edge of the viewport lacks room.
$(document).on('mouseenter', '.actionmenu-subitem', function () {
  var content = $(this).children('.actionmenu-subcontent');
  if (!content.length) return;
  var el = content[0];
  el.style.left = '';
  el.style.right = '';
  var rect = this.getBoundingClientRect();
  var spaceRight = window.innerWidth - rect.right;
  if (spaceRight >= 250 || spaceRight >= rect.left) {
    el.style.left = '100%';
    el.style.right = 'auto';
  } else {
    el.style.left = 'auto';
    el.style.right = '100%';
  }
});

function adjustValueWidths() {
  if (window.innerWidth <= 768) return;
  limit = 251;
  $(".nanopub-graph").each(function () {
    updateNanopubGraph(this);
  });
}

function setCollapseOverflow() {
  $(".collapse-overflow").each(function () {
    p = $(this).find('.collapse-content')[0];
    if ($(p).height() > 45) {
      $(p).css('max-height', '36px');
    } else {
      $($(this).find(".expand")[0]).hide();
    }
  });
}

function expandOverflow(el) {
  $($(el).closest('.collapse-overflow').find('.collapse-content')[0]).css('max-height', 'none');
  $(el).hide();
  $($(el).closest('.collapse-overflow').find(".collapse")[0]).show();
}

function collapseOverflow(el) {
  $($(el).closest('.collapse-overflow').find('.collapse-content')[0]).css('max-height', '36px');
  $(el).hide();
  $($(el).closest('.collapse-overflow').find(".expand")[0]).show();
}

function updateNanopubGraph(el) {
  // Reset widths so they can be recalculated based on current visibility
  $(el).find(".nanopub-statement .subj, .nanopub-statement .pred").each(function () {
    $(this).css('width', 'auto');
  });

  maxs = getMaxWidth(el, ".nanopub-statement .subj", limit);
  maxp = getMaxWidth(el, ".nanopub-statement .pred", limit);
  $(el).find(".nanopub-statement").each(function () {
    limitExceeded = false;
    if (maxs > 0) {
      $(this).find(".subj").each(function () {
        if ($(this).width() < limit) {
          $(this).width(maxs + 1);
        } else {
          limitExceeded = true;
        }
      });
    }
    if (maxp > 0) {
      $(this).find(".pred").each(function () {
        if ($(this).width() < limit && !limitExceeded) {
          $(this).width(maxp + 1);
        }
      });
    }
  });
}

function updateNanopubGraphForId(id) {
  updateNanopubGraph($('#' + id).closest('.nanopub-graph'));
}

function expandPubinfo(el) {
  $(el).parent().find('.nanopub-pubinfo').each(function () {
    $(this).show();
  });
  $(el).hide();
  $($(el).parent().find('.collapse')[0]).show();
  $(el).parent().find(".nanopub-graph").each(function () {
    updateNanopubGraph(this);
  });
}

function collapsePubinfo(el) {
  first = true;
  $(el).parent().find('.nanopub-pubinfo').each(function () {
    if (!first) {
      $(this).hide();
    }
    first = false;
  });
  $(el).hide();
  $($(el).parent().find('.expand')[0]).show();
}

function expandAssertion(el) {
  $(el).closest('.nanopub-assertion').find('.nanopub-statement, .nanopub-group, hr').each(function () {
    $(this).show();
  });
  $(el).hide();
  $($(el).parent().find('.collapse')[0]).show();
  $(el).parent().find(".nanopub-graph").each(function () {
    updateNanopubGraph(this);
  });
}

function collapseAssertion(el) {
  collapseNanopubAssertion($(el).closest('.nanopub-view'));
  $(el).hide();
  $($(el).parent().find('.expand')[0]).show();
}

function collapseNanopubAssertions() {
  $(".nanopub-view").each(function () {
    collapseNanopubAssertion($(this));
  });
}

function collapseNanopubAssertion(el) {
  a = $(el).find(".nanopub-assertion")[0];
  n = $(a).find(".nanopub-statement").length;
  $
  if (n < 10) return;
  $($(a).find(".expand")[0]).show();
  c = 0;
  $(a).find(".nanopub-statement, .nanopub-group, hr").each(function () {
    if (c > 5) {
      $(this).hide();
    } else {
      $(this).show();
    }
    if ($(this).hasClass("nanopub-statement")) {
      c = c + 1;
    }
  });
}

function showMore(el) {
  const $longLiteral = $(el).siblings('.long-literal');
  let maxHeight = '80px'; // if you update this also update it in the style.css file

  if ($longLiteral.hasClass('collapsed')) {
    $longLiteral.css('max-height', 'none');
    $longLiteral.removeClass('collapsed').addClass('expanded');
    $(el).css('transform', 'scale(1 ,-1');
  } else {
    $longLiteral.css('max-height', maxHeight);
    $longLiteral.removeClass('expanded').addClass('collapsed');
    $(el).css('transform', 'scale(1, 1)');
  }
}

// Show a transient, auto-dismissing message at the top of the viewport, styled
// like the post-publish confirmation box. Used e.g. for the "link copied"
// feedback instead of a blocking alert().
function showToast(message) {
  var existing = document.getElementById('nanodash-toast');
  if (existing) existing.remove();
  var toast = document.createElement('div');
  toast.id = 'nanodash-toast';
  toast.className = 'nanodash-toast';
  toast.textContent = message;
  document.body.appendChild(toast);
  // Trigger the fade-in on the next frame so the transition runs.
  requestAnimationFrame(function () {
    toast.classList.add('nanodash-toast-visible');
  });
  setTimeout(function () {
    toast.classList.remove('nanodash-toast-visible');
    setTimeout(function () { toast.remove(); }, 400);
  }, 2500);
}

function toggleView() {
  $('.view-selector .list').on('click', function () {
    $('.flex-container').addClass('list-view').removeClass('grid-view');
  });
  $('.view-selector .grid').on('click', function () {
    $('.flex-container').addClass('grid-view').removeClass('list-view');
  });
}

function toggleMode() {
  var body = document.body;
  var toggleButton = document.getElementById('mode-toggle');
  var toggleText = toggleButton.querySelector('.mode-toggle-text');

  if (body.classList.contains('mode-advanced')) {
    body.classList.remove('mode-advanced');
    toggleText.textContent = 'show more';
  } else {
    body.classList.add('mode-advanced');
    toggleText.textContent = 'show less';
  }

  // Recalculate layout after visibility changes have been applied
  requestAnimationFrame(function () {
    requestAnimationFrame(function () {
      updateElements();
    });
  });
}
