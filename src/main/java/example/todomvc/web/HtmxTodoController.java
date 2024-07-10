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

import java.util.Optional;
import java.util.UUID;

import example.todomvc.Todo;
import example.todomvc.web.TemplateModel.TodoForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.FragmentsRendering;

@Profile("htmx")
@Controller
@RequiredArgsConstructor
@RequestMapping(headers = "HX-Request=true")
class HtmxTodoController {

	private final TemplateModel template;

	@GetMapping("/")
	FragmentsRendering htmxIndex(Model model, @RequestParam Optional<String> filter) {

		template.prepareForm(model, filter);
		model.addAttribute("action", "true");

		return FragmentsRendering.with("index :: todos").fragment("index :: foot").build();
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
	FragmentsRendering htmxCreateTodo(@Valid @ModelAttribute("form") TodoForm form, @RequestParam Optional<String> filter,
			Model model) {

		template.saveForm(form, model, filter);
		model.addAttribute("form", new TodoForm(""));
		model.addAttribute("action", "beforeend");

		return FragmentsRendering.with("index :: new-todo")
				.fragment("index :: todos")
				.fragment("index :: foot")
				.build();
	}

	@PutMapping("/{id}/toggle")
	FragmentsRendering htmxToggleCompletion(@PathVariable UUID id, @RequestParam Optional<String> filter, Model model) {

		Todo todo = template.find(id);
		final Todo result = template.save(todo.toggleCompletion(), model, filter);
		model.addAttribute("todo", result);

		String viewName = filter
				.map(it -> it.equals("active") && result.isCompleted() || it.equals("inactive") && !result.isCompleted()
						? "fragments :: remove-todo"
						: "fragments :: update-todo")
				.orElse("fragments :: update-todo");

		return FragmentsRendering.with(viewName).fragment("index :: foot").build();
	}

	@DeleteMapping("/{id}")
	FragmentsRendering htmxDeleteTodo(@PathVariable UUID id, @RequestParam Optional<String> filter, Model model) {

		Todo todo = template.find(id);
		template.delete(todo, model, filter);
		model.addAttribute("todo", todo);

		return FragmentsRendering.with("fragments :: remove-todo").fragment("index :: foot").build();
	}

	@DeleteMapping("/completed")
	FragmentsRendering htmxDeleteCompletedTodos(@RequestParam Optional<String> filter, Model model) {

		template.deleteCompletedTodos();

		return htmxIndex(model, filter);
	}
}
