package example.todomvc.web;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.View;

import example.todomvc.Todo;
import example.todomvc.web.TemplateModel.Form;
import io.jstach.opt.spring.webmvc.JStachioModelView;
import jakarta.validation.Valid;

/**
 * Spring MVC controller to render a Jstachio template. Assumes full HTTP requests and rendering.
 *
 * @author Oliver Drotbohm
 */
@Controller
@RequestMapping("/")
class TodoController {

	private final TemplateModel template;

	public TodoController(TemplateModel template) {
		this.template = template;
	}

	@GetMapping
	View index(@RequestParam Optional<String> filter) {
		return JStachioModelView.of(template.preparePage(filter));
	}

	@PostMapping
	String createTodo(@Valid @ModelAttribute("form") Form form) {

		template.save(form);

		return "redirect:/";
	}

	@PutMapping("/{id}/toggle")
	String toggleCompletion(@PathVariable("id") Todo todo) {

		if (todo == null) {
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
		}

		template.save(todo.toggleCompletion());

		return "redirect:/";
	}

	@DeleteMapping("/{todo}")
	String deleteTodo(@PathVariable Todo todo) {

		template.delete(todo);

		return "redirect:/";
	}

	@DeleteMapping("/completed")
	String deleteCompletedTodos(@RequestParam Optional<String> filter) {

		template.deleteCompletedTodos();

		return "redirect:/?filter=" + filter.orElse("");
	}
}
