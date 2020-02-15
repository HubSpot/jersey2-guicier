package com.hubspot.jersey2.guicier;

import com.google.inject.Injector;
import javax.annotation.Priority;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerFactory;

@Priority(5) // HK2 is 10
public class GuiceInjectionManagerFactory implements InjectionManagerFactory {

  @Override
  public InjectionManager create() {
    return create(null);
  }

  @Override
  public InjectionManager create(Object parent) {
    if (
      parent != null &&
      !(parent instanceof Injector) &&
      !(parent instanceof GuiceInjectionManager)
    ) {
      throw new IllegalArgumentException(/* TODO */);
    }

    return new GuiceInjectionManager();
  }
}
