(function(global) {
  if (global.ReabbleUserMenu) return;

  var MENU_ITEM_KEY_ATTR = "data-reabble-menu-key";
  var MENU_MANAGED_ATTR = "data-reabble-menu-managed";
  var SETTINGS_GEAR_PATH_PREFIX = "M15.95 10.78";

  var menuUpdateInProgress = false;
  var menuObserver = null;
  var clickListenerAttached = false;
  var configuredItems = [];

  function getNodeText(node) {
    return ((node && node.textContent) || "").trim();
  }

  function normalizeItems(items) {
    if (!Array.isArray(items)) return [];
    var normalized = [];
    for (var i = 0; i < items.length; i++) {
      var item = items[i] || {};
      if (!item.key || !item.label) continue;
      normalized.push({
        key: String(item.key),
        label: String(item.label),
        url: item.url ? String(item.url) : null
      });
    }
    return normalized;
  }

  var SIGN_OUT_LABELS = ["Sign out", "登出", "Log out", "Logout", "退出登录"];

  function findSignOutItem(root) {
    var allDivs = root.getElementsByTagName("div");
    for (var i = 0; i < allDivs.length; i++) {
      var txt = getNodeText(allDivs[i]);
      for (var s = 0; s < SIGN_OUT_LABELS.length; s++) {
        if (txt === SIGN_OUT_LABELS[s]) {
          return allDivs[i];
        }
      }
    }
    return null;
  }

  function setItemLabel(menuItem, label) {
    var spans = menuItem.getElementsByTagName("span");
    var labelSpan = null;
    for (var i = 0; i < spans.length; i++) {
      var sText = getNodeText(spans[i]);
      if (sText) labelSpan = spans[i];
    }
    if (labelSpan) {
      labelSpan.textContent = label;
    } else {
      menuItem.textContent = label;
    }
  }

  function isLegacyRegionMenuLabel(txt) {
    return txt === "Region..." || txt === "Region…";
  }

  function removeLegacyRegionDuplicates(menuContainer) {
    var children = Array.prototype.slice.call(menuContainer.children);
    var foundRegion = false;
    for (var i = 0; i < children.length; i++) {
      var txt = getNodeText(children[i]);
      if (!isLegacyRegionMenuLabel(txt)) continue;
      if (!foundRegion) {
        foundRegion = true;
      } else {
        menuContainer.removeChild(children[i]);
      }
    }
  }

  function ensureManagedMenuItems() {
    if (menuUpdateInProgress) return;
    menuUpdateInProgress = true;
    try {
      var root = document.getElementById("portal-root");
      if (!root) return false;

      var signOutItem = findSignOutItem(root);
      if (!signOutItem) return false;

      var menuContainer = signOutItem.parentElement;
      if (!menuContainer) return false;

      removeLegacyRegionDuplicates(menuContainer);

      var desiredByKey = {};
      for (var i = 0; i < configuredItems.length; i++) {
        desiredByKey[configuredItems[i].key] = configuredItems[i];
      }

      var managedNodes = Array.prototype.slice.call(
        menuContainer.querySelectorAll("[" + MENU_MANAGED_ATTR + "='1']")
      );
      var managedByKey = {};
      for (var j = 0; j < managedNodes.length; j++) {
        var key = managedNodes[j].getAttribute(MENU_ITEM_KEY_ATTR) || "";
        if (!desiredByKey[key]) {
          menuContainer.removeChild(managedNodes[j]);
          continue;
        }
        if (!managedByKey[key]) {
          managedByKey[key] = managedNodes[j];
        } else {
          menuContainer.removeChild(managedNodes[j]);
        }
      }

      for (var k = configuredItems.length - 1; k >= 0; k--) {
        var item = configuredItems[k];
        var menuItem = managedByKey[item.key];
        if (!menuItem) {
          menuItem = signOutItem.cloneNode(true);
        }

        menuItem.setAttribute(MENU_MANAGED_ATTR, "1");
        menuItem.setAttribute(MENU_ITEM_KEY_ATTR, item.key);
        setItemLabel(menuItem, item.label);

        menuItem.onclick = (function(targetUrl) {
          return function() {
            if (targetUrl) {
              window.location.href = targetUrl;
            }
          };
        })(item.url);

        menuContainer.insertBefore(menuItem, signOutItem);
      }
      return true;
    } finally {
      menuUpdateInProgress = false;
    }
  }

  function startObservingMenuChanges() {
    if (menuObserver) return;
    if (typeof MutationObserver === "undefined") {
      // 老环境兜底：直接尝试一次即可，不做轮询。
      ensureManagedMenuItems();
      return;
    }

    menuObserver = new MutationObserver(function() {
      if (configuredItems.length === 0) return;
      var resolved = ensureManagedMenuItems();
      if (resolved) {
        menuObserver.disconnect();
        menuObserver = null;
      }
    });

    var target = document.getElementById("portal-root") || document.body || document.documentElement;
    if (!target) {
      menuObserver = null;
      return;
    }

    menuObserver.observe(target, { childList: true, subtree: true });
  }

  function isSettingsButton(button) {
    if (!button) return false;
    var path = button.querySelector("svg path[d*='" + SETTINGS_GEAR_PATH_PREFIX + "']");
    return !!path;
  }

  function attachClickListener() {
    if (clickListenerAttached) return;
    clickListenerAttached = true;
    document.addEventListener("click", function(event) {
      if (configuredItems.length === 0) return;
      var node = event.target;
      var button = node && node.closest ? node.closest("button") : null;
      if (!button) return;
      if (!isSettingsButton(button)) return;
      // 用户点击设置按钮时，优先直接尝试插入；若此时菜单结构尚未渲染，再依赖 MutationObserver 监听 DOM 变更。
      if (!ensureManagedMenuItems()) {
        startObservingMenuChanges();
      }
    }, true);
  }

  function revalidateSoon() {
    // 先尝试一次；若菜单尚未渲染则依赖 DOM 变更事件再尝试。
    if (!ensureManagedMenuItems()) {
      startObservingMenuChanges();
    }
  }

  global.ReabbleUserMenu = {
    configure: function(items) {
      configuredItems = normalizeItems(items);
      attachClickListener();
      revalidateSoon();
    },
    setItems: function(items) {
      this.configure(items);
    },
    addItem: function(item) {
      var appended = configuredItems.slice();
      appended.push(item);
      configuredItems = normalizeItems(appended);
      revalidateSoon();
    },
    ensureNow: ensureManagedMenuItems
  };
})(window);
