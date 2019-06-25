package com.hubspot.jersey2.guicier;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.glassfish.jersey.inject.spi.BinderConfigurationFactory;
import org.glassfish.jersey.model.ContractProvider;

public class GuiceBinderConfigurationFactory implements BinderConfigurationFactory {

  @Override
  public BinderConfiguration createBinderConfiguration(Function<Predicate<ContractProvider>, Set<Object>> getInstances) {
    // TODO
    return null;
  }
}
