package com.yavanika.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UrlControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void shortenRedirectAndCountClicks() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/page\",\"customAlias\":\"demo1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("demo1"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/demo1"));

        mvc.perform(get("/demo1").header("Referer", "https://linkedin.com"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/page"));
        mvc.perform(get("/demo1")).andExpect(status().isFound());

        mvc.perform(get("/api/urls/demo1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(2))
                .andExpect(jsonPath("$.topReferrers['https://linkedin.com']").value(1))
                .andExpect(jsonPath("$.topReferrers.direct").value(1));
    }

    @Test
    void generatesBase62CodeWhenNoAlias() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"github.com/yavanika07\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", matchesPattern("^[0-9a-zA-Z]+$")))
                .andExpect(jsonPath("$.originalUrl").value("https://github.com/yavanika07"));
    }

    @Test
    void duplicateAliasReturnsConflict() throws Exception {
        String body = "{\"url\":\"https://example.com\",\"customAlias\":\"taken\"}";
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidInputReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"notaurl\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCodeReturnsNotFound() throws Exception {
        mvc.perform(get("/doesNotExist")).andExpect(status().isNotFound());
    }
}
