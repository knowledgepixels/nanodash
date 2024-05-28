function getMaxWidth(el, type, limit) {
  max = 0;
  $(el).find(type).each(function() {
    w = parseInt($(this).width());
    if (w > max && w < limit) { max = w; }
  });
  return max;
}

$(window).on('load', updateElements);

function updateElements() {
  adjustValueWidths();
  setCollapseOverflow();
  collapseNanopubAssertions();
};

function adjustValueWidths() {
  limit = 251;
  $(".nanopub-graph").each(function() {
    updateNanopubGraph(this);
  });
}

function setCollapseOverflow() {
  $(".collapse-overflow").each(function() {
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
  maxs = getMaxWidth(el, ".nanopub-statement .subj", limit);
  maxp = getMaxWidth(el, ".nanopub-statement .pred", limit);
  $(el).find(".nanopub-statement").each(function() {
    limitExceeded = false;
    if (maxs > 0) {
      $(this).find(".subj").each(function() {
        if ($(this).width() < limit) {
          $(this).width(maxs + 1);
        } else {
          limitExceeded = true;
        }
      });
    }
    if (maxp > 0 ) {
      $(this).find(".pred").each(function() {
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
  $(el).parent().find('.nanopub-pubinfo').each(function() {
    $(this).show();
  });
  $(el).hide();
  $($(el).parent().find('.collapse')[0]).show();
  $(el).parent().find(".nanopub-graph").each(function() {
    updateNanopubGraph(this);
  });
}

function collapsePubinfo(el) {
  first=true;
  $(el).parent().find('.nanopub-pubinfo').each(function() {
    if (!first) { $(this).hide(); }
    first=false;
  });
  $(el).hide();
  $($(el).parent().find('.expand')[0]).show();
}

function expandAssertion(el) {
  $(el).closest('.nanopub-assertion').find('.nanopub-statement, .nanopub-group, hr').each(function() {
    $(this).show();
  });
  $(el).hide();
  $($(el).parent().find('.collapse')[0]).show();
  $(el).parent().find(".nanopub-graph").each(function() {
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
  n = $(a).find(".nanopub-statement").length;$
  if (n < 10) return;
  $($(a).find(".expand")[0]).show();
  c = 0;
  $(a).find(".nanopub-statement, .nanopub-group, hr").each(function() {
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
