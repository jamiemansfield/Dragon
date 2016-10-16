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
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The common tweaker for both the Minecraft client and server.
 */
public abstract class DragonTweaker implements ITweaker {

    static final Logger LOGGER = LogManager.getLogger("Dragon");

    private List<String> args;
    protected File dragonDir;

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.args = args;
        this.dragonDir = new File(gameDir, "dragon");
        if (!this.dragonDir.exists()) {
            this.dragonDir.mkdirs();
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        LOGGER.info("Initialising Dragon...");

        Arrays.stream(this.dragonDir.listFiles((dir, name) -> name.endsWith(".jar"))).forEach(this::loadTweaker);
    }

    @Override
    public String[] getLaunchArguments() {
        return this.args.toArray(new String[this.args.size()]);
    }

    private void loadTweaker(File tweaker) {
        final List<ITweaker> tweakers = (List<ITweaker>) Launch.blackboard.get("Tweaks");

        try {
            Launch.classLoader.addURL(tweaker.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }

        try {
            final JarFile jarFile = new JarFile(tweaker);
            final Manifest manifest = jarFile.getManifest();
            final String tweakClassName = manifest.getMainAttributes().getValue("TweakClass");

            if (tweakClassName != null && !tweakClassName.isEmpty()) {
                try {
                    final Class tweakClass = Class.forName(tweakClassName, false, Launch.classLoader);
                    final ITweaker tweak = (ITweaker) tweakClass.newInstance();

                    tweakers.add(tweak);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
