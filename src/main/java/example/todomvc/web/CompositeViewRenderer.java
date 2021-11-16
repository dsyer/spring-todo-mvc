package example.todomvc.web;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CompositeViewRenderer implements HandlerResultHandler {

	private static Log logger = LogFactory.getLog(CompositeViewRenderer.class);

	private final ViewResolver resolver;

	public CompositeViewRenderer(ViewResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public boolean supports(HandlerResult result) {
		if (Publisher.class.isAssignableFrom(result.getReturnType().toClass())) {
			if (Rendering.class.isAssignableFrom(result.getReturnType().getGeneric(0).toClass())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		String[] methodAnnotation = ((InvocableHandlerMethod) result.getHandler())
				.getMethodAnnotation(RequestMapping.class).produces();
		MediaType type = methodAnnotation.length > 0 ? MediaType.valueOf(methodAnnotation[0]) : MediaType.TEXT_HTML;
		exchange.getResponse().getHeaders().setContentType(type);
		boolean sse = MediaType.TEXT_EVENT_STREAM.includes(type);
		@SuppressWarnings("unchecked")
		Flux<Rendering> renderings = Flux.from((Publisher<Rendering>) result.getReturnValue());
		final ExchangeWrapper wrapper = new ExchangeWrapper(exchange);
		return exchange.getResponse().writeAndFlushWith(render(wrapper, renderings)
				.map(buffers -> transform(exchange.getResponse().bufferFactory(), buffers, sse)));
	}

	private Publisher<DataBuffer> transform(DataBufferFactory factory, Flux<DataBuffer> buffers, boolean sse) {
		if (sse) {
			buffers = buffers.map(buffer -> prefix(buffer, factory.allocateBuffer(buffer.capacity())));
		}
		// Add closing empty lines
		return buffers.map(buffer -> buffer.write("\n\n", StandardCharsets.UTF_8));
	}

	private DataBuffer prefix(DataBuffer buffer, DataBuffer result) {
		String body = buffer.toString(StandardCharsets.UTF_8);
		body = "data:" + body.replace("\n", "\ndata:");
		result.write(body, StandardCharsets.UTF_8);
		DataBufferUtils.release(buffer);
		return result;
	}

	private Flux<Flux<DataBuffer>> render(ExchangeWrapper exchange, Flux<Rendering> renderings) {
		return renderings.flatMap(rendering -> render(exchange, rendering))
				.contextWrite(view -> view.put("body", new AtomicReference<Flux<Flux<DataBuffer>>>(Flux.empty())));
	}

	private Flux<Flux<DataBuffer>> render(ExchangeWrapper exchange, Rendering rendering) {
		Mono<View> view = null;
		if (rendering.view() instanceof View) {
			view = Mono.just((View) rendering.view());
		} else {
			Locale locale = exchange.getLocaleContext().getLocale();
			if (locale == null) {
				locale = Locale.getDefault();
			}
			view = resolver.resolveViewName((String) rendering.view(), locale);
		}
		logger.debug("View: " + rendering.view());
		return view.flatMap(actual -> actual.render(rendering.modelAttributes(), MediaType.TEXT_HTML, exchange))
				.thenMany(Flux.deferContextual(context -> {
					@SuppressWarnings("unchecked")
					Flux<Flux<DataBuffer>> flux = ((AtomicReference<Flux<Flux<DataBuffer>>>) context.get("body"))
							.getAndSet(Flux.empty());
					return flux;
				}));
	}

	static class ExchangeWrapper extends ServerWebExchangeDecorator {

		private ResponseWrapper response;

		protected ExchangeWrapper(ServerWebExchange delegate) {
			super(delegate);
			this.response = new ResponseWrapper(super.getResponse());
		}

		@Override
		public ServerHttpResponse getResponse() {
			return this.response;
		}

	}

	static class ResponseWrapper extends ServerHttpResponseDecorator {

		private HttpHeaders headers;

		public ResponseWrapper(ServerHttpResponse delegate) {
			super(delegate);
			headers = HttpHeaders.writableHttpHeaders(delegate.getHeaders());
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Override
		public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
			return writeAndFlushWith(Mono.just(body));
		}

		@Override
		public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
			Flux<Flux<DataBuffer>> map = Flux.from(body).map(publisher -> Flux.from(publisher));
			return Mono.deferContextual(context -> {
				@SuppressWarnings("unchecked")
				AtomicReference<Flux<Flux<DataBuffer>>> flux = (AtomicReference<Flux<Flux<DataBuffer>>>) context
						.get("body");
				return flux.updateAndGet(buffer -> {
					logger.debug("Appending: " + map);
					return buffer.concatWith(map);
				}).then();
			});
		}

	}

}