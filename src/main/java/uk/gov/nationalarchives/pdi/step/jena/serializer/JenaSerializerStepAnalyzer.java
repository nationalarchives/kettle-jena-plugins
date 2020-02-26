/**
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
package uk.gov.nationalarchives.pdi.step.jena.serializer;

import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.metaverse.api.IMetaverseNode;
import org.pentaho.metaverse.api.MetaverseAnalyzerException;
import org.pentaho.metaverse.api.StepField;
import org.pentaho.metaverse.api.analyzer.kettle.step.StepAnalyzer;

import java.util.HashSet;
import java.util.Set;

public class JenaSerializerStepAnalyzer extends StepAnalyzer<JenaSerializerStepMeta> {

  @Override
    protected Set<StepField> getUsedFields(final JenaSerializerStepMeta meta) {
        // no incoming fields are used by the Dummy step
        return null;
    }

    @Override
    protected void customAnalyze(final JenaSerializerStepMeta meta, final IMetaverseNode rootNode) throws MetaverseAnalyzerException {
        // add any custom properties or relationships here
        rootNode.setProperty("do_nothing", true);
    }

    @Override
    public Set<Class<? extends BaseStepMeta>> getSupportedSteps() {
        final Set<Class<? extends BaseStepMeta>> supportedSteps = new HashSet<>();
        supportedSteps.add(JenaSerializerStepMeta.class);
        return supportedSteps;
    }
}
