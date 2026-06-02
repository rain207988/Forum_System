// =================== 定义全局变量 ======================
let avatarUrl = 'image/avatar01.jpeg'; // 默认头像
let currentArticle; // 当前访问的帖子
let currentUserId;  // 当前登录用户
let profileUserId;  // 查看个人信息
let articleKeyword = ''; // 搜索关键字
const FORUM_TOKEN_KEY = 'forum_access_token';
const FORUM_REFRESH_TOKEN_KEY = 'forum_refresh_token';
const FORUM_TOKEN_TYPE_KEY = 'forum_token_type';
const FORUM_ACCESS_TOKEN_EXPIRES_AT_KEY = 'forum_access_token_expires_at';
const FORUM_REFRESH_TOKEN_EXPIRES_AT_KEY = 'forum_refresh_token_expires_at';
const FORUM_REFRESH_SKEW_MS = 60 * 1000;

let forumRefreshPromise = null;
let forumRedirectingToLogin = false;
let forumMessageSocket = null;
let forumMessageSocketConnecting = false;
let forumMessageSocketReconnectTimer = null;
let forumMessageSocketManualClose = false;
const forumRealtimeHandlers = [];

function getForumToken() {
  return window.localStorage.getItem(FORUM_TOKEN_KEY);
}

function getForumRefreshToken() {
  return window.localStorage.getItem(FORUM_REFRESH_TOKEN_KEY);
}

function buildForumRefreshTokenPayload() {
  const refreshToken = getForumRefreshToken();
  return refreshToken ? { refreshToken: refreshToken } : {};
}

function getForumTokenType() {
  return window.localStorage.getItem(FORUM_TOKEN_TYPE_KEY) || 'Bearer';
}

function getForumAccessTokenExpiresAt() {
  return parseForumTime(window.localStorage.getItem(FORUM_ACCESS_TOKEN_EXPIRES_AT_KEY));
}

function getForumRefreshTokenExpiresAt() {
  return parseForumTime(window.localStorage.getItem(FORUM_REFRESH_TOKEN_EXPIRES_AT_KEY));
}

function storeForumAuth(authData) {
  if (!authData || !authData.token) {
    return;
  }
  const previousToken = getForumToken();
  window.localStorage.setItem(FORUM_TOKEN_KEY, authData.token);
  if (authData.refreshToken) {
    window.localStorage.setItem(FORUM_REFRESH_TOKEN_KEY, authData.refreshToken);
  }
  window.localStorage.setItem(FORUM_TOKEN_TYPE_KEY, authData.tokenType || 'Bearer');
  if (authData.expiresAt) {
    window.localStorage.setItem(FORUM_ACCESS_TOKEN_EXPIRES_AT_KEY, String(authData.expiresAt));
  }
  if (authData.refreshExpiresAt) {
    window.localStorage.setItem(FORUM_REFRESH_TOKEN_EXPIRES_AT_KEY, String(authData.refreshExpiresAt));
  }
  if (!isAuthPage() && previousToken && previousToken !== authData.token) {
    closeForumMessageSocket();
    connectForumMessageSocket();
  }
}

function clearForumAuth() {
  closeForumMessageSocket();
  window.localStorage.removeItem(FORUM_TOKEN_KEY);
  window.localStorage.removeItem(FORUM_REFRESH_TOKEN_KEY);
  window.localStorage.removeItem(FORUM_TOKEN_TYPE_KEY);
  window.localStorage.removeItem(FORUM_ACCESS_TOKEN_EXPIRES_AT_KEY);
  window.localStorage.removeItem(FORUM_REFRESH_TOKEN_EXPIRES_AT_KEY);
  forumRefreshPromise = null;
}

function isAuthPage() {
  const path = window.location.pathname || '';
  return path.endsWith('/sign-in.html')
    || path.endsWith('/sign-up.html')
    || path.endsWith('sign-in.html')
    || path.endsWith('sign-up.html');
}

function redirectToLogin() {
  if (isAuthPage() || forumRedirectingToLogin) {
    return;
  }
  forumRedirectingToLogin = true;
  clearForumAuth();
  window.location.assign('/sign-in.html');
}

function parseForumTime(rawValue) {
  if (!rawValue) {
    return null;
  }
  if (/^\d+$/.test(rawValue)) {
    return Number(rawValue);
  }
  const parsed = Date.parse(rawValue);
  return Number.isNaN(parsed) ? null : parsed;
}

function buildForumWebSocketUrl(path) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return protocol + '//' + window.location.host + path;
}

function emitForumRealtimeEvent(event) {
  forumRealtimeHandlers.forEach(function(handler) {
    try {
      handler(event);
    } catch (error) {
      console.warn('forum realtime handler failed', error);
    }
  });
}

function onForumRealtimeEvent(handler) {
  if (typeof handler !== 'function') {
    return function noop() {};
  }
  forumRealtimeHandlers.push(handler);
  return function unsubscribe() {
    const index = forumRealtimeHandlers.indexOf(handler);
    if (index >= 0) {
      forumRealtimeHandlers.splice(index, 1);
    }
  };
}

