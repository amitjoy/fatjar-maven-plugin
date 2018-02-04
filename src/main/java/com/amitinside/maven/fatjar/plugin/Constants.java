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

import static java.io.File.separator;

public final class Constants {

    private Constants() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static final String MAVEN_LOCAL_REPOSITORY = ".m2" + separator + "repository";
    public static final String BNDLIB = "biz.aQute.bnd.jar";

}
