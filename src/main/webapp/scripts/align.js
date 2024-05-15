function getMaxWidth(type) {
  max = 0;
  $(type).each(function(){
    w = parseInt($(this).width());
    if (w > max) { max = w; }
  });
  return max;
}

function enableNanopubAlignment() {
  return;  // currently disabled
  $(window).on('load', function() {
    maxs = getMaxWidth(".nanopub-assertion .nanopub-statement .subj");
    $(".nanopub-assertion .nanopub-statement .subj").each(function(){
      $(this).width(maxs);
    });
    maxp = getMaxWidth(".nanopub-assertion .nanopub-statement .pred");
    $(".nanopub-assertion .nanopub-statement .pred").each(function(){
      $(this).width(maxp);
    });
  });
}