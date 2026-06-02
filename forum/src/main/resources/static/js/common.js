// =================== 定义全局变量 ======================
let avatarUrl = 'image/avatar01.jpeg'; // 默认头像
let currentArticle; // 当前访问的帖子
let currentUserId;  // 当前登录用户
let profileUserId;  // 查看个人信息
let articleKeyword = ''; // 搜索关键字
const FORUM_TOKEN_KEY = 'forum_access_token';
const FORUM_TOKEN_TYPE_KEY = 'forum_token_type';

function getForumToken() {
  return window.localStorage.getItem(FORUM_TOKEN_KEY);
}

function getForumTokenType() {
  return window.localStorage.getItem(FORUM_TOKEN_TYPE_KEY) || 'Bearer';
}

function storeForumAuth(authData) {
  if (!authData || !authData.token) {
    return;
  }
  window.localStorage.setItem(FORUM_TOKEN_KEY, authData.token);
  window.localStorage.setItem(FORUM_TOKEN_TYPE_KEY, authData.tokenType || 'Bearer');
}

function clearForumAuth() {
  window.localStorage.removeItem(FORUM_TOKEN_KEY);
  window.localStorage.removeItem(FORUM_TOKEN_TYPE_KEY);
}

function isAuthPage() {
  const path = window.location.pathname || '';
  return path.endsWith('/sign-in.html')
    || path.endsWith('/sign-up.html')
    || path.endsWith('sign-in.html')
    || path.endsWith('sign-up.html');
}

function redirectToLogin() {
  if (isAuthPage()) {
    return;
  }
  clearForumAuth();
  window.location.assign('/sign-in.html');
}

function initForumAjaxAuth() {
  if (typeof $ === 'undefined' || $.forumAjaxAuthInitialized) {
    return;
  }

  $.ajaxSetup({
    beforeSend: function(xhr) {
      const token = getForumToken();
      if (!token) {
        return;
      }
      xhr.setRequestHeader('Authorization', getForumTokenType() + ' ' + token);
    }
  });

  $(document).ajaxError(function(event, xhr) {
    if (xhr && xhr.status === 401) {
      redirectToLogin();
    }
  });

  $.forumAjaxAuthInitialized = true;
}

initForumAjaxAuth();

if (!isAuthPage() && !getForumToken()) {
  redirectToLogin();
}


// ============================ 处理导航激活效果 ===========================
function changeNavActive (boardItem) {
    // 判断当前是否为激活状态
    if (boardItem.hasClass('active') == false) {
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
  console.log('发送请求查询帖子列表');
  $('#bit-forum-content').load('article_list.html');
}

// 设置站内信接收用户信息
function setMessageReceiveUserInfo (userId, nickname) {
  console.log('userId = ' + userId);
  console.log('nickname = ' + nickname);
  $('#index_message_receive_user_id').val(userId);
  $('#index_message_receive_user_name').html('发送给: <strong>' + nickname + '</strong>');
  console.log('value = ' + $('#index_message_receive_user_id').val());
}
