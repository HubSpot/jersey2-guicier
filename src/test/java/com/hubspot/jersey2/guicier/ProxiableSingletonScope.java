/*
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
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

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import javax.inject.Singleton;

/**
 * The context keeping the objects created in {@link ProxiableSingleton} scope.
 */
@Singleton
public class ProxiableSingletonScope implements Scope {

  @Override
  public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
    return Scopes.SINGLETON.scope(key, unscoped);
  }
}