function closeForumMessageSocket() {
  forumMessageSocketManualClose = true;
  if (forumMessageSocketReconnectTimer) {
    window.clearTimeout(forumMessageSocketReconnectTimer);
    forumMessageSocketReconnectTimer = null;
  }
  if (forumMessageSocket) {
    forumMessageSocket.close();
    forumMessageSocket = null;
  }
  forumMessageSocketConnecting = false;
}

function scheduleForumMessageSocketReconnect() {
  if (forumMessageSocketManualClose || forumRedirectingToLogin || isAuthPage()) {
    return;
  }
  if (forumMessageSocketReconnectTimer) {
    return;
  }
  forumMessageSocketReconnectTimer = window.setTimeout(function() {
    forumMessageSocketReconnectTimer = null;
    if (hasUsableRefreshToken()) {
      refreshForumAuth()
        .done(function() {
          connectForumMessageSocket();
        })
        .fail(function() {
          redirectToLogin();
        });
      return;
    }
    connectForumMessageSocket();
  }, 3000);
}

function connectForumMessageSocket() {
  if (isAuthPage()) {
    return;
  }
  if (forumMessageSocketConnecting) {
    return;
  }
  if (forumMessageSocket && (forumMessageSocket.readyState === WebSocket.OPEN || forumMessageSocket.readyState === WebSocket.CONNECTING)) {
    return;
  }

  const token = getForumToken();
  if (!token) {
    if (hasUsableRefreshToken()) {
      refreshForumAuth().done(function() {
        connectForumMessageSocket();
      }).fail(function() {
        redirectToLogin();
      });
    }
    return;
  }

  forumMessageSocketManualClose = false;
  forumMessageSocketConnecting = true;
  const socketUrl = buildForumWebSocketUrl('/ws/messages?token=' + encodeURIComponent(token));
  const socket = new WebSocket(socketUrl);
  forumMessageSocket = socket;

  socket.onopen = function() {
    forumMessageSocketConnecting = false;
    if (forumMessageSocketReconnectTimer) {
      window.clearTimeout(forumMessageSocketReconnectTimer);
      forumMessageSocketReconnectTimer = null;
    }
  };

  socket.onmessage = function(event) {
    try {
      emitForumRealtimeEvent(JSON.parse(event.data));
    } catch (error) {
      console.warn('failed to parse realtime message event', error);
    }
  };

  socket.onerror = function() {
    // reconnect is driven by onclose to avoid duplicate retries
  };

  socket.onclose = function() {
    forumMessageSocketConnecting = false;
    if (forumMessageSocket === socket) {
      forumMessageSocket = null;
    }
    if (!forumMessageSocketManualClose) {
      scheduleForumMessageSocketReconnect();
    }
  };
}

function hasUsableRefreshToken() {
  const refreshToken = getForumRefreshToken();
  if (!refreshToken) {
    return false;
  }
  const refreshExpiresAt = getForumRefreshTokenExpiresAt();
  return refreshExpiresAt === null || refreshExpiresAt > Date.now();
}

function shouldRefreshAccessTokenSoon() {
  if (!hasUsableRefreshToken()) {
    return false;
  }
  const accessToken = getForumToken();
  if (!accessToken) {
    return true;
  }
  const accessExpiresAt = getForumAccessTokenExpiresAt();
  return accessExpiresAt !== null && accessExpiresAt - Date.now() <= FORUM_REFRESH_SKEW_MS;
}

function isRefreshRequestUrl(url) {
  return typeof url === 'string' && url.indexOf('user/refreshToken') !== -1;
}

function isAuthBootstrapRequest(url) {
  if (typeof url !== 'string') {
    return false;
  }
  return url.indexOf('user/login') !== -1
    || url.indexOf('user/register') !== -1
    || isRefreshRequestUrl(url);
}

function normalizeAjaxOptions(urlOrOptions, maybeOptions) {
  if (typeof urlOrOptions === 'string') {
    const normalizedOptions = $.extend(true, {}, maybeOptions || {});
    normalizedOptions.url = urlOrOptions;
    return normalizedOptions;
  }
  return $.extend(true, {}, urlOrOptions || {});
}

function attachForumAuthHeader(xhr) {
  const token = getForumToken();
  if (!token) {
    return;
  }
  xhr.setRequestHeader('Authorization', getForumTokenType().trim() + ' ' + token);
}

function refreshForumAuth() {
  if (!hasUsableRefreshToken()) {
    return $.Deferred().reject().promise();
  }
  if (forumRefreshPromise) {
    return forumRefreshPromise;
  }

  const deferred = $.Deferred();
  forumRefreshPromise = deferred.promise();

  $.forumOriginalAjax({
    type: 'post',
    url: 'user/refreshToken',
    contentType: 'application/x-www-form-urlencoded',
    data: {
      refreshToken: getForumRefreshToken()
    }
  }).done(function(respData) {
    if (respData && respData.code === 0 && respData.data && respData.data.token) {
      storeForumAuth(respData.data);
      deferred.resolve(respData.data);
      return;
    }
    clearForumAuth();
    deferred.reject(respData);
  }).fail(function(xhr, textStatus, errorThrown) {
    clearForumAuth();
    deferred.reject(xhr, textStatus, errorThrown);
  }).always(function() {
    forumRefreshPromise = null;
  });

  return forumRefreshPromise;
}

