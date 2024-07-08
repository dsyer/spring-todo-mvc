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

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Fragment;
import org.springframework.web.reactive.result.view.FragmentRendering;

import example.todomvc.Todo;
import example.todomvc.web.TemplateModel.TodoForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Profile("htmx")
@Controller
@RequiredArgsConstructor
@RequestMapping(headers = "HX-Request=true")
class HtmxTodoController {

	private final TemplateModel template;

	@GetMapping("/")
	FragmentRendering htmxIndex(Model model, @RequestParam Optional<String> filter) {

		template.prepareForm(model, filter);
		model.addAttribute("action", "true");

		return FragmentRendering.fromPublisher(Flux.just(Fragment.create("index :: todos", model.asMap()),
				Fragment.create("index :: foot", model.asMap()))).build();
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
	FragmentRendering htmxCreateTodo(@Valid @ModelAttribute("form") TodoForm form, @RequestParam Optional<String> filter,
			Model model) {

		template.saveForm(form, model, filter);
		model.addAttribute("form", new TodoForm(""));
		model.addAttribute("action", "beforeend");

		return FragmentRendering.fromPublisher(Flux.just(Fragment.create("index :: new-todo", model.asMap()),
				Fragment.create("index :: todos", model.asMap()),
				Fragment.create("index :: foot", model.asMap()))).build();
	}

	@GetMapping("/{id}/toggle")
	FragmentRendering htmxToggleCompletion(@PathVariable UUID id, @RequestParam Optional<String> filter, Model model) {

		Todo todo = template.find(id);
		final Todo result = template.save(todo.toggleCompletion(), model, filter);
		model.addAttribute("todo", result);

		return FragmentRendering.fromPublisher(filter
				.map(it -> it.equals("active") && result.isCompleted() || it.equals("inactive") && !result.isCompleted()
						? Flux.just(Fragment.create("fragments :: remove-todo", model.asMap()))
						: Flux.just(Fragment.create("fragments :: update-todo", model.asMap())))
				.orElse(Flux.just(Fragment.create("fragments :: update-todo", model.asMap())))
				.concatWithValues(Fragment.create("index :: foot", model.asMap()))).build();
	}

	@DeleteMapping("/{id}")
	FragmentRendering htmxDeleteTodo(@PathVariable UUID id, @RequestParam Optional<String> filter, Model model) {

		Todo todo = template.find(id);
		template.delete(todo, model, filter);
		model.addAttribute("todo", todo);

		return FragmentRendering.fromPublisher(Flux.just(Fragment.create("fragments :: remove-todo", model.asMap()),
				Fragment.create("index :: foot", model.asMap()))).build();
	}

	@DeleteMapping("/completed")
	FragmentRendering htmxDeleteCompletedTodos(@RequestParam Optional<String> filter, Model model) {

		template.deleteCompletedTodos();

		return htmxIndex(model, filter);
	}
}
