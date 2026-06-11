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

document.addEventListener("DOMContentLoaded", function() {
  wrapLeadingEmoji();
  wrapCellEmoji();
  renderFriendlyDates();
  // Re-run after Wicket AJAX calls complete (dynamically loaded content)
  if (typeof Wicket !== "undefined" && Wicket.Event) {
    Wicket.Event.subscribe("/ajax/call/complete", function() {
      wrapLeadingEmoji();
      wrapCellEmoji();
      renderFriendlyDates();
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
  adjustValueWidths();
  setCollapseOverflow();
  collapseNanopubAssertions();
};

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
  var rect = this.getBoundingClientRect();
  var spaceRight = window.innerWidth - rect.left;
  var spaceLeft = rect.right;
  if (spaceRight >= 250) {
    el.style.left = '0';
    el.style.right = 'auto';
  } else if (spaceLeft >= 250) {
    el.style.left = 'auto';
    el.style.right = '0';
  } else {
    el.style.position = 'fixed';
    el.style.left = '5px';
    el.style.right = '5px';
    el.style.top = rect.bottom + 'px';
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

function toggleMobileNav() {
  $('#titlebar').toggleClass('nav-open');
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
