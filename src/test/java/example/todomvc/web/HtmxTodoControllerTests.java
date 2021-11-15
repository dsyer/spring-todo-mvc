package example.todomvc.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.reactive.server.WebTestClient;

import example.todomvc.Todo;
import example.todomvc.Todos;

@SpringBootTest
@AutoConfigureWebTestClient
class HtmxTodoControllerTests {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private Todos todos;

    private Todo todo = new Todo("Incomplete");

    @BeforeEach
    void init() {
        todos.findAll(Sort.unsorted()).forEach(todo -> todos.delete(todo));
        todos.save(new Todo("Completed").toggleCompletion());
        todos.save(todo);
    }

    @Test
    void testToggle() throws Exception {
        this.webClient.put().uri("/{id}/toggle", todo.getId()).header("HX-Request", "true").exchange().expectStatus()
                .isOk().expectBody(String.class).value(value -> {
                    assertThat(value).contains("<li");
                    assertThat(value).contains("hx-swap-oob=\"true\"");
                    assertThat(value).contains("Incomplete");
                    assertThat(value).contains("class=\"completed\"");
                    assertThat(value).contains("id=\"foot\"");
                });
    }

    @Test
    void testDelete() throws Exception {
        this.webClient.delete().uri("/{id}", todo.getId()).header("HX-Request", "true").exchange().expectStatus()
                .isOk().expectBody(String.class).value(value -> {
                    assertThat(value).contains("id=\"todo-" + todo.getId() + "\"");
                    assertThat(value).contains("<li hx-swap-oob=\"true\"");
                    assertThat(value).contains("></li>"); // empty list item
                    assertThat(value).contains("id=\"foot\"");
                });
    }

    @Test
    void testCreate() throws Exception {
        this.webClient.post().uri("/").bodyValue("title=Foo").header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8").header("HX-Request", "true")
        .exchange()
        .expectStatus()
                .isOk().expectBody(String.class).value(value -> {
                    assertThat(value).contains("<form id=\"new-todo\"");
                    assertThat(value).doesNotContain("htmx-swap-oob");
                    assertThat(value).contains("value=\"\"");
                    assertThat(value).contains("<ul id=\"todos\" class=\"todo-list\" hx-swap-oob=\"beforeend\">");
                    assertThat(value).contains("<label>Foo</label>");
                    assertThat(value).contains("id=\"foot\"");
                });
                assertThat(todos.findAll(Sort.unsorted()).toSet()).hasSize(3);
    }

    @Test
    void testIndex() throws Exception {
        this.webClient.get().uri("/").header("HX-Request", "true").exchange().expectStatus().isOk()
                .expectBody(String.class).value(value -> assertThat(value).contains("Completed"));
    }

}