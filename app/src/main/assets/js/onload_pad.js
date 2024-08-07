(function() {
  var refreshBtn = '<button onclick="javascript: window.location.reload();" type="button" class="bRefresh js-51-26 js-52-27 js-53-28 js-54-29 js-29-17 js-48-22 js-7-1l js-40-1m js-6-1a js-30-1b js-31-1c js-32-1d js-33-1e js-8-8 js-34-1f js-35-1g js-36-1h js-37-1i js-38-1j js-39-1k   js-16-l js-41-1n js-42-1o js-43-1p js-44-1q  js-45-1r"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-refresh-cw"><polyline points="23 4 23 10 17 10"></polyline><polyline points="1 20 1 14 7 14"></polyline><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path></svg></button>';

  var intervalId = setInterval(function(){
    if (document.getElementsByClassName("bRefresh").length < 1) {
      var target = document.getElementsByClassName("js-12-f js-13-2t")[2];
      if (target) {
        target.insertAdjacentHTML("afterend", refreshBtn);
        clearInterval(intervalId);
      }
    }
  }, 2000);

  setTimeout(function(){
    clearInterval(intervalId);
  }, 10000);

  const styleSheets = document.styleSheets[document.styleSheets.length - 1];
  styleSheets.addRule(".js-49-3e", "border:none;");
  styleSheets.addRule(".js-29-44", "border-left-style: none;border-bottom:1px dashed #000000;");
  styleSheets.addRule(".js-68-3z", "border-left-style: none;border-bottom:1px dashed #000000;");

  styleSheets.addRule(".js-26-12 .js-24-z .js-5-14 .js-9-9 .js-28-15 .js-14-16 .js-29-17 .js-6-18", "border:12px;");
  document.getElementsByClassName("app-loading")[0].innerHTML="LOADING";
})();

function tryInsertRule(attempts = 0, maxAttempts = 5) {
  if (attempts >= maxAttempts) {
    return;
  }
  if (document.styleSheets.length > 0 && document.styleSheets[0].cssRules) {
    try {
      document.styleSheets[0].insertRule('center{ display: none }',0);
    } catch (e) {
      setTimeout(tryInsertRule, 2000, attempts + 1);
    }
  } else {
    setTimeout(tryInsertRule, 2000, attempts + 1);
  }
}
tryInsertRule();