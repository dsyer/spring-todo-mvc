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

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.TypedSort;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import example.todomvc.Todo;
import example.todomvc.Todos;
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

	void prepareForm(Model model, Optional<String> filter) {

		model.addAttribute("form", new TodoForm(""));

		prepareTodos(model, filter);
	}

	TodoDto save(TodoForm form) {
		return new TodoDto(todos.save(new Todo(form.title())));
	}

	Todo save(Todo todo) {
		return todos.save(todo);
	}

	TodoDto save(Todo todo, Model model, Optional<String> filter) {
		var result = new TodoDto(save(todo));
		prepareReferenceData(model, filter);
		model.addAttribute("todo", result);

		return result;
	}

	void saveForm(TodoForm form, Model model, Optional<String> filter) {

		var todo = save(form);

		model.addAttribute("todos", Arrays.asList(todo));
		model.addAttribute("todo", todo);
		prepareReferenceData(model, filter);
	}

	void delete(Todo todo) {
		todos.delete(todo);
	}

	void delete(Todo todo, Model model, Optional<String> filter) {
		todos.delete(todo);
		prepareReferenceData(todo, model, filter);
	}

	void deleteCompletedTodos() {
		todos.findByCompleted(true, Sort.unsorted()).forEach(todos::delete);
	}

	void prepareTodos(Model model, Optional<String> filter) {

		model.addAttribute("todos",
				todos(filter).map(it -> new TodoDto(it.getId(), it.getTitle(), it.isCompleted())).toList());

		prepareReferenceData(model, filter);
	}

	void prepareReferenceData(Todo todo, Model model, Optional<String> filter) {

		model.addAttribute("todo", new TodoDto(todo.getId(), todo.getTitle(), todo.isCompleted()));

		prepareReferenceData(model, filter);
	}

	void prepareReferenceData(Model model, Optional<String> filter) {

		model.addAttribute("filter", filter.orElse(""));
		model.addAttribute("numberOfIncomplete", todos.findByCompleted(false, DEFAULT_SORT).toList().size());
		model.addAttribute("numberOfTodos", todos.findAll(DEFAULT_SORT).toList().size());
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

	public record TodoForm(@NotBlank String title) {

		Todo toEntity() {
			return new Todo(title);
		}
	}

	public record TodoDto(UUID id, String title, boolean completed) {
		TodoDto(Todo todo) {
			this(todo.getId(), todo.getTitle(), todo.isCompleted());
		}
	}

	public Todo find(UUID id) {
		return todos.findById(id).orElseGet(() -> {
			Todo todo = new Todo("Not found");
			return todo;
		});
	}

}
