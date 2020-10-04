package utils.module;

import akka.routing.FromConfig;
import akka.routing.RouterConfig;
import com.google.inject.AbstractModule;
import play.libs.akka.AkkaGuiceSupport;

public class ActorStartModule extends AbstractModule implements AkkaGuiceSupport {

    @Override
    protected void configure() {
        System.out.println("binding actors for dependency injection");
        final RouterConfig config = new FromConfig();
        for (ACTOR_NAMES actor : ACTOR_NAMES.values()) {
            bindActor(
                    actor.getActorClass(),
                    actor.getActorName(),
                    (props) -> {
                        return props.withRouter(config);
                    });
        }
        System.out.println("binding completed");
    }
}
