package utils.module;


import org.sunbird.actor.CertBackgroundActor;
import org.sunbird.actor.CertificationActor;

public enum ACTOR_NAMES {
  CERTIFICATION_ACTOR(CertificationActor.class, "certification_actor"),
  CERTIFICATE_BACKGROUND_ACTOR(CertBackgroundActor.class, "certificate_background_actor");

  ACTOR_NAMES(Class clazz, String name) {
    actorClass = clazz;
    actorName = name;
  }

  private Class actorClass;
  private String actorName;

  public Class getActorClass() {
    return actorClass;
  }

  public String getActorName() {
    return actorName;
  }
}
