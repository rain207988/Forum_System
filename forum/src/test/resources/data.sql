insert into t_user (id, username, password, nickname, phoneNum, email, gender, salt, avatarUrl, articleCount, isAdmin, remark, state, deleteState, createTime, updateTime)
values
    (1, 'admin', '$2a$10$7Y.hwwN5r3JFAJt7w0o2LuowJ0kQSg4mFoztEzh0ElBTtPlqvGjYy', 'admin-user', '13800000000', 'admin@example.com', 1, null, 'https://example.com/admin.png', 1, 1, 'forum-admin', 0, 0, current_timestamp, current_timestamp),
    (2, 'alice', '$2a$10$7Y.hwwN5r3JFAJt7w0o2LuowJ0kQSg4mFoztEzh0ElBTtPlqvGjYy', 'Alice', '13900000000', 'alice@example.com', 2, null, 'https://example.com/alice.png', 1, 0, 'tech-sharer', 0, 0, current_timestamp, current_timestamp),
    (3, 'bob', '$2a$10$7Y.hwwN5r3JFAJt7w0o2LuowJ0kQSg4mFoztEzh0ElBTtPlqvGjYy', 'Bob', '13700000000', 'bob@example.com', 1, null, 'https://example.com/bob.png', 0, 0, 'backend-dev', 0, 0, current_timestamp, current_timestamp);

insert into t_board (id, name, articleCount, sort, state, deleteState, createTime, updateTime)
values
    (1, 'Java', 1, 1, 0, 0, current_timestamp, current_timestamp),
    (2, 'Frontend', 1, 2, 0, 0, current_timestamp, current_timestamp),
    (3, 'Experience', 0, 3, 0, 0, current_timestamp, current_timestamp);

insert into t_article (id, boardId, userId, title, visitCount, replyCount, likeCount, state, deleteState, createTime, updateTime, content)
values
    (1, 1, 1, 'Spring Boot forum launch checklist', 12, 1, 3, 0, 0, current_timestamp, current_timestamp, 'This post collects a launch checklist for the forum project.'),
    (2, 2, 2, 'Markdown and rich text integration', 8, 0, 2, 0, 0, current_timestamp, current_timestamp, 'Sharing some notes about editor integration and rendering.');

insert into t_article_reply (id, articleId, postUserId, replyId, replyUserId, content, likeCount, state, deleteState, createTime, updateTime)
values
    (1, 1, 2, null, null, 'This checklist is very helpful. Thanks for sharing.', 0, 0, 0, current_timestamp, current_timestamp);

insert into t_message (id, postUserId, receiveUserId, content, state, deleteState, createTime, updateTime)
values
    (1, 2, 1, 'Hi admin, I want to apply for a new board.', 0, 0, current_timestamp, current_timestamp),
    (2, 1, 2, 'Please submit the detailed request first.', 1, 0, current_timestamp, current_timestamp);
