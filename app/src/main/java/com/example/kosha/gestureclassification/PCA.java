package com.example.kosha.gestureclassification;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;

public class PCA {

    private DenseMatrix64F V_t;
    private int numComponents;
    private DenseMatrix64F A = new DenseMatrix64F(1,1);
    private int sampleIndex;
    double mean[];

    public PCA() {
    }


    public void setup( int numSamples , int dimensionality ) {
        mean = new double[ dimensionality ];
        A.reshape(numSamples,dimensionality,false);
        sampleIndex = 0;
        numComponents = -1;
    }

    public void addSample( double[] sampleData ) {
        if( A.getNumCols() != sampleData.length )
            throw new IllegalArgumentException("Input data has incorrect dimensionality");
        if( sampleIndex >= A.getNumRows() )
            throw new IllegalArgumentException("Too many samples");

        for( int i = 0; i < sampleData.length; i++ ) {
            A.set(sampleIndex,i,sampleData[i]);
        }
        sampleIndex++;
    }

    public void computeBasis( int numComponents ) {
        if( numComponents > A.getNumCols() )
            throw new IllegalArgumentException("More components requested that the data's length.");
        if( sampleIndex != A.getNumRows() )
            throw new IllegalArgumentException("Not all the data has been added");
        if( numComponents > sampleIndex )
            throw new IllegalArgumentException("More data needed to compute the desired number of components");

        this.numComponents = numComponents;

        for( int i = 0; i < A.getNumRows(); i++ ) {
            for( int j = 0; j < mean.length; j++ ) {
                mean[j] += A.get(i,j);
            }
        }
        for( int j = 0; j < mean.length; j++ ) {
            mean[j] /= A.getNumRows();
        }
        for( int i = 0; i < A.getNumRows(); i++ ) {
            for( int j = 0; j < mean.length; j++ ) {
                A.set(i,j,A.get(i,j)-mean[j]);
            }
        }
        SingularValueDecomposition<DenseMatrix64F> svd =
                DecompositionFactory.svd(A.numRows, A.numCols, false, true, false);
        if( !svd.decompose(A) )
            throw new RuntimeException("SVD failed");

        V_t = svd.getV(null,true);
        DenseMatrix64F W = svd.getW(null);
        SingularOps.descendingOrder(null,false,W,V_t,true);

        V_t.reshape(numComponents,mean.length,true);
    }

    public double[] getBasisVector( int which ) {
        if( which < 0 || which >= numComponents )
            throw new IllegalArgumentException("Invalid component");

        DenseMatrix64F v = new DenseMatrix64F(1,A.numCols);
        CommonOps.extract(V_t,which,which+1,0,A.numCols,v,0,0);

        return v.data;
    }
    public double[] sampleToEigenSpace( double[] sampleData ) {
        if( sampleData.length != A.getNumCols() )
            throw new IllegalArgumentException("Unexpected sample length");
        DenseMatrix64F mean = DenseMatrix64F.wrap(A.getNumCols(),1,this.mean);

        DenseMatrix64F s = new DenseMatrix64F(A.getNumCols(),1,true,sampleData);
        DenseMatrix64F r = new DenseMatrix64F(numComponents,1);

        CommonOps.subtract(s, mean, s);

        CommonOps.mult(V_t,s,r);

        return r.data;
    }
    public double[] eigenToSampleSpace( double[] eigenData ) {
        if( eigenData.length != numComponents )
            throw new IllegalArgumentException("Unexpected sample length");

        DenseMatrix64F s = new DenseMatrix64F(A.getNumCols(),1);
        DenseMatrix64F r = DenseMatrix64F.wrap(numComponents,1,eigenData);

        CommonOps.multTransA(V_t,r,s);

        DenseMatrix64F mean = DenseMatrix64F.wrap(A.getNumCols(),1,this.mean);
        CommonOps.add(s,mean,s);

        return s.data;
    }

}
