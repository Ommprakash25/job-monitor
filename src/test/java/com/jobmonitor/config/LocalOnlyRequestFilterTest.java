package com.jobmonitor.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LocalOnlyRequestFilterTest {

    private final LocalOnlyRequestFilter filter = new LocalOnlyRequestFilter();

    @Test
    void allowsLocalPagesAndForms() throws Exception {
        MockHttpServletRequest post = request("POST", "127.0.0.1");
        post.addHeader("Origin", "http://127.0.0.1:8080");
        assertThat(run(post).getStatus()).isEqualTo(200);
        assertThat(run(request("GET", "localhost")).getStatus()).isEqualTo(200);
    }

    @Test
    void blocksOtherHostNames() throws Exception {
        assertThat(run(request("GET", "evil.example")).getStatus()).isEqualTo(403);
    }

    @Test
    void blocksFormsSentFromOtherSites() throws Exception {
        MockHttpServletRequest post = request("POST", "127.0.0.1");
        post.addHeader("Origin", "https://evil.example");
        assertThat(run(post).getStatus()).isEqualTo(403);

        MockHttpServletRequest noOrigin = request("POST", "127.0.0.1");
        noOrigin.addHeader("Sec-Fetch-Site", "cross-site");
        assertThat(run(noOrigin).getStatus()).isEqualTo(403);
    }

    @Test
    void allowsReadingEvenWhenLinkedFromElsewhere() throws Exception {
        MockHttpServletRequest get = request("GET", "127.0.0.1");
        get.addHeader("Sec-Fetch-Site", "cross-site");
        assertThat(run(get).getStatus()).isEqualTo(200);
    }

    private static MockHttpServletRequest request(String method, String host) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/master/profile");
        request.setServerName(host);
        request.setServerPort(8080);
        return request;
    }

    private MockHttpServletResponse run(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
