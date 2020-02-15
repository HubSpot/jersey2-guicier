package com.hubspot.jersey2.guicier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.PerLookup;
import org.glassfish.jersey.internal.inject.ServiceHolder;
import org.glassfish.jersey.internal.inject.ServiceHolderImpl;
import org.glassfish.jersey.internal.inject.SupplierClassBinding;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;

public class GuiceInjectionManager implements InjectionManager {
  private final Stage stage = Stage.DEVELOPMENT; // TODO customizable?
  private final ImmutableList.Builder<Module> builder = ImmutableList
    .<Module>builder()
    .add(
      binder -> {
        binder.bindScope(PerLookup.class, Scopes.NO_SCOPE);
        // TODO per-thread
      }
    );
  private volatile Injector injector;

  @Override
  public void completeRegistration() {
    if (injector != null) {
      throw new IllegalStateException(/* TODO */);
    }

    synchronized (this) {
      if (injector != null) {
        throw new IllegalStateException(/* TODO */);
      }

      injector = Guice.createInjector(stage, builder.build());
    }
  }

  @Override
  public void shutdown() {
    // TODO
  }

  @Override
  public void register(Binding binding) {
    @SuppressWarnings("unchecked")
    Class<? extends Annotation> bindingScope = (Class<? extends Annotation>) binding.getScope();
    List<Key<Object>> bindingKeys = bindingKeys(binding);

    if (binding instanceof InstanceBinding) {
      InstanceBinding<?> instanceBinding = (InstanceBinding<?>) binding;
      Object instance = instanceBinding.getService();

      builder.add(
        binder -> {
          for (Key<Object> bindingKey : bindingKeys) {
            binder.bind(bindingKey).toInstance(instance);
            binder.bind(supplierKey(bindingKey)).toInstance(() -> instance);
          }
        }
      );
    } else if (binding instanceof ClassBinding) {
      ClassBinding<?> classBinding = (ClassBinding<?>) binding;
      Class<?> implementation = classBinding.getService();

      builder.add(
        binder -> {
          for (Key<Object> bindingKey : bindingKeys) {
            if (bindingKey.getTypeLiteral().getType() == implementation) {
              scope(binder.bind(bindingKey), bindingScope);
            } else {
              scope(binder.bind(bindingKey).to(implementation), bindingScope);
            }

            Provider<Object> provider = binder.getProvider(bindingKey);
            binder.bind(supplierKey(bindingKey)).toInstance(provider::get);
          }
        }
      );
    } else if (binding instanceof SupplierInstanceBinding) {
      SupplierInstanceBinding<?> supplierBinding = (SupplierInstanceBinding<?>) binding;
      @SuppressWarnings("unchecked")
      Supplier<Object> supplier = (Supplier<Object>) supplierBinding.getSupplier();

      builder.add(
        binder -> {
          for (Key<Object> bindingKey : bindingKeys) {
            scope(binder.bind(bindingKey).toProvider(supplier::get), bindingScope);

            // go through a provider to make sure we respect the binding scope
            Provider<Object> provider = binder.getProvider(bindingKey);
            binder.bind(supplierKey(bindingKey)).toInstance(provider::get);
          }
        }
      );
    } else if (binding instanceof SupplierClassBinding) {
      SupplierClassBinding<?> supplierClassBinding = (SupplierClassBinding<?>) binding;

      builder.add(
        binder -> {
          for (Key<Object> bindingKey : bindingKeys) {
            Provider<Supplier<Object>> supplierProvider = scopedProvider(
              supplierClassBinding,
              bindingKey,
              binder
            );

            Provider<Object> provider = () -> supplierProvider.get().get();
            scope(binder.bind(bindingKey).toProvider(provider), bindingScope);
            binder.bind(supplierKey(bindingKey)).toProvider(supplierProvider);
          }
        }
      );
    } else {
      throw new IllegalArgumentException(/* TODO */);
    }
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
      com.google.inject.Module module = (com.google.inject.Module) provider;
      builder.add(module);
    } else {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public boolean isRegistrable(Class<?> clazz) {
    return com.google.inject.Module.class.isAssignableFrom(clazz);
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
    // TODO support just-in-time bindings?
    return (T) injector.getExistingBinding(Key.get(contractOrImpl));
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

  @SuppressWarnings("unchecked")
  private static List<Key<Object>> bindingKeys(Binding<?, ?> binding) {
    final Function<Type, Key<?>> keyCreator;

    ImmutableSet.Builder<Annotation> builder = ImmutableSet.builder();
    builder.addAll(binding.getQualifiers());
    if (binding.getName() != null) {
      builder.add(Names.named(binding.getName()));
    }

    Set<Annotation> qualifiers = builder.build();
    if (qualifiers.isEmpty()) {
      keyCreator = Key::get;
    } else if (qualifiers.size() == 1) {
      Annotation qualifier = qualifiers.iterator().next();
      keyCreator = type -> Key.get(type, qualifier);
    } else {
      throw new IllegalArgumentException(/* TODO */);
    }

    return binding
      .getContracts()
      .stream()
      .map(keyCreator)
      .map(key -> (Key<Object>) key)
      .collect(ImmutableList.toImmutableList());
  }

  private static Provider<Supplier<Object>> scopedProvider(
    SupplierClassBinding<?> supplierClassBinding,
    Key<Object> bindingKey,
    com.google.inject.Binder binder
  ) {
    @SuppressWarnings("unchecked")
    Provider<Supplier<Object>> supplierProvider = (Provider<Supplier<Object>>) binder.getProvider(
      supplierClassBinding.getSupplierClass()
    );

    Class<? extends Annotation> scope = supplierClassBinding.getSupplierScope();
    if (scope == null || scope == PerLookup.class) {
      return supplierProvider;
    } else if (scope == Singleton.class) {
      @SuppressWarnings("unchecked")
      Key<Supplier<Object>> supplierKey = (Key<Supplier<Object>>) bindingKey.ofType(
        Types.newParameterizedType(Supplier.class, bindingKey.getTypeLiteral().getType())
      );

      return Scopes.SINGLETON.scope(supplierKey, supplierProvider);
    } else {
      // TODO support more than PerLookup/Singleton?
      throw new IllegalStateException(/* TODO */);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> Key<Supplier<T>> supplierKey(Key<T> key) {
    return (Key<Supplier<T>>) key.ofType(
      Types.newParameterizedType(Supplier.class, key.getTypeLiteral().getType())
    );
  }

  private static void scope(
    ScopedBindingBuilder bindingBuilder,
    Class<? extends Annotation> bindingScope
  ) {
    if (bindingScope != null) {
      bindingBuilder.in(bindingScope);
    }
  }
}
