/*
 * This file is part of Dragon, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016, Jamie Mansfield <https://www.jamierocks.uk>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package uk.jamierocks.dragon;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The common tweaker for both the Minecraft client and server.
 */
public abstract class DragonTweaker implements ITweaker {

    static final Logger LOGGER = LogManager.getLogger("Dragon");

    protected File dragonDir;

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.dragonDir = new File(gameDir, "dragon");
        if (!this.dragonDir.exists()) {
            this.dragonDir.mkdirs();
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        LOGGER.info("Initialising Dragon...");

        final File[] jars = this.dragonDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars != null) {
            Arrays.asList(jars).forEach(this::loadTweaker);
        }

        LOGGER.info("Finished initialising Dragon. Launching Minecraft...");
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[]{};
    }

    private void loadTweaker(File tweaker) {
        LOGGER.info("Found tweaker candidate: " + tweaker.getName());

        final JarFile jarFile;
        final Manifest manifest;

        try {
            jarFile = new JarFile(tweaker);
            manifest = jarFile.getManifest();
        } catch (IOException e) {
            LOGGER.error("Failed to get jar info for " + tweaker.getName(), e);
            return;
        }

        try {
            Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
            addUrl.invoke(Launch.classLoader.getClass().getClassLoader(), tweaker.toURI().toURL());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | MalformedURLException e) {
            LOGGER.error("Failed to add " + tweaker.getName() + "to the classloader!", e);
            return;
        }

        try {
            Launch.classLoader.addURL(tweaker.toURI().toURL());
        } catch (MalformedURLException e) {
            LOGGER.error("Failed to add " + tweaker.getName() + "to the classloader!", e);
            return;
        }

        final String tweakClassName = manifest.getMainAttributes().getValue("TweakClass");
        if (tweakClassName != null && !tweakClassName.isEmpty()) {
            LOGGER.info("Found tweaker: " + tweakClassName + " (" + tweaker.getName() + ")");
            ((List<String>) Launch.blackboard.get("TweakClasses")).add(tweakClassName);
        }
    }

}
