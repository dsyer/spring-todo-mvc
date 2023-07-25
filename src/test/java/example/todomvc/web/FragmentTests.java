package example.todomvc.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import example.todomvc.web.TemplateModel.RemoveTodoDto;
import example.todomvc.web.TemplateModel.TodoDto;
import example.todomvc.web.TemplateModel.TodoFoot;
import example.todomvc.web.TemplateModel.TodosDto;
import example.todomvc.web.TemplateModel.UpdateTodoDto;
import io.jstach.jstachio.JStachio;

@SpringBootTest
public class FragmentTests {

	@Test
	void testIndex() throws Exception {
		TodoDto todo = new TodoDto(UUID.randomUUID(), "Something", false);
		TodosDto todos = new TodosDto(Arrays.asList(todo));
		StringBuilder out = new StringBuilder();
		JStachio.render(todos, out);
		assertThat(out.toString()).contains(todo.id().toString());
		assertThat(out.toString()).doesNotContain("hx-swap-oob");
	}
	
	@Test
	void testFoot() throws Exception {
		TodoFoot foot = new TodoFoot(12, 3, Optional.of("active"));
		StringBuilder out = new StringBuilder();
		JStachio.render(foot, out);
		System.err.println(out.toString());
		assertThat(out.toString()).contains("hx-swap-oob");
	}
	
	@Test
	void testTodo() throws Exception {
		TodoDto todo = new TodoDto(UUID.randomUUID(), "Something", false);
		StringBuilder out = new StringBuilder();
		JStachio.render(todo, out);
		assertThat(out.toString()).contains(todo.id().toString());
		assertThat(out.toString()).doesNotContain("hx-swap-oob");
	}
	
	@Test
	void testUpdate() throws Exception {
		TodoDto todo = new TodoDto(UUID.randomUUID(), "Something", false);
		StringBuilder out = new StringBuilder();
		JStachio.render(new UpdateTodoDto(todo), out);
		assertThat(out.toString()).contains(todo.id().toString());
		assertThat(out.toString()).contains("hx-swap-oob=\"true\"");
	}

	@Test
	void testRemove() throws Exception {
		TodoDto todo = new TodoDto(UUID.randomUUID(), "Something", false);
		StringBuilder out = new StringBuilder();
		JStachio.render(new RemoveTodoDto(todo.id()), out);
		// System.err.println(out.toString());
		assertThat(out.toString()).contains(todo.id().toString());
		assertThat(out.toString()).contains("hx-swap-oob=\"true\"");
	}
}
