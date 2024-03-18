const serverNpApiUrls = ['https://server.np.trustyuri.net/', 'https://np.knowledgepixels.com/']
const grlcNpApiUrls = ['https://grlc.services.np.trustyuri.net/api/local/local/', 'https://grlc.nps.knowledgepixels.com/api/local/local/'];

function getStatus(elementId, npUri) {
  document.getElementById(elementId).innerHTML = "<em>Checking for updates...</em>";
  getStatusX(elementId, npUri, [...serverNpApiUrls]);
}

function getStatusX(elementId, npUri, apiUrls) {
  if (apiUrls.length == 0) {
    document.getElementById(elementId).innerHTML = "<em>An error has occurred while checking the status of this nanopublication.</em>";
    return;
  }
  var apiUrl = apiUrls.shift();
  //console.log('Trying ' + apiUrl);
  requestUrl = apiUrl + npUri.slice(-45);
  var r = new XMLHttpRequest();
  r.open('GET', requestUrl, true);
  r.setRequestHeader('Accept', 'application/trig');
  r.onload = function() {
    var h = '';
    if (r.status == 200) {
      getUpdateStatusX(elementId, npUri, [...grlcNpApiUrls]);
    } else if (r.status >= 400 && r.status < 500) {
      document.getElementById(elementId).innerHTML = "<em>This nanopublication doesn't seem to be properly published (yet). This can take a minute or two for new nanopublications.</em>";
    } else {
      getStatusX(elementId, npUri, apiUrls);
    }
  };
  r.onerror = function(error) {
    getStatusX(elementId, npUri, apiUrls);
  }
  r.send();
}

function getUpdateStatusX(elementId, npUri, apiUrls) {
  if (apiUrls.length == 0) {
    document.getElementById(elementId).innerHTML = "<em>This nanopublication seems fully published but an error has occurred while checking for updates.</em>";
    return;
  }
  var apiUrl = apiUrls.shift();
  //console.log('Trying ' + apiUrl);
  requestUrl = apiUrl + '/get_latest_version?np=' + npUri;
  var r = new XMLHttpRequest();
  r.open('GET', requestUrl, true);
  r.setRequestHeader('Accept', 'application/json');
  r.responseType = 'json';
  r.onload = function() {
    var h = '';
    if (r.status == 200) {
      const bindings = r.response['results']['bindings'];
      if (bindings.length == 1 && bindings[0]['latest']['value'] === npUri) {
        h = 'This is the latest version.';
      } else if (bindings.length == 0) {
        h = 'This nanopublication has been <strong>retracted</strong>.'
      } else {
        h = 'This nanopublication has a <strong>newer version</strong>: ';
        if (bindings.length > 1) {
        h = 'This nanopublication has <strong>newer versions</strong>: ';
        }
        for (const b of bindings) {
          l = b['latest']['value'];
          h += ' <a href="./explore?id=' + l + '">' + l.substring(l.length-45, l.length-35) + '</a>';
        }
      }
      document.getElementById(elementId).innerHTML = h;
    } else {
      getUpdateStatusX(elementId, npUri, apiUrls);
    }
  };
  r.onerror = function(error) {
    getUpdateStatusX(elementId, npUri, apiUrls);
  }
  r.send();
}
