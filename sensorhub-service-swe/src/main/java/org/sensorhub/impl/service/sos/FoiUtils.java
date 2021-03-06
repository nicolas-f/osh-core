/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import net.opengis.OgcProperty;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.Envelope;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.api.persistence.IFoiFilter;
import org.vast.ogc.gml.GMLUtils;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.util.Bbox;


public class FoiUtils
{

    public static void updateFois(SOSOfferingCapabilities caps, IDataProducerModule<?> producer, int maxFois)
    {
        caps.getRelatedFeatures().clear();
        caps.getObservedAreas().clear();        
        
        if (producer instanceof IMultiSourceDataProducer)
        {
            Collection<? extends AbstractFeature> fois = ((IMultiSourceDataProducer)producer).getFeaturesOfInterest();
            int numFois = fois.size();
            
            Bbox boundingRect = new Bbox();
            for (AbstractFeature foi: fois)
            {
                if (numFois <= maxFois)
                    caps.getRelatedFeatures().add(foi.getUniqueIdentifier());
                
                AbstractGeometry geom = foi.getLocation();
                if (geom != null)
                {
                    Envelope env = geom.getGeomEnvelope();
                    boundingRect.add(GMLUtils.envelopeToBbox(env));
                }
            }
            
            if (!boundingRect.isNull())
                caps.getObservedAreas().add(boundingRect);
        }
        else
        {
            AbstractFeature foi = producer.getCurrentFeatureOfInterest();
            if (foi != null)
            {
                caps.getRelatedFeatures().add(foi.getUniqueIdentifier());
                
                AbstractGeometry geom = foi.getLocation();
                if (geom != null)
                {
                    Envelope env = geom.getGeomEnvelope();
                    Bbox bbox = GMLUtils.envelopeToBbox(env);
                    caps.getObservedAreas().add(bbox);
                }
            }
        }
    }
    
    
    public static Iterator<AbstractFeature> getFilteredFoiIterator(IDataProducerModule<?> producer, IFoiFilter filter)
    {
        // get all fois from producer
        Iterator<? extends AbstractFeature> allFois;
        if (producer instanceof IMultiSourceDataProducer)
            allFois = ((IMultiSourceDataProducer)producer).getFeaturesOfInterest().iterator();
        else if (producer.getCurrentFeatureOfInterest() != null)
            allFois = Arrays.asList(producer.getCurrentFeatureOfInterest()).iterator();
        else
            allFois = Collections.EMPTY_LIST.iterator();
        
        // return all features if no filter is used
        if ((filter.getFeatureIDs() == null || filter.getFeatureIDs().isEmpty()) && filter.getRoi() == null)
            return (Iterator<AbstractFeature>)allFois;
        
        return new FilteredFoiIterator(allFois, filter);
    }
    
    
    public static String findEntityIDComponentURI(DataComponent dataStruct)
    {
        if (dataStruct instanceof DataRecord)
        {        
            for (int i = 0; i < dataStruct.getComponentCount(); i++)
            {
                OgcProperty<DataComponent> prop = ((DataRecord) dataStruct).getFieldList().getProperty(i);
                if (IMultiSourceDataInterface.ENTITY_ID_URI.equals(prop.getRole()))
                    return prop.getValue().getDefinition();
            }
        }
        else if (dataStruct instanceof DataChoice)
        {
            return findEntityIDComponentURI(dataStruct.getComponent(0));
        }
        
        return null;
    }
}
