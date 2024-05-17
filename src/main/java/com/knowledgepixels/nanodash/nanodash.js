function getMaxWidth(el, type, limit) {
  max = 0;
  $(el).find(type).each(function() {
    w = parseInt($(this).width());
    if (w > max && w < limit) { max = w; }
  });
  return max + 1;
}

$(window).on('load', function() {
  adjustValueWidths(this);
  addExpandCollapseHandles();
});

function addExpandCollapseHandles() {
  $(".nanopub-view").each(function() {
    first=true;
    $(this).find('.nanopub-pubinfo').each(function() {
      if (!first) { $(this).hide(); }
      first=false;
    });
    $(this).append('<div class="nanopub-expand" onclick="expandNanopub(this);">V</div>');
    $(this).append('<div class="nanopub-collapse" onclick="collapseNanopub(this);">V</div>');
  });
}

function adjustValueWidths(el) {
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
    $(this).find(".subj").each(function() {
      if ($(this).width() < limit) {
        $(this).width(maxs);
      } else {
        limitExceeded = true;
      }
    });
    $(this).find(".pred").each(function() {
      if ($(this).width() < limit && !limitExceeded) {
        $(this).width(maxp);
      }
    });
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