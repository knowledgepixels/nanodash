/* Drawing for network views (issue #792). The server has already turned the view's
   query result into a node set and an edge set; everything here is about getting them
   onto a canvas, laid out, hoverable and clickable. */

/* Canvas text needs a real font family — the page's, so the diagram's labels read as
   part of it rather than as a picture pasted onto it. */
var NETWORK_FONT = "Inter, Verdana, Helvetica, sans-serif";

/* The instances drawn so far, so that the ones whose view has since been replaced can be
   shut down. vis-network listens on the window for resizes, which would otherwise pile up
   one dead listener per view refresh. Kept if it is already there: this script is asked for
   both by a view and by the placeholder that waits in its place, and a second evaluation
   must not lose hold of what the first one drew. */
var networkInstances = networkInstances || [];

/* A tooltip is built as an element with its text set as text, never as a string handed
   to the library: vis-network writes a string title into the popup with innerHTML, so a
   label coming from published nanopublication data would be parsed as markup there. The
   line breaks survive through `white-space: pre-line` on the class. */
function networkTooltip(text) {
  var el = document.createElement("div");
  el.className = "network-view-tooltip";
  el.textContent = text;
  return el;
}

/* Above this many edges, captions on them are noise rather than information: they overlap
   each other and the nodes, and the same text is a hover away. */
var EDGE_LABEL_LIMIT = 60;

/* How much of the layout is computed before the first frame. vis-network paints nothing
   while it stabilizes out of sight, so a long hidden phase is a box that stays empty: this
   is enough to pull the worst crossings out, and the rest of the settling is worth more on
   screen, where the movement shows the diagram arriving rather than hiding that it is. */
var PRESETTLE_ITERATIONS = 80;

/* While the graph settles on screen it keeps spreading, so the view follows it: a fit this
   often, until the layout comes to rest or this long, whichever is first. */
var FIT_INTERVAL_MS = 250;
var SETTLE_MAX_MS = 6000;

/* How long a box that never got drawn keeps claiming to be loading. Long enough that no
   slow connection is called a failure, short enough not to spin at a reader all day. */
var ENGINE_WAIT_MS = 15000;

/* A reader who has asked for less motion gets the finished layout instead of a settling one:
   the whole of it is computed before the first frame, the way it was when this view type
   could do nothing else. Dragging still moves the graph — that motion is theirs to start. */
function prefersReducedMotion() {
  return !!(window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches);
}

/* Repulsion has to grow with the graph, or everything past a few dozen nodes collapses into
   an unreadable ball in the middle. The numbers come from trying the real data. */
function networkOptions(nodeCount, edgeCount) {
  return {
    nodes: {
      shape: "dot",
      size: 12,
      borderWidth: 1,
      font: {size: 13, face: NETWORK_FONT, color: "#222"},
      /* Captions are wrapped rather than left to run into each other: a label of any
         length is one node's width wide, and the layout can keep nodes apart. */
      widthConstraint: {maximum: 150}
    },
    edges: {
      arrows: {to: {enabled: true, scaleFactor: 0.55}},
      color: {color: "#b8b8b8", highlight: "#707070", hover: "#707070"},
      font: {size: 11, face: NETWORK_FONT, color: "#666", strokeWidth: 3, strokeColor: "#fff",
        align: "middle"},
      smooth: {type: "continuous"},
      width: 1
    },
    physics: {
      solver: "barnesHut",
      barnesHut: {
        gravitationalConstant: -2000 - 400 * Math.min(nodeCount, 150),
        centralGravity: 0.1,
        springLength: 130,
        springConstant: 0.02,
        damping: 0.4,
        avoidOverlap: 0.2
      },
      /* The timestep is deliberately left at the library's 0.5. Raising it does shorten the
         settling — a graph of a hundred nodes came to rest half a second sooner at 0.7 —
         but a visible simulation gets no adaptive timestepping to catch an overshoot, and
         on a graph of several hundred nodes 0.7 left one of them bouncing between two
         positions at the velocity cap for as long as the page was open (issue #823).
         "Settled" is measured on the fastest node, though, so one twitching leaf is enough
         to hold the whole simulation in the render loop at the default threshold — and that
         is where the time actually was: the same graph took 7.7 s to come to rest at the
         library's 0.75 and 2.5 s at this one. A node moving this slowly cannot be seen. */
      minVelocity: 1.5,
      stabilization: {
        iterations: prefersReducedMotion() ? Math.min(1200, 300 + 12 * nodeCount) : PRESETTLE_ITERATIONS,
        fit: true
      }
    },
    interaction: {hover: true, tooltipDelay: 150, navigationButtons: false, keyboard: false},
    /* A fixed seed so the same graph comes up in the same arrangement on every page load:
       a layout that jumps around between visits is hard to recognize again. */
    layout: {randomSeed: 1}
  };
}

