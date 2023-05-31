package example.todomvc.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitterReturnValueHandler;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import reactor.core.publisher.Flux;

@Component
public class RequestMappingHandlerAdapterConfiguration implements BeanPostProcessor {

	private final ApplicationContext context;

	RequestMappingHandlerAdapterConfiguration(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof RequestMappingHandlerAdapter adapter) {
			List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>(adapter.getReturnValueHandlers());
			for (int i = 0; i < handlers.size(); i++) {
				if (handlers.get(i) instanceof ResponseBodyEmitterReturnValueHandler handler) {
					handlers.set(i, new CompositeViewRenderer(context, handler));
				}
			}
			adapter.setReturnValueHandlers(handlers);
		}
		return bean;
	}

}

class CompositeViewRenderer implements HandlerMethodReturnValueHandler {

	private ViewResolver resolver;

	private final ApplicationContext context;

	private final ResponseBodyEmitterReturnValueHandler handler;

	public CompositeViewRenderer(ApplicationContext context, ResponseBodyEmitterReturnValueHandler handler) {
		this.context = context;
		this.handler = handler;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		initialize();
		if (isPublisherOfViews(returnType)) {
			return true;
		}
		return handler.supportsReturnType(returnType);
	}

	private boolean isPublisherOfViews(MethodParameter returnType) {
		if (Publisher.class.isAssignableFrom(returnType.getParameterType())) {
			if (ModelAndView.class
					.isAssignableFrom(ResolvableType.forMethodParameter(returnType).getGeneric().resolve())) {
				return true;
			}
		}
		return false;
	}

	private void initialize() {
		if (this.resolver == null) {
			this.resolver = context.getBean("viewResolver", ViewResolver.class);
		}
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
		if (isPublisherOfViews(returnType)) {
			String[] methodAnnotation = returnType
					.getMethodAnnotation(RequestMapping.class).produces();
			MediaType type = methodAnnotation.length > 0 ? MediaType.valueOf(methodAnnotation[0]) : MediaType.TEXT_HTML;
			var response = webRequest.getNativeResponse(HttpServletResponse.class);
			var request = webRequest.getNativeRequest(HttpServletRequest.class);
			response.setContentType(type.toString());
			@SuppressWarnings("unchecked")
			Flux<String> renderings = Flux.from((Publisher<ModelAndView>) returnValue).map(
					rendering -> new String(new RenderingModelAndView(rendering, resolver, request, response).render())
							+ "\n\n");
			returnValue = renderings;
			returnType = MethodParameter.forExecutable(getClass().getMethod("fluxString"), -1);
		}
		handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);

	}

	public Flux<String> fluxString() {
		return null;
	}
}

class RenderingModelAndView {

	private ModelAndView rendering;
	private ViewResolver resolver;
	private HttpServletResponse response;
	private HttpServletRequest request;

	public RenderingModelAndView(ModelAndView rendering, ViewResolver resolver, HttpServletRequest request,
			HttpServletResponse response) {
		this.rendering = rendering;
		this.resolver = resolver;
		this.request = request;
		this.response = response;
	}

	private void resolve(HttpServletResponse response, ModelAndView rendering) throws Exception {
		View view = null;
		if (rendering.getView() instanceof View) {
			view = (View) rendering.getView();
		} else {
			Locale locale = response.getLocale();
			if (locale == null) {
				locale = Locale.getDefault();
			}
			boolean clear = false;
			if (RequestContextHolder.getRequestAttributes() == null) {
				RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
				clear = true;
			}
			view = resolver.resolveViewName((String) rendering.getViewName(), locale);
			if (clear) {
				RequestContextHolder.resetRequestAttributes();
			}
		}
		rendering.setView(view);
	}

	public byte[] render() {
		try {
			ResponseWrapper wrapper = new ResponseWrapper(response);
			resolve(response, rendering);
			rendering.getView().render(rendering.getModel(), request, wrapper);
			return wrapper.getBytes();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	static class ResponseWrapper extends HttpServletResponseWrapper {

		private ServletOutputStream out;
		private ByteArrayOutputStream bytes = new ByteArrayOutputStream();

		public ResponseWrapper(HttpServletResponse response) {
			super(response);
			this.out = new ServletOutputStream() {

				@Override
				public void write(int b) throws IOException {
					bytes.write(b);
				}

				@Override
				public boolean isReady() {
					return true;
				}

				@Override
				public void setWriteListener(WriteListener listener) {
				}

			};
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return this.out;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			return new PrintWriter(this.out);
		}

		public byte[] getBytes() {
			return this.bytes.toByteArray();
		}

	}

}