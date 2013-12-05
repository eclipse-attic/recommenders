/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.rcp;

import static org.eclipse.jface.resource.ImageDescriptor.createFromFile;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.services.IDisposable;

/**
 * Registry for images shared over several Recommenders plugins. It's worth mentioning that this registry supports lazy
 * loading of {@link ImageResource}s, and thus, can be used by other plugins to share their images too.
 * 
 * @See {@link ImageResource}
 */
public final class SharedImages implements IDisposable {

    /**
     * An {@link ImageResource} is a key for Recommenders‘ {@link SharedImages}. The key is used (i) to lookup images in
     * the registry and (ii) to load these images lazily if not yet available in the registry. If not yet loaded, the
     * resource's bundle-classloader is used to lookup the image.
     * 
     * @see #getName()
     * @see ImageDescriptor#createFromFile(Class, String)
     */
    public interface ImageResource {

        /**
         * @return the resource name of an image, i.e., a valid parameter to {@link Class#getResource(String)}
         * 
         * @see <a href="http://wiki.eclipse.org/User_Interface_Guidelines#Folder_Structure">Eclipse standard for folder
         *      names</a>
         */
        String getName();
    }

    /**
     * @return resource name for a <strong>d</strong>isabled <strong>l</strong>o<strong>c</strong>a<strong>l</strong>
     *         toolbar
     */
    public static String dlcl16(String image) {
        return "/icons/dlcl16/" + image;
    }

    /**
     * @return resource name for an <strong>e</strong>nabled <strong>l</strong>o<strong>c</strong>a<strong>l</strong>
     *         toolbar
     */
    public static String elcl16(String image) {
        return "/icons/elcl16/" + image;
    }

    /**
     * @return resource name for a model <strong>obj</strong>ect
     */
    public static String obj16(String image) {
        return "/icons/obj16/" + image;
    }

    /**
     * @return resource name for an object <strong>ov</strong>e<strong>r</strong>lay
     */
    public static String ovr16(String image) {
        return "/icons/ovr16/" + image;
    }

    /**
     * @return resource name for a <strong>view</strong>
     */
    public static String view16(String image) {
        return "/icons/view16/" + image;
    }

    /**
     * @return resource name for a <strong>wiz</strong>ard <strong>ban</strong>ner graphic
     */
    public static String wizban(String image) {
        return "/icons/wizban/" + image;
    }

    public static enum Images implements ImageResource {
        // @formatter:off
        ELCL_COLLAPSE_ALL(elcl16("collapseall.gif")),
        ELCL_DELETE(elcl16("delete.gif")),
        ELCL_EXPAND_ALL(elcl16("expandall.gif")),
        ELCL_REFRESH(elcl16("refresh_tab.gif")),
        ELCL_SYNCED(elcl16("synced.gif")),
        ELCL_ADD_REPOSITORY(elcl16("add_repository.gif")),
        ELCL_REMOVE_REPOSITORY(elcl16("remove_repository.gif")),
        OBJ_CHECK_GREEN(obj16("tick_small.png")),
        OBJ_CROSS_RED(obj16("cross_small.png")),
        OBJ_BULLET_BLUE(obj16("bullet_blue.png")),
        OBJ_BULLET_GREEN(obj16("bullet_green.png")),
        OBJ_BULLET_ORANGE(obj16("bullet_orange.png")),
        OBJ_BULLET_RED(obj16("bullet_red.png")),
        OBJ_BULLET_STAR(obj16("bullet_star.png")),
        OBJ_BULLET_YELLOW(obj16("bullet_yellow.png")),
        OBJ_LIGHTBULB(obj16("lightbulb.gif")),
        OBJ_JAR(obj16("jar.gif")),
        OBJ_JAVA_PROJECT(obj16("project.gif")),
        OBJ_JRE(obj16("jre.gif")),
        OBJ_REPOSITORY(obj16("repository.gif")),
        OVR_STAR(ovr16("star.png")),
        VIEW_SLICE(view16("slice.gif"));
        // @formatter:on

        private final String name;

        private Images(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

    }

    private ImageRegistry registry = new ImageRegistry();

    public synchronized ImageDescriptor getDescriptor(ImageResource resource) {
        ImageDescriptor desc = registry.getDescriptor(toKey(resource));
        if (desc == null) {
            desc = register(resource);
        }
        return desc;
    }

    public synchronized Image getImage(ImageResource resource) {
        String key = toKey(resource);
        Image img = registry.get(key);
        if (img == null) {
            register(resource);
            img = registry.get(key);
        }
        return img;
    }

    private ImageDescriptor register(ImageResource resource) {
        ImageDescriptor desc = createFromFile(resource.getClass(), resource.getName());
        String key = toKey(resource);
        registry.put(key, desc);
        return desc;
    }

    private static String toKey(ImageResource resource) {
        return resource.getClass().getName() + "#" + resource.getName();
    }

    @Override
    public void dispose() {
        registry.dispose();
    }
}
