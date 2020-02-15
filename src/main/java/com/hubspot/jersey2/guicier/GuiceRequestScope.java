package com.hubspot.jersey2.guicier;

import org.glassfish.jersey.process.internal.RequestContext;
import org.glassfish.jersey.process.internal.RequestScope;

public class GuiceRequestScope extends RequestScope {

  @Override
  public RequestContext createContext() {
    return new RequestContext() {

      @Override
      public RequestContext getReference() {
        // TODO
        return null;
      }

      @Override
      public void release() {
        // TODO
      }
    };
  }
}
