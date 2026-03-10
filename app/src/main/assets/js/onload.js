(function() {

  const styleSheets = document.styleSheets[document.styleSheets.length - 1];
  styleSheets.addRule(".js-49-3e", "border:none;");
  styleSheets.addRule(".js-29-44", "border-left-style: none;border-bottom:1px dashed #aaaaaa;");
  styleSheets.addRule(".js-26-12 .js-24-z .js-5-14 .js-9-9 .js-28-15 .js-14-16 .js-29-17 .js-6-18", "border:12px;");
  document.getElementsByClassName("app-loading")[0].innerHTML="LOADING";
  var menuItems = [
    { key: "region", label: "Region...", url: "reabble://choose-region" }
    // 以后新增菜单时，继续在这里追加：
    // { key: "about", label: "About...", url: "reabble://about" }
  ];
  if (window.ReabbleUserMenu) {
    window.ReabbleUserMenu.setItems(menuItems);
    // 也可以按需追加：window.ReabbleUserMenu.addItem({ key: "about", label: "About...", url: "reabble://about" });
  }
})();