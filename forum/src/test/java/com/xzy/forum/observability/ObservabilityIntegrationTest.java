package com.xzy.forum.observability;

import com.xzy.forum.filter.TraceIdFilter;
import com.xzy.forum.services.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ObservabilityIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private IUserService userService;

    @Autowired
    private TraceIdFilter traceIdFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(traceIdFilter)
                .build();
    }

    @Test
    void shouldReturnTraceIdHeaderAndWriteControllerAccessLog(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test/hello")
                        .header("X-Trace-Id", "trace-test-001")
                        .param("name", "codex"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "trace-test-001"));

        assertThat(output.getOut())
                .contains("[controller-access]")
                .contains("traceId=trace-test-001")
                .contains("path=/test/hello");
    }

    @Test
    void shouldWriteServiceErrorLogWithMaskedSensitiveData(CapturedOutput output) {
        try {
            userService.changePassword(1L, "wrongOldPassword", "newPassword123", "newPassword123");
        } catch (Exception ignored) {
            // ignore expected business exception
        }

        assertThat(output.getOut())
                .contains("[service-error]")
                .contains("changePassword")
                .contains("oldPassword=***")
                .contains("newPassword=***")
                .contains("passwordRepeat=***");
    }
}
