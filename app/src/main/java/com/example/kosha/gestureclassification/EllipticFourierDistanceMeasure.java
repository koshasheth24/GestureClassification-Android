
package com.example.kosha.gestureclassification;

import android.util.Log;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EllipticFourierDistanceMeasure
{

    private List<List<Double>> unclassifiedShapeDescriptors;    // set of unclassified descriptors
    private List<List<List<Double>>> referenceDescriptors;      // sets of known shape descriptors
    private Constants.SHAPE_TYPE referenceShapeType;

    public EllipticFourierDistanceMeasure(List<List<Double>> unclassifiedShapeDescriptors, Constants.SHAPE_TYPE referenceShapeType) throws Exception
    {
        this.unclassifiedShapeDescriptors = unclassifiedShapeDescriptors;
        this.referenceShapeType = referenceShapeType;

        switch(referenceShapeType){
            case CIRCLE:
                this.referenceDescriptors = Constants.DESCRIPTORS_CIRCLE;
                break;
            case VEE:
                this.referenceDescriptors = Constants.DESCRIPTORS_VEE;
                break;
            case LINE:
                this.referenceDescriptors = Constants.DESCRIPTORS_LINE;
                break;
            default:
                throw new Exception("undefined shape type");
        }
    }

    public static Constants.SHAPE_TYPE findBestMatch(CopyOnWriteArrayList<Distance> listOfDistanceMeasures)
    {
        Constants.SHAPE_TYPE classification = Constants.SHAPE_TYPE.UNKNOWN;
        ArrayList<Distance> nonConcurrentList = new ArrayList<Distance>();
        for(Distance d : listOfDistanceMeasures)
            nonConcurrentList.add(d);

        Collections.sort(nonConcurrentList);

        Distance closestMatch = nonConcurrentList.get(0);

        classification = closestMatch.getShape();

        return(classification);
    }

    public List<Distance> computeDistanceMetric()
    {
        List<Distance> distances = new ArrayList<Distance>();

        for(List<List<Double>> reference_setOf8descriptors : this.referenceDescriptors)
        {
            Iterator<List<Double>> iterator_ref = reference_setOf8descriptors.iterator();
            Iterator<List<Double>> iterator_unknown = unclassifiedShapeDescriptors.iterator();

            double diffVector=0.0d;
            while(iterator_ref.hasNext() && iterator_unknown.hasNext())
            {
                double cosTheta_numerator = 0.0d;
                double cosTheta_denominator_multiplicand = 0.0d, cosTheta_denominator_multiplier = 0.0d;

                List<Double> R = iterator_ref.next();       // REFERENCE vector
                List<Double> U = iterator_unknown.next();   // UNCLASSIFIED vector

                Iterator<Double> iterator_R = R.iterator();
                Iterator<Double> iterator_U = U.iterator();

                double R_min = Double.MAX_VALUE,
                       U_min = Double.MAX_VALUE;            // represents the minimum vector components of each vector

                while(iterator_R.hasNext() && iterator_U.hasNext())
                {
                    double r = iterator_R.next(), u = iterator_U.next();
                    cosTheta_numerator += r * u;
                    cosTheta_denominator_multiplicand += u*u;
                    cosTheta_denominator_multiplier += r*r;

                    R_min = r < R_min && Math.abs(r)>0.0d ? r : R_min;
                    U_min = u < U_min && Math.abs(u)>0.0d ? u : U_min;
                }

                double cosTheta_denominator = Math.sqrt(cosTheta_denominator_multiplicand) * Math.sqrt(cosTheta_denominator_multiplier);
                double cosTheta=0.0d, theta=0.0d;
                try {
                    cosTheta = cosTheta_numerator / cosTheta_denominator;
                    theta = Math.acos(cosTheta);

                    iterator_R = R.iterator();
                    iterator_U = U.iterator();

                    double r_normalized,
                           u_normalized,
                           diffVectorComponent=0.0d;    // accumulator for differences between a single unclassified EFD and its counterpart reference EFD

                    while(iterator_R.hasNext() && iterator_U.hasNext())
                    {
                        r_normalized = iterator_R.next()/R_min;
                        u_normalized = iterator_U.next()/U_min;
                        diffVectorComponent += Math.abs(u_normalized - r_normalized) * (1+theta);
                    }
                    diffVector += diffVectorComponent;

                }catch(ArithmeticException arex)
                {
                }
            }

            distances.add(new Distance(this.referenceShapeType, diffVector));
        }

        return(distances);
    }

    public final class Distance implements Comparable<Distance>
    {
        private Map.Entry<Constants.SHAPE_TYPE,Double> distance;

        public double getDistance()
        {
            return(distance.getValue());
        }

        public Constants.SHAPE_TYPE getShape()
        {
            return(distance.getKey());
        }

        public Distance(Constants.SHAPE_TYPE st, double dist)
        {
            this.distance = new AbstractMap.SimpleEntry<Constants.SHAPE_TYPE,Double>(st,dist);
        }

        public int compareTo(Distance compareDistance)
        {
            double metric = compareDistance.getDistance();
            if(metric > this.distance.getValue())
                return(-1);
            else
            if(metric == this.distance.getValue())
                return(0);
            else
                return(1);
        }

    }

}
