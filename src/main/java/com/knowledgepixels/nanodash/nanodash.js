function getMaxWidth(el, type, limit) {
  max = 0;
  $(el).find(type).each(function() {
    w = parseInt($(this).width());
    if (w > max) {
      if (w < limit) {
        max = w;
      } else {
        max = limit;
      }
    }
  });
  return max;
}

$(window).on('load', function() {
  adjustValueWidths();
  addExpandCollapseHandles();
});

function addExpandCollapseHandles() {
  $(".nanopub-view").each(function() {
    $(this).append('<div class="nanopub-expand" onclick="expandNanopub(this);"></div>');
    $(this).append('<div class="nanopub-collapse" onclick="collapseNanopub(this);"></div>');
  });
}

function adjustValueWidths() {
  limit = 251;
  $(".nanopub-graph").each(function() {
    updateNanopubGraph(this);
  });
}

function updateNanopubGraph(el) {
  maxs = getMaxWidth(el, ".nanopub-statement .subj", limit);
  maxp = getMaxWidth(el, ".nanopub-statement .pred", limit);
  $(el).find(".nanopub-statement").each(function() {
    limitExceeded = false;
    if (maxs > 0 ) {
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

function expandNanopub(el) {
  $(el).parent().find('.nanopub-pubinfo').each(function() {
    $(this).show();
  });
  $(el).hide();
  $($(el).parent().find('.nanopub-collapse')[0]).show();
}

function collapseNanopub(el) {
  first=true;
  $(el).parent().find('.nanopub-pubinfo').each(function() {
    if (!first) { $(this).hide(); }
    first=false;
  });
  $(el).hide();
  $($(el).parent().find('.nanopub-expand')[0]).show();
}