package jooby.internal.mvc;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import jooby.Request;
import jooby.Response;
import jooby.Route;
import jooby.TemplateProcessor;
import jooby.mvc.Template;
import net.sf.cglib.reflect.FastMethod;

class MvcRoute implements Route {

  private FastMethod route;

  private ParamResolver resolver;

  public MvcRoute(final FastMethod route, final ParamResolver resolver) {
    this.route = requireNonNull(route, "The route is required.");
    this.resolver = requireNonNull(resolver, "The resolver is required.");
  }

  @Override
  public void handle(final Request request, final Response response) throws Exception {
    Method method = route.getJavaMethod();

    Object handler = request.get(method.getDeclaringClass());

    List<ParamValue> parameters = resolver.resolve(method);
    Object[] args = new Object[parameters.size()];
    for (int i = 0; i < parameters.size(); i++) {
      args[i] = parameters.get(i).get(request);
    }

    Object result = route.invoke(handler, args);

    // default view name
    String defaultViewName = Optional.ofNullable(method.getAnnotation(Template.class))
        .map(template -> template.value().isEmpty() ? method.getName() : template.value())
        .orElse(method.getName());
    response.header(TemplateProcessor.VIEW_NAME).setString(defaultViewName);

    // send it!
    response.send(result);
  }
}
