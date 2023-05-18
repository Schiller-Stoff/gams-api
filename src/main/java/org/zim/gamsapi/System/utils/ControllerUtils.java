package org.zim.gamsapi.System.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ControllerUtils {

    /**
     * Constructs URL's origin (protocol + host) according to request header-variables X-Forwarded-Host and X-Forwarded-Protocol.
     * provided by reverse-proxy. [e.g. 'https://my.domain.com/' OR JUST '/']
     * If X-Forwarded-Host is not available resolves to "/" - context relative root - for
     * local development without reverse-proxy.
     * Necessary for reverse-proxy aware redirects ("/" would only be servlet context aware) invoked by spring controllers.
     * @param requestHeaders request's header
     * @return Resolved origin [e.g. 'https://my.domain.com/' OR JUST '/']
     */
    public static String resolveProxiedOrigin(Map<String, String> requestHeaders){

        String xForwardedHostHeader = "x-forwarded-host";

        if(!requestHeaders.containsKey(xForwardedHostHeader)){
            log.debug("Found no {} header at incoming request. Resolving origin to just '/'", xForwardedHostHeader);
            return "/";
        }

        String forwardedHostname = requestHeaders.get(xForwardedHostHeader);
        String forwardProtocol = requestHeaders.get("x-forwarded-proto");
        String resolvedProxyOrigin = String.format("%s://%s/",forwardProtocol, forwardedHostname);
        log.debug("Found {} header at incoming request. Resolving origin to just '/'", xForwardedHostHeader);

        // assert no null will land inside redirection
        if(!resolvedProxyOrigin.contains("null")) log.error("Failed redirection: Incorrectly constructed origin {} containing null. Got x-forwarded-host value {} and x-forwarded-proto value {}. Both have to be supplied by a reverse-proxy.", resolvedProxyOrigin, forwardedHostname, forwardProtocol);

        return resolvedProxyOrigin;
    }

}
