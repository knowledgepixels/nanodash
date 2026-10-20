/* Drawing for network views (issue #792). The server has already turned the view's
   query result into a node set and an edge set; everything here is about getting them
   onto a canvas, laid out, hoverable and clickable. */

/* Canvas text needs a real font family — the page's, so the diagram's labels read as
   part of it rather than as a picture pasted onto it. */
var NETWORK_FONT = "Inter, Verdana, Helvetica, sans-serif";

/* The instances drawn so far, so that the ones whose view has since been replaced can be
   shut down. vis-network listens on the window for resizes, which would otherwise pile up
   one dead listener per view refresh. */
var networkInstances = [];

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
      stabilization: {iterations: Math.min(1200, 300 + 12 * nodeCount), fit: true}
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
  container.setAttribute("data-network-ready", "1");
  networkInstances.push({el: container, net: network});

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

  /* The layout is worth the CPU once; leaving the simulation running would keep every
     network view on the page busy for as long as it is open. */
  network.once("stabilizationIterationsDone", function () {
    network.setOptions({physics: false});
  });

  /* A click on a node or an edge goes to its page. Dragging a node reports no click, so
     rearranging the layout by hand does not navigate away from it. */
  network.on("click", function (params) {
    var url = null;
    if (params.nodes && params.nodes.length) {
      url = urls[params.nodes[0]];
    } else if (params.edges && params.edges.length) {
      url = edgeUrls[params.edges[0]];
    }
    if (url) window.location.href = url;
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

function startNetworks() {
  initNetworks();
  if (typeof Wicket !== "undefined" && Wicket.Event && !window.networkAjaxHooked) {
    window.networkAjaxHooked = true;
    Wicket.Event.subscribe("/ajax/call/complete", function () {
      initNetworks();
    });
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
