package example.todomvc.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import example.todomvc.web.TemplateModel.TodoDto;
import freemarker.template.Configuration;

@SpringBootTest
public class FragmentTests {

	@Autowired
	private Configuration configuration;

	@Test
	void testIndex() throws Exception {
		TodoDto todo = new TodoDto(UUID.randomUUID(), "Something", false);
		Map<String,Object> model = Map.of("todos", Arrays.asList(todo), "action", "");
		StringWriter out = new StringWriter();
		configuration.getTemplate("todos.ftlh").process(model, out);
		assertThat(out.toString()).contains(todo.id().toString());
		assertThat(out.toString()).doesNotContain("hx-swap-oob");
	}
	
	@Test
	void testTodo() throws Exception {
		TodoDto todo = new TodoDto(UUID.randomUUID(), "Something", false);
		Map<String,Object> model = Map.of("todo", todo, "action", "");
		StringWriter out = new StringWriter();
		configuration.getTemplate("todo.ftlh").process(model, out);
		assertThat(out.toString()).contains(todo.id().toString());
		assertThat(out.toString()).doesNotContain("hx-swap-oob");
	}
	
	@Test
	void testUpdate() throws Exception {
		TodoDto todo = new TodoDto(UUID.randomUUID(), "Something", false);
		Map<String,Object> model = Map.of("todo", todo);
		StringWriter out = new StringWriter();
		configuration.getTemplate("update-todo.ftlh").process(model, out);
		assertThat(out.toString()).contains(todo.id().toString());
		assertThat(out.toString()).contains("hx-swap-oob=\"true\"");
	}

	@Test
	void testRemove() throws Exception {
		TodoDto todo = new TodoDto(UUID.randomUUID(), "Something", false);
		Map<String,Object> model = Map.of("todo", todo);
		StringWriter out = new StringWriter();
		configuration.getTemplate("remove-todo.ftlh").process(model, out);
		// System.err.println(out.toString());
		assertThat(out.toString()).contains(todo.id().toString());
		assertThat(out.toString()).contains("hx-swap-oob=\"true\"");
	}
}
