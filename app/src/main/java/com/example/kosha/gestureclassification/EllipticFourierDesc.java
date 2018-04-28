package com.example.kosha.gestureclassification;

public class EllipticFourierDesc {

    private double[] x; //the x and y coordinates
    private double[] y;
    private int m;
    public int nFD;
    public double[] efd_ax, efd_ay, efd_bx, efd_by;
    public double[] efd;

  public EllipticFourierDesc(double[] x, double[] y, int n) {
        this.x = x;
        this.y = y;
        this.nFD = n;
        this.m = x.length;
        computeEllipticFD();
    }

    public EllipticFourierDesc(double[] x, double[] y) {
        this.x = x;
        this.y = y;
        this.nFD = x.length / 2;
        this.m = x.length;
        computeEllipticFD();
    }

    private void computeEllipticFD() {
        efd_ax = new double[nFD];
        efd_ay = new double[nFD];
        efd_bx = new double[nFD];
        efd_by = new double[nFD];

        double t = 2.0 * Math.PI / m;
        double p = 0.0;
        double twoOverM = 2.0 / m;
        for (int k = 0; k < nFD; k++) {
            for (int i = 0; i < m; i++) {
                p = k * t * i;
                efd_ax[k] += x[i] * Math.cos(p);
                efd_bx[k] += x[i] * Math.sin(p);
                efd_ay[k] += y[i] * Math.cos(p);
                efd_by[k] += y[i] * Math.sin(p);
            }

            efd_ax[k] *= twoOverM;
            efd_bx[k] *= twoOverM;
            efd_ay[k] *= twoOverM;
            efd_by[k] *= twoOverM;

        }
        efd = new double[nFD];
        int first = 1; //index of the normalization values
        double denomA = (efd_ax[first] * efd_ax[first]) + (efd_ay[first] * efd_ay[first]);
        double denomB = (efd_bx[first] * efd_bx[first]) + (efd_by[first] * efd_by[first]);
        for (int k = 0; k < nFD; k++) {
            efd[k] = Math.sqrt((efd_ax[k] * efd_ax[k] + efd_ay[k] * efd_ay[k]) / denomA)
                    + Math.sqrt((efd_bx[k] * efd_bx[k] + efd_by[k] * efd_by[k]) / denomB);
        }

    }


}