/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.hubspot.jersey2.guicier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.junit.jupiter.api.Test;

/**
 * @author Petr Bouda
 */
public class InjectionManagerTest {

  @Test
  public void testServiceLocatorParent() {
    Module module = binder -> binder.bind(EnglishGreeting.class);

    Injector parentInjector = Guice.createInjector(module);

    InjectionManager injectionManager = Injections.createInjectionManager(parentInjector);
    injectionManager.completeRegistration();
    assertNotNull(injectionManager.getInstance(EnglishGreeting.class));
  }

  @Test
  public void testInjectionManagerParent() {
    ClassBinding<EnglishGreeting> greetingBinding = Bindings.serviceAsContract(
      EnglishGreeting.class
    );
    InjectionManager parentInjectionManager = Injections.createInjectionManager();
    parentInjectionManager.register(greetingBinding);
    parentInjectionManager.completeRegistration();

    InjectionManager injectionManager = Injections.createInjectionManager(
      parentInjectionManager
    );
    injectionManager.completeRegistration();
    assertNotNull(injectionManager.getInstance(EnglishGreeting.class));
  }

  @Test
  public void testUnknownParent() {
    assertThrows(
      IllegalArgumentException.class,
      () -> {
        Injections.createInjectionManager(new Object());
      }
    );
  }

  @Test
  public void testIsRegistrable() {
    InjectionManager injectionManager = Injections.createInjectionManager();
    assertTrue(injectionManager.isRegistrable(Module.class));
    assertFalse(
      injectionManager.isRegistrable(
        org.glassfish.jersey.internal.inject.AbstractBinder.class
      )
    );
    assertFalse(injectionManager.isRegistrable(String.class));
  }

  @Test
  public void testRegisterBinder() {
    Module module = binder -> binder.bind(EnglishGreeting.class);

    InjectionManager injectionManager = Injections.createInjectionManager();
    injectionManager.register(module);
    injectionManager.completeRegistration();
    assertNotNull(injectionManager.getInstance(EnglishGreeting.class));
  }

  @Test
  public void testRegisterUnknownProvider() {
    assertThrows(
      IllegalArgumentException.class,
      () -> {
        InjectionManager injectionManager = Injections.createInjectionManager();
        injectionManager.register(new Object());
      }
    );
  }
}
