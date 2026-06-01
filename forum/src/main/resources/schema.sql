drop table if exists t_message;
drop table if exists t_article_reply;
drop table if exists t_article;
drop table if exists t_board;
drop table if exists t_user;

create table t_user (
    id bigint auto_increment primary key,
    username varchar(64) not null unique,
    password varchar(255) not null,
    nickname varchar(64) not null,
    phoneNum varchar(32),
    email varchar(128),
    gender tinyint default 0,
    salt varchar(64),
    avatarUrl varchar(255),
    articleCount int default 0,
    isAdmin tinyint default 0,
    remark varchar(500),
    state tinyint default 0,
    deleteState tinyint default 0,
    createTime timestamp,
    updateTime timestamp
);

create table t_board (
    id bigint auto_increment primary key,
    name varchar(64) not null,
    articleCount int default 0,
    sort int default 0,
    state tinyint default 0,
    deleteState tinyint default 0,
    createTime timestamp,
    updateTime timestamp
);

create table t_article (
    id bigint auto_increment primary key,
    boardId bigint not null,
    userId bigint not null,
    title varchar(255) not null,
    visitCount int default 0,
    replyCount int default 0,
    likeCount int default 0,
    state tinyint default 0,
    deleteState tinyint default 0,
    createTime timestamp,
    updateTime timestamp,
    content clob,
    constraint fk_article_board foreign key (boardId) references t_board(id),
    constraint fk_article_user foreign key (userId) references t_user(id)
);

create table t_article_reply (
    id bigint auto_increment primary key,
    articleId bigint not null,
    postUserId bigint not null,
    replyId bigint,
    replyUserId bigint,
    content varchar(2000) not null,
    likeCount int default 0,
    state tinyint default 0,
    deleteState tinyint default 0,
    createTime timestamp,
    updateTime timestamp,
    constraint fk_reply_article foreign key (articleId) references t_article(id),
    constraint fk_reply_user foreign key (postUserId) references t_user(id)
);

create table t_message (
    id bigint auto_increment primary key,
    postUserId bigint not null,
    receiveUserId bigint not null,
    content varchar(2000) not null,
    state tinyint default 0,
    deleteState tinyint default 0,
    createTime timestamp,
    updateTime timestamp,
    constraint fk_message_post_user foreign key (postUserId) references t_user(id),
    constraint fk_message_receive_user foreign key (receiveUserId) references t_user(id)
);