/* Shuts down the networks whose container has left the page. */
function pruneNetworks() {
  networkInstances = networkInstances.filter(function (entry) {
    if (entry.el.isConnected) return true;
    try {
      entry.net.destroy();
    } catch (e) {
      /* A half-torn-down instance is not worth a page error. */
    }
    return false;
  });
}

/* The pre-settle fits the view to a graph that is not the graph a moment later: the nodes
   keep spreading as the simulation runs on, and would wander off the canvas while they did.
   So the view follows them until the layout comes to rest. */
function followLayout(network, container) {
  var timer = setInterval(function () {
    /* A view replaced by a refresh in the meantime leaves a network pruneNetworks has
       destroyed, and nothing left to fit the view to. */
    if (!container.isConnected) {
      stop();
      return;
    }
    network.fit();
  }, FIT_INTERVAL_MS);

  function stop() {
    clearInterval(timer);
  }

  network.once("stabilized", stop);
  /* From the reader's first pan, drag or zoom the framing is theirs, and a view pulling the
     graph back into the box would be arguing with them. Both events come only from real
     input: a fit() of ours moves the view without raising either. */
  network.on("dragStart", stop);
  network.on("zoom", stop);
}

/* Not every graph comes to rest. A few hundred nodes are enough to leave one of them
   bouncing between two positions at the velocity cap, with the simulation — and the
   redrawing — going on for as long as the page is open. So every run of the simulation is
   given a deadline: on load, and again each time a drag wakes it. Stopping it leaves the
   nodes where they are and takes nothing away, since physics stays enabled and the next
   drag starts it over. This is deliberately not tied to the fitting above, which the reader
   calls off as soon as they touch the diagram: a reader who zooms in on a flickering node
   is the last person who should lose the thing that stops it. */
function capSettling(network, container) {
  var cap = null;
  network.on("startStabilizing", function () {
    clearTimeout(cap);
    cap = setTimeout(function () {
      if (container.isConnected) network.stopSimulation();
    }, SETTLE_MAX_MS);
  });
  network.on("stabilized", function () {
    clearTimeout(cap);
  });
}

/* One container. A failure is left in the console rather than taking the rest of the
   page's views down with it. */
function initNetwork(container) {
  if (typeof vis === "undefined" || !vis.Network) return;
  var dataEl = container.querySelector("script.network-view-data");
  if (!dataEl) return;
  var graph;
  try {
    graph = JSON.parse(dataEl.textContent);
  } catch (e) {
    console.error("network view: could not read its graph", e);
    return;
  }
  if (!graph || !graph.nodes || !graph.nodes.length) return;

  /* Claimed before anything is drawn, so a second pass leaves this container alone. */
  container.setAttribute("data-network-ready", "1");
  /* Taken hold of now because vis-network empties the container as it takes it over, and
     put back below: on a graph of any size the first frame is a few hundred milliseconds
     after that, and the box should not stand empty in between. */
  var spinner = container.querySelector(".refresh-spinner");

  /* One frame's grace before any of it: laying out a few hundred edges holds the main
     thread, and the box and its spinner are worth painting before it does. */
  requestAnimationFrame(function () {
    if (container.isConnected) drawNetwork(container, graph, spinner);
  });
}

