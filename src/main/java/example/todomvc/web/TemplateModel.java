/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.todomvc.web;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.TypedSort;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import example.todomvc.Todo;
import example.todomvc.Todos;
import io.jstach.jstache.JStache;
import jakarta.validation.constraints.NotBlank;

/**
 * Helper component to prepare {@link Model} instances to render a template.
 * Also converts form data into domain objects.
 *
 * @author Oliver Drotbohm
 */
@Component
class TemplateModel {

	private static final Sort DEFAULT_SORT = TypedSort.sort(Todo.class).by(Todo::getCreated);

	private final Todos todos;

	public TemplateModel(Todos todos) {
		this.todos = todos;
	}

	List<TodoDto> prepareForm(Optional<String> filter) {
		return prepareTodos(filter);
	}

	TodoDto save(Form form) {
		return new TodoDto(todos.save(new Todo(form.getTitle())));
	}

	Todo save(Todo todo) {
		return todos.save(todo);
	}

	TodoDto save(Todo todo, Optional<String> filter) {
		var result = new TodoDto(save(todo));
		return result;
	}

	void saveForm(Form form) {
		save(form);
	}

	TodoDto delete(Todo todo) {
		todos.delete(todo);
		return new TodoDto(todo);
	}

	void deleteCompletedTodos() {
		todos.findByCompleted(true, Sort.unsorted()).forEach(todos::delete);
	}

	List<TodoDto> prepareTodos(Optional<String> filter) {
		return todos(filter).map(it -> new TodoDto(it.getId(), it.getTitle(), it.isCompleted())).toList();
	}

	Foot createFoot(Optional<String> filter) {
		return new Foot(todos.findByCompleted(false, DEFAULT_SORT).toList().size(),
				todos.findAll(DEFAULT_SORT).toList().size(), filter);
	}

	private Streamable<Todo> todos(Optional<String> filter) {

		// Needed due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=576093
		var defaulted = filter.orElse("");

		return switch (defaulted) {
			case "active" -> todos.findByCompleted(false, DEFAULT_SORT);
			case "completed" -> todos.findByCompleted(true, DEFAULT_SORT);
			default -> todos.findAll(DEFAULT_SORT);
		};
	}

	public static class Form {
		@NotBlank String title;
		String action;

		Form(String title) {
			this.title = title;
		}

		public Form() {
			this("");
		}

		Todo toEntity() {
			return new Todo(title);
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}
	}

	public Page preparePage(Optional<String> filter) {
		return new Page(new TodosDto(prepareTodos(filter), null), createFoot(filter), new Form());
	}

	@JStache(path = "index")
	public record Page(TodosDto todos, Foot foot, Form form) {
	}

	@JStache(path = "todo")
	public record TodoDto(UUID id, String title, boolean completed) {
		TodoDto(Todo todo) {
			this(todo.getId(), todo.getTitle(), todo.isCompleted());
		}
	}

	@JStache(path = "foot")
	public record Foot(int numberOfIncomplete, int numberOfTodos, Optional<String> filter) {
		boolean filterAll() {
			return filter.isEmpty() || filter.get().isEmpty();
		}

		boolean filterActive() {
			return filter.isPresent() && "active".equals(filter.get());
		}

		boolean filterCompleted() {
			return filter.isPresent() && "completed".equals(filter.get());
		}
	}

	@JStache(path = "todos")
	public record TodosDto(List<TodoDto> todos, String action) {
		TodosDto(List<TodoDto> todos) {
			this(todos, null);
		}
	}

	@JStache(path = "remove-todo")
	public record RemoveTodo(UUID id) {
	}

	@JStache(path = "new-todo")
	public record NewTodo(String title) {
		NewTodo() {
			this("");
		}
	}

	@JStache(path = "update-todo")
	public record UpdateTodo(TodoDto todo) {
		UUID id() {
			return todo.id();
		}

		String title() {
			return todo.title();
		}

		boolean completed() {
			return todo.completed();
		}
	}

	public Todo find(UUID id) {
		return todos.findById(id).orElseGet(() -> {
			Todo todo = new Todo("Not found");
			return todo;
		});
	}

}
