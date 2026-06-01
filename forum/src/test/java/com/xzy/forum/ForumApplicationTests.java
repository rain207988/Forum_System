package com.xzy.forum;

import com.xzy.forum.dao.UserMapper;
import com.xzy.forum.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ForumApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserMapper userMapper;

    @Test
    void contextLoadsWithTestDataSource() throws Exception {
        assertThat(dataSource).isNotNull();
        assertThat(dataSource.getConnection().isClosed()).isFalse();
    }

    @Test
    void shouldQuerySeedUser() {
        User user = userMapper.selectByPrimaryKey(1L);
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getIsAdmin()).isEqualTo((byte) 1);
    }
}