function initForumAjaxAuth() {
  if (typeof $ === 'undefined' || $.forumAjaxAuthInitialized) {
    return;
  }

  $.forumOriginalAjax = $.ajax.bind($);

  $.ajax = function(urlOrOptions, maybeOptions) {
    const originalOptions = normalizeAjaxOptions(urlOrOptions, maybeOptions);
    if (originalOptions._forumSkipAuthHandling || isRefreshRequestUrl(originalOptions.url)) {
      return $.forumOriginalAjax(originalOptions);
    }

    const deferred = $.Deferred();

    function finalizeSuccess(context, requestOptions, data, textStatus, jqXHR) {
      if (typeof requestOptions.success === 'function') {
        requestOptions.success.call(context, data, textStatus, jqXHR);
      }
      if (typeof requestOptions.complete === 'function') {
        requestOptions.complete.call(context, jqXHR, textStatus);
      }
      deferred.resolveWith(context, [data, textStatus, jqXHR]);
    }

    function finalizeError(context, requestOptions, jqXHR, textStatus, errorThrown) {
      if (typeof requestOptions.error === 'function') {
        requestOptions.error.call(context, jqXHR, textStatus, errorThrown);
      }
      if (typeof requestOptions.complete === 'function') {
        requestOptions.complete.call(context, jqXHR, textStatus);
      }
      deferred.rejectWith(context, [jqXHR, textStatus, errorThrown]);
    }

    function sendRequest(requestOptions, hasRetriedAfterRefresh) {
      const ajaxOptions = $.extend(true, {}, requestOptions);
      const userBeforeSend = ajaxOptions.beforeSend;

      delete ajaxOptions.success;
      delete ajaxOptions.error;
      delete ajaxOptions.complete;

      ajaxOptions.beforeSend = function(xhr, settings) {
        attachForumAuthHeader(xhr);
        if (typeof userBeforeSend === 'function') {
          return userBeforeSend.call(this, xhr, settings);
        }
      };

      ajaxOptions.success = function(data, textStatus, jqXHR) {
        finalizeSuccess(this, requestOptions, data, textStatus, jqXHR);
      };

      ajaxOptions.error = function(jqXHR, textStatus, errorThrown) {
        const canRetryAfterRefresh = !hasRetriedAfterRefresh
          && jqXHR
          && jqXHR.status === 401
          && !isAuthBootstrapRequest(requestOptions.url)
          && hasUsableRefreshToken();

        if (canRetryAfterRefresh) {
          refreshForumAuth().done(function() {
            sendRequest(requestOptions, true);
          }).fail(function() {
            redirectToLogin();
            finalizeError(this, requestOptions, jqXHR, textStatus, errorThrown);
          });
          return;
        }

        if (jqXHR && jqXHR.status === 401 && !isAuthPage()) {
          redirectToLogin();
        }
        finalizeError(this, requestOptions, jqXHR, textStatus, errorThrown);
      };

      $.forumOriginalAjax(ajaxOptions);
    }

    if (!isAuthBootstrapRequest(originalOptions.url) && shouldRefreshAccessTokenSoon()) {
      refreshForumAuth().done(function() {
        sendRequest(originalOptions, false);
      }).fail(function() {
        redirectToLogin();
        finalizeError(window, originalOptions, null, 'error', 'refresh_failed');
      });
    } else {
      sendRequest(originalOptions, false);
    }

    return deferred.promise();
  };

  $.forumAjaxAuthInitialized = true;
}

initForumAjaxAuth();

if (!isAuthPage() && !getForumToken() && !hasUsableRefreshToken()) {
  redirectToLogin();
}


// ============================ 处理导航激活效果 ===========================
function changeNavActive (boardItem) {
    // 判断当前是否为激活状态
    if (!boardItem.hasClass('active')) {
      let activeLiEl = $('#topBoardList>.active');
      activeLiEl.removeClass('active');
      boardItem.addClass('active');
      // 请求版块中的帖子
      buildArticleList();
    }
}

// ============================ 删除导航激活效果 ===========================
function removeNavActive () {
    // 判断当前是否为激活状态
    let activeLiEl = $('#topBoardList>.active');
    if (activeLiEl) {
      activeLiEl.removeClass('active');
    }
}

//======================= 处理导航栏点击并获取帖子列表 ======================
function buildArticleList (){
  $('#bit-forum-content').load('article_list.html');
}

// 设置站内信接收用户信息
function setMessageReceiveUserInfo (userId, nickname) {
  $('#index_message_receive_user_id').val(userId);
  $('#index_message_receive_user_name').html('发送给: <strong>' + nickname + '</strong>');
}
