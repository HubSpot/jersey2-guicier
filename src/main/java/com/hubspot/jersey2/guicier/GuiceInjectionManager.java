package com.hubspot.jersey2.guicier;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.ServiceHolder;
import org.glassfish.jersey.internal.inject.ServiceHolderImpl;

public class GuiceInjectionManager implements InjectionManager {
  private Injector injector = Guice.createInjector(); // TODO

  @Override
  public void completeRegistration() {
    // TODO
  }

  @Override
  public void shutdown() {
    // TODO
  }

  @Override
  public void register(Binding binding) {
    // TODO
  }

  @Override
  public void register(Iterable<Binding> descriptors) {
    descriptors.forEach(this::register);
  }

  @Override
  public void register(Binder binder) {
    binder.getBindings().forEach(this::register);
  }

  @Override
  public void register(Object provider) throws IllegalArgumentException {
    if (isRegistrable(provider.getClass())) {
      com.google.inject.Binder binder = (com.google.inject.Binder) provider;
      // TODO
    } else {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public boolean isRegistrable(Class<?> clazz) {
    return com.google.inject.Binder.class.isAssignableFrom(clazz);
  }

  @Override
  public <T> T createAndInitialize(Class<T> createMe) {
    // TODO is this ok?
    return getInstance(createMe);
  }

  @Override
  public <T> List<ServiceHolder<T>> getAllServiceHolders(
    Class<T> contractOrImpl,
    Annotation... qualifiers
  ) {
    switch (qualifiers.length) {
      case 0:
        TypeLiteral<T> typeLiteral = TypeLiteral.get(contractOrImpl);
        return injector
          .findBindingsByType(typeLiteral)
          .stream()
          .map(this::asServiceHolder)
          .collect(Collectors.toList());
      case 1:
        Key<T> key = Key.get(contractOrImpl, qualifiers[0]);
        ServiceHolder<T> serviceHolder = asServiceHolder(injector.getBinding(key));
        return Collections.singletonList(serviceHolder);
      default:
        // TODO
        throw new IllegalArgumentException();
    }
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl, Annotation... qualifiers) {
    switch (qualifiers.length) {
      case 0:
        return getInstance(contractOrImpl);
      case 1:
        Key<T> key = Key.get(contractOrImpl, qualifiers[0]);
        return injector.getInstance(key);
      default:
        // TODO
        throw new IllegalArgumentException();
    }
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl, String classAnalyzer) {
    return getInstance(contractOrImpl);
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl) {
    return injector.getInstance(contractOrImpl);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getInstance(Type contractOrImpl) {
    return (T) injector.getInstance(Key.get(contractOrImpl));
  }

  @Override
  public Object getInstance(ForeignDescriptor foreignDescriptor) {
    // TODO
    return null;
  }

  @Override
  public ForeignDescriptor createForeignDescriptor(Binding binding) {
    // TODO
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> List<T> getAllInstances(Type contractOrImpl) {
    TypeLiteral<T> typeLiteral = (TypeLiteral<T>) TypeLiteral.get(contractOrImpl);
    return injector
      .findBindingsByType(typeLiteral)
      .stream()
      .map(binding -> binding.getProvider().get())
      .collect(Collectors.toList());
  }

  @Override
  public void inject(Object injectMe) {
    injector.injectMembers(injectMe);
  }

  @Override
  public void inject(Object injectMe, String classAnalyzer) {
    inject(injectMe);
  }

  @Override
  public void preDestroy(Object preDestroyMe) {
    // TODO should we support this?
    throw new UnsupportedOperationException();
  }

  private <T> ServiceHolder<T> asServiceHolder(com.google.inject.Binding<T> binding) {
    T instance = binding.getProvider().get();
    // TODO what are contract types?
    Set<Type> contractTypes = Collections.singleton(
      binding.getKey().getTypeLiteral().getType()
    );
    return new ServiceHolderImpl<>(instance, contractTypes);
  }
}
