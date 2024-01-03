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

import jakarta.validation.Valid;

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
import org.springframework.web.reactive.result.view.Rendering;

import example.todomvc.Todo;
import example.todomvc.web.TemplateModel.TodoForm;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Profile("unpoly")
@Controller
@RequiredArgsConstructor
@RequestMapping(headers = "X-Up-Context")
class UnpolyTodoController {

	private final TemplateModel template;

	@GetMapping("/")
	Flux<Rendering> upIndex(Model model, @RequestParam Optional<String> filter) {

		template.prepareForm(model, filter);

		return Flux.just(Rendering.view("index :: todos").model(model.asMap()).build(),
				Rendering.view("index :: foot").model(model.asMap()).build());
	}

	@GetMapping("/toggles")
	Flux<Rendering> toggles() {
		return Flux.just(Rendering.view("fragments :: toggle-all").build());
	}

	/**
	 * An optimized variant of {@link #createTodo(TodoItemFormData)}. We perform the
	 * normal insert and then return two {@link upPartials} for the parts of the
	 * page that need updates by rendering the corresponding fragments of the
	 * template.
	 *
	 * @param form
	 * @param model
	 * @return
	 */
	@PostMapping("/")
	Flux<Rendering> upCreateTodo(@Valid @ModelAttribute("form") TodoForm form, @RequestParam Optional<String> filter,
			Model model) {

		template.saveForm(form, model, filter);
		model.addAttribute("form", new TodoForm(""));

		return Flux.just(Rendering.view("index :: new-todo").model(model.asMap()).build(),
				Rendering.view("index :: todos").model(model.asMap()).build(),
				Rendering.view("index :: foot").model(model.asMap()).build());
	}

	@PutMapping("/{id}/toggle")
	Flux<Rendering> upToggleCompletion(@PathVariable UUID id, @RequestParam Optional<String> filter, Model model) {

		Todo todo = template.find(id);
		final Todo result = template.save(todo.toggleCompletion(), model, filter);
		model.addAttribute("todo", result);

		return filter
				.map(it -> it.equals("active") && result.isCompleted() || it.equals("inactive") && !result.isCompleted()
						? Flux.just(Rendering.view("fragments :: remove-todo").model(model.asMap()).build())
						: Flux.just(Rendering.view("fragments :: update-todo").model(model.asMap()).build()))
				.orElse(Flux.just(Rendering.view("fragments :: update-todo").model(model.asMap()).build()))
				.concatWithValues(Rendering.view("index :: foot").model(model.asMap()).build());
	}

	@DeleteMapping("/{id}")
	Flux<Rendering> upDeleteTodo(@PathVariable UUID id, @RequestParam Optional<String> filter, Model model) {

		Todo todo = template.find(id);
		template.delete(todo, model, filter);
		model.addAttribute("todo", todo);

		return Flux.just(Rendering.view("fragments :: remove-todo").model(model.asMap()).build(),
				Rendering.view("index :: foot").model(model.asMap()).build());
	}

	@DeleteMapping("/completed")
	Flux<Rendering> upDeleteCompletedTodos(@RequestParam Optional<String> filter, Model model) {

		template.deleteCompletedTodos();

		return upIndex(model, filter);
	}
}