/* The drawing itself, once the browser has had its frame. */
function drawNetwork(container, graph, spinner) {
  var urls = {};
  var nodes = graph.nodes.map(function (n) {
    urls[n.id] = n.url;
    return {
      id: n.id,
      label: n.label,
      title: n.title ? networkTooltip(n.title) : undefined,
      color: {background: n.color, border: n.color, highlight: {background: n.color, border: "#333"}}
    };
  });
  var edgeUrls = {};
  var edgeIris = {};
  var rawEdges = graph.edges || [];
  var withLabels = rawEdges.length <= EDGE_LABEL_LIMIT;
  var edges = rawEdges.map(function (e, i) {
    var id = "e" + i;
    edgeUrls[id] = e.url;
    edgeIris[id] = e.iri;
    return {
      id: id,
      from: e.from,
      to: e.to,
      label: withLabels ? e.label : undefined,
      title: e.title ? networkTooltip(e.title) : undefined
    };
  });

  var network = new vis.Network(container,
      {nodes: new vis.DataSet(nodes), edges: new vis.DataSet(edges)},
      networkOptions(nodes.length, edges.length));
  networkInstances.push({el: container, net: network});

  /* The spinner, back in the box the library just emptied, until there is something in it
     to look at instead. */
  if (spinner) {
    container.appendChild(spinner);
    network.once("afterDrawing", function () {
      if (spinner.parentNode) spinner.parentNode.removeChild(spinner);
    });
  }

  /* Added after the network, because vis-network empties the container when it takes it
     over. Nothing in it is interactive, so it never gets in the way of a click. */
  var status = document.createElement("div");
  status.className = "network-view-status";
  status.style.display = "none";
  container.appendChild(status);

  /* The IRI of whatever is under the pointer, shown the way a browser shows the target of
     the link you are on. Set as text, for the same reason the tooltips are. */
  function showStatus(iri) {
    status.textContent = iri || "";
    status.style.display = iri ? "block" : "none";
  }

  /* Leaving the canvas sideways does not always report a blur, and a status line left
     standing would name something the pointer is no longer on. */
  container.addEventListener("mouseleave", function () {
    showStatus(null);
  });

  /* The legend sits under the canvas and names the types by colour; pointing at one of its
     entries reports that type's IRI in the same corner, so every named thing in the view
     can be identified the same way. */
  var legend = container.parentNode && container.parentNode.querySelector(".network-view-legend");
  if (legend) {
    var entries = legend.querySelectorAll("[data-iri]");
    for (var e = 0; e < entries.length; e++) {
      (function (entry) {
        entry.addEventListener("mouseenter", function () {
          showStatus(entry.getAttribute("data-iri"));
        });
        entry.addEventListener("mouseleave", function () {
          showStatus(null);
        });
      })(entries[e]);
    }
  }

  /* Nothing switches the simulation off for good here, and that is the point: vis-network
     stops its own render loop as soon as the fastest node is slower than
     physics.minVelocity, so a settled graph costs nothing until something wakes it — and a
     drag does, which is what makes a dragged node bring its neighbours along instead of
     sliding out of its own diagram. capSettling is what handles the graphs that never
     settle; where the rest of the layout happens on screen, the view follows it there. */
  capSettling(network, container);
  if (!prefersReducedMotion()) followLayout(network, container);

  /* A click on a node or an edge goes to its page — on the release that ends the gesture,
     and only if nothing was dragged in between. vis-network reports a click from two
     different gestures: from a tap, which is a press short and still enough to be nothing
     else, and from a long press, which its gesture library raises on a timer a quarter of a
     second after the button goes down, while it is still down and before anything has moved.
     That second one is what made a diagram undraggable: pressing a node, taking aim and only
     then moving is exactly what a deliberate drag looks like, and the view had navigated away
     before the pointer ever moved. So a click whose gesture has already ended is followed
     where it is reported — nothing moved, and nothing more is coming — while one reported
     mid-gesture is held until the button comes up, by which time a drag, if there was one,
     has said so. */
  var heldUrl = null;
  var dragged = false;

  function clickTarget(params) {
    if (params.nodes && params.nodes.length) return urls[params.nodes[0]];
    if (params.edges && params.edges.length) return edgeUrls[params.edges[0]];
    return null;
  }

  network.on("click", function (params) {
    /* A fresh gesture: a long press is reported before the drag it may turn into, so what
       the flag says at release is this gesture's doing and no earlier one's. */
    heldUrl = null;
    dragged = false;
    var url = clickTarget(params);
    if (params.event && params.event.isFinal) {
      if (url) window.location.href = url;
    } else {
      heldUrl = url;
    }
  });
  network.on("dragStart", function () {
    dragged = true;
  });
  network.on("release", function () {
    var url = heldUrl;
    heldUrl = null;
    if (url && !dragged) window.location.href = url;
  });

  /* The pointer says which of the two kinds of hover leads somewhere. */
  network.on("hoverNode", function (params) {
    container.style.cursor = urls[params.node] ? "pointer" : "default";
    /* A node's id is its IRI. */
    showStatus(params.node);
  });
  network.on("hoverEdge", function (params) {
    container.style.cursor = edgeUrls[params.edge] ? "pointer" : "default";
    showStatus(edgeIris[params.edge]);
  });
  network.on("blurNode", function () {
    container.style.cursor = "default";
    showStatus(null);
  });
  network.on("blurEdge", function () {
    container.style.cursor = "default";
    showStatus(null);
  });
}

