package datadog.trace.instrumentation.grizzly;

import datadog.trace.bootstrap.instrumentation.api.URIDataAdapter;
import datadog.trace.bootstrap.instrumentation.decorator.HttpServerDecorator;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

public class GrizzlyDecorator extends HttpServerDecorator<Request, Request, Response> {
  public static final GrizzlyDecorator DECORATE = new GrizzlyDecorator();

  @Override
  protected String method(final Request request) {
    return request.getMethod().getMethodString();
  }

  @Override
  protected URIDataAdapter url(final Request request) {
    return new RequestURIDataAdapter(request);
  }

  @Override
  protected String peerHostIP(final Request request) {
    return request.getRemoteAddr();
  }

  @Override
  protected Integer peerPort(final Request request) {
    return request.getRemotePort();
  }

  @Override
  protected Integer status(final Response containerResponse) {
    return containerResponse.getStatus();
  }

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"grizzly"};
  }

  @Override
  protected String component() {
    return "grizzly";
  }
}
