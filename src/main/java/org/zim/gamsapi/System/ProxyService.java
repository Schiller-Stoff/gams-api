package org.zim.gamsapi.System;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplateHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;

@Service
@Slf4j
public class ProxyService implements IProxyService {

  private final RestTemplate restTemplate;

  public ProxyService(){
    ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
    restTemplate = new RestTemplate(factory);

    // makes sure that json data in url won't crash the resttemplate
    // https://stackoverflow.com/questions/21819210/using-resttemplate-in-spring-exception-not-enough-variables-available-to-expan
    // https://www.baeldung.com/spring-not-enough-variables-available
    UriTemplateHandler skipVariablePlaceHolderUriTemplateHandler = new UriTemplateHandler() {
      @Override
      public URI expand(String uriTemplate, Object... uriVariables) {
        return retrieveURI(uriTemplate);
      }

      @Override
      public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
        return retrieveURI(uriTemplate);
      }

      private URI retrieveURI(String uriTemplate) {
        return UriComponentsBuilder.fromUriString(uriTemplate).build().toUri();
      }
    };

    restTemplate.setUriTemplateHandler(skipVariablePlaceHolderUriTemplateHandler);
  }

  public ResponseEntity<String> proxy(HttpServletRequest request, String body, String targetUrl) {
    // append REST search params
    if(request.getQueryString() != null){
      targetUrl += "?" + request.getQueryString();
    }

    // to avoid doubled encoding (might be already encoded by reverse-proxy)
    targetUrl = URLDecoder.decode(targetUrl, StandardCharsets.UTF_8);
    log.trace("*** Built proxy target url {}", targetUrl);

    // include original request headers
    HttpHeaders headers = new HttpHeaders();
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      headers.set(headerName, request.getHeader(headerName));
    }
    headers.remove(HttpHeaders.ACCEPT_ENCODING);

    // to actual proxy request and assign result to proxied response
    HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
    try {

      ResponseEntity<String> serverResponse = restTemplate.exchange(targetUrl, HttpMethod.valueOf(request.getMethod()), httpEntity, String.class);
      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.put(HttpHeaders.CONTENT_TYPE, serverResponse.getHeaders().get(HttpHeaders.CONTENT_TYPE));
      return serverResponse;
    } catch (HttpStatusCodeException e) {
      String msg = String.format("Failed to proxy to solr via %s. Original error message: %s", targetUrl, e);
      log.error(msg);
      return ResponseEntity.status(e.getRawStatusCode())
              .headers(e.getResponseHeaders())
              .body(e.getResponseBodyAsString());
    }

  }
}
