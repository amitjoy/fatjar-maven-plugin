/*******************************************************************************
 * Copyright (c) 2018 Amit Kumar Mondal
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package com.amitinside.maven.fatjar.plugin;

import java.util.EnumMap;

import com.google.common.collect.Maps;

public enum Configurer {

    INSTANCE;

    public enum Params {
        MAVEN_LOCATION,
        POM_LOCATION,
        BUNDLE_SYMBOLIC_NAME,
        BUNDLE_VERSION,
        EXTENSION_TO_UNARCHIVE,
        SOURCE_DIRECTORY,
        TARGET_FILENAME,
        TARGET_DIRECTORY,
        BUNDLE_RESOLVABLE,
        UPDATE_VERSION;
    }

    private final EnumMap<Params, Object> configuration = Maps.newEnumMap(Params.class);

    public void put(final Params key, final Object value) {
        configuration.put(key, value);
    }

    public String getAsString(final Params key) {
        final Object object = configuration.get(key);
        if (object instanceof String) {
            return String.valueOf(object);
        }
        return "";
    }

    public Object get(final Params key) {
        return configuration.get(key);
    }

}
