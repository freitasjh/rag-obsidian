package br.com.freitasjh.obsidianrag.config;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class McpSseMethodFilter {

    void init(@Observes StartupEvent ev, Router router) {
        router.route("/sse").order(-1).handler(ctx -> {
            if (ctx.request().method() != HttpMethod.GET) {
                ctx.response().setStatusCode(405).end();
            } else {
                ctx.next();
            }
        });
    }
}
