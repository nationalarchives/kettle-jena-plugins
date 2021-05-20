/*
 * The MIT License
 * Copyright © 2020 The National Archives
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
package uk.gov.nationalarchives.pdi.step;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.plugins.*;
import org.pentaho.di.trans.step.StepMetaInterface;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * JUnit 5 Extension for working with a Step Plugin.
 *
 * Loads and registers the plugin with Pentaho Kettle before
 * a test runs, and unregisters it afterwards.
 *
 * For beforeAll/afterAll like functionality, it may be used within a test class like:
 *
 * <pre><code>
 *
 *     @RegisterExtension
 *     static final StepPluginResource JENA_MODEL_STEP_PLUGIN = new StepPluginResource(JenaModelStepMeta.class);
 *
 * </code></pre>
 *
 * or for before/after like functionality, it may be used instead like so:
 *
 * <pre><code>
 *
 *     @RegisterExtension
 *     final StepPluginResource jenaModelStepPlugin = new StepPluginResource(JenaModelStepMeta.class);
 *
 * </code></pre>
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class StepPluginResource implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback {

    private static final String STORE_REGISTRY_KEY = "registry";
    private static final String STORE_PLUGIN_TYPE_CLASS_KEY = "pluginTypeClass";
    private static final String STORE_STEP_PLUGIN_KEY = "stepPlugin";

    private final Class<? extends StepMetaInterface> clazz;
    private final String id;
    private final String category;
    private final String name;
    private final String description;
    private final String image;

    /**
     * Ensures that before/after is only
     * called once if beforeAll and afterAll
     * are in play, e.g. this resource
     * is static resource.
     */
    private int referenceCount;

    public StepPluginResource(final Class<? extends StepMetaInterface> stepMetaInterfaceClazz) {
        final Step stepAnnotation = stepMetaInterfaceClazz.getAnnotation(Step.class);
        if (stepAnnotation == null) {
            throw new IllegalArgumentException("The class: " + stepMetaInterfaceClazz.getName() + " is missing an @org.pentaho.di.core.annotations.Step annotation");
        }

        this.clazz = stepMetaInterfaceClazz;
        this.id = stepAnnotation.id();
        this.category = stepAnnotation.categoryDescription();
        this.name = stepAnnotation.name();
        this.description = stepAnnotation.description();
        this.image = stepAnnotation.image();
    }

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws Exception {
        before(extensionContext);
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        before(extensionContext);
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) throws Exception {
        after(extensionContext);
    }


    @Override
    public void afterAll(final ExtensionContext extensionContext) throws Exception {
        after(extensionContext);
    }

    private void before(final ExtensionContext extensionContext) throws KettlePluginException {
        if (++referenceCount != 1) {
            return;
        }

        // Get the Plugin Registry
        final PluginRegistry pluginRegistry = PluginRegistry.getInstance();

        // Create a Plugin
        final PluginTypeInterface pluginType = pluginRegistry.getPluginType(StepPluginType.class);
        final Class<? extends PluginTypeInterface> pluginTypeClass = pluginType.getClass();
        final Map<Class<?>, String> classMap = new HashMap();
        final PluginMainClassType mainClassTypesAnnotation = pluginTypeClass.getAnnotation(PluginMainClassType.class);
        classMap.put(mainClassTypesAnnotation.value(), clazz.getName());
        final PluginInterface stepPlugin = new Plugin(new String[]{id}, pluginTypeClass, mainClassTypesAnnotation.value(), category, name, description, image, false, false, classMap, new ArrayList(), (String)null, (URL)null, (String)null, (String)null, (String)null);

        // Register the Plugin
        pluginRegistry.registerPlugin(pluginTypeClass, stepPlugin);
        
        // Save state
        getStore(extensionContext).put(STORE_REGISTRY_KEY, pluginRegistry);
        getStore(extensionContext).put(STORE_PLUGIN_TYPE_CLASS_KEY, pluginTypeClass);
        getStore(extensionContext).put(STORE_STEP_PLUGIN_KEY, stepPlugin);
    }

    private void after(final ExtensionContext extensionContext) {
        if (--referenceCount != 0) {
            return;
        }

        // Get state
        final PluginRegistry pluginRegistry = getStore(extensionContext).get(STORE_REGISTRY_KEY, PluginRegistry.class);
        if (pluginRegistry != null) {
            final Class<? extends PluginTypeInterface> pluginType = getStore(extensionContext).get(STORE_PLUGIN_TYPE_CLASS_KEY, Class.class);
            final PluginInterface stepPlugin = getStore(extensionContext).get(STORE_STEP_PLUGIN_KEY, PluginInterface.class);

            // Unregister the Plugin
            pluginRegistry.removePlugin(pluginType, stepPlugin);
        }
    }

    private Store getStore(final ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getTestMethod()));
    }
}
