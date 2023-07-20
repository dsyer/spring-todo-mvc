package example.todomvc.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.reactive.server.WebTestClient;

import example.todomvc.Todo;
import example.todomvc.Todos;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TodoControllerTests {

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
    void testIndex() throws Exception {
        this.webClient.get().uri("/").exchange().expectStatus().isOk()
                .expectBody(String.class).value(value -> assertThat(value).contains("Completed"));
    }

}