/* Idempotent, because it runs again after every Wicket AJAX call: a view that is already
   drawn is left alone, since re-initializing it would throw its layout away. */
function initNetworks() {
  pruneNetworks();
  var containers = document.querySelectorAll(".network-view-content:not([data-network-ready])");
  for (var i = 0; i < containers.length; i++) {
    initNetwork(containers[i]);
  }
}

/* Draws a view arriving in an AJAX response. The hook has to be installed even when this
   script is evaluated before Wicket's own has been — which it now can be, since it is
   loaded with the placeholder a lazily loaded view shows, so that the engine is on its way
   while the query is still running. A later AJAX contribution of the same script is skipped
   by its src rather than evaluated again, so this is the only chance there is to install it:
   without the hook, the view replacing the placeholder would never be drawn. */
function hookNetworkAjax() {
  if (window.networkAjaxHooked) return true;
  if (typeof Wicket === "undefined" || !Wicket.Event) return false;
  window.networkAjaxHooked = true;
  Wicket.Event.subscribe("/ajax/call/complete", function () {
    initNetworks();
  });
  return true;
}

/* A box the drawing never reached stops claiming to be loading: an engine that 404s or is
   blocked would otherwise leave the spinner turning at a reader for nothing. Armed once. */
function watchForMissingEngine() {
  if (window.networkEngineWatched) return;
  window.networkEngineWatched = true;
  setTimeout(function () {
    /* A drawn view takes its own spinner down with its first frame, so whatever is still
       here by now is waiting for something that is not coming. */
    var left = document.querySelectorAll(".network-view-content .refresh-spinner");
    for (var i = 0; i < left.length; i++) {
      left[i].parentNode.removeChild(left[i]);
    }
  }, ENGINE_WAIT_MS);
}

function startNetworks() {
  initNetworks();
  watchForMissingEngine();
  if (!hookNetworkAjax()) {
    /* Wicket's script is loaded synchronously in the head, so by either of these it is
       certainly there. Both are no-ops once the hook is in. */
    document.addEventListener("DOMContentLoaded", hookNetworkAjax);
    window.addEventListener("load", hookNetworkAjax);
  }
}

/* A view is usually loaded lazily, which means this script arrives in an AJAX response,
   long after DOMContentLoaded has fired — waiting for that event alone would leave such a
   view undrawn. The deferred second pass covers the case where the script is evaluated
   before the markup it belongs to has been put in place. */
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", startNetworks);
} else {
  startNetworks();
  setTimeout(initNetworks, 0);
}
