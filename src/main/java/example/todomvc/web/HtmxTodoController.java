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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;

import example.todomvc.Todo;
import example.todomvc.web.TemplateModel.NewTodo;
import example.todomvc.web.TemplateModel.RemoveTodo;
import example.todomvc.web.TemplateModel.TodoDto;
import example.todomvc.web.TemplateModel.Form;
import example.todomvc.web.TemplateModel.TodosDto;
import example.todomvc.web.TemplateModel.UpdateTodo;
import io.jstach.opt.spring.webmvc.JStachioModelView;
import jakarta.validation.Valid;

@Profile("htmx")
@Controller
@RequestMapping(headers = "HX-Request=true")
class HtmxTodoController {

	private final TemplateModel template;

	public HtmxTodoController(TemplateModel template) {
		this.template = template;
	}

	@GetMapping("/")
	List<View> htmxIndex(@RequestParam Optional<String> filter) {
		return List.of(JStachioModelView.of(new TodosDto(template.prepareForm(filter), "true")),
				JStachioModelView.of(template.createFoot(filter)));
	}

	/**
	 * An optimized variant of {@link #createTodo(TodoItemFormData)}. We perform the
	 * normal insert and then return two {@link HtmxPartials} for the parts of the
	 * page that need updates by rendering the corresponding fragments of the
	 * template.
	 *
	 * @param form
	 * @param model
	 * @return
	 */
	@PostMapping("/")
	List<View> htmxCreateTodo(@Valid Form form, @RequestParam Optional<String> filter) {

		template.saveForm(form);

		return List.of(JStachioModelView.of(new NewTodo()),
				JStachioModelView.of(new TodosDto(template.prepareTodos(filter), "beforeend")),
				JStachioModelView.of(template.createFoot(filter)));
	}

	@PutMapping("/{id}/toggle")
	List<View> htmxToggleCompletion(@PathVariable UUID id, @RequestParam Optional<String> filter) {

		Todo todo = template.find(id);
		TodoDto result = template.save(todo.toggleCompletion(), filter);

		List<View> list = new ArrayList<>(filter
				.map(it -> it.equals("active") && todo.isCompleted() || it.equals("inactive") && !todo.isCompleted()
						? List.of(JStachioModelView.of(new RemoveTodo(id)))
						: List.of(JStachioModelView.of(new UpdateTodo(result))))
				.orElse(List.of(JStachioModelView.of(new UpdateTodo(result)))));
		list.add(JStachioModelView.of(template.createFoot(filter)));
		return list;
	}

	@DeleteMapping("/{id}")
	List<View> htmxDeleteTodo(@PathVariable UUID id, @RequestParam Optional<String> filter) {

		Todo todo = template.find(id);
		template.delete(todo);

		return List.of(JStachioModelView.of(new RemoveTodo(id)),
				JStachioModelView.of(template.createFoot(filter)));
	}

	@DeleteMapping("/completed")
	List<View> htmxDeleteCompletedTodos(@RequestParam Optional<String> filter) {

		template.deleteCompletedTodos();

		return htmxIndex(filter);
	}
}
