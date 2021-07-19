package com.aselab.rtls;

import com.aselab.rtls.model.Fraction;

public class DescartesLogic {

    int GetFirstTextNumber(int maxnum,int minnum, int ScFactor){
        int StNumber = 65;

        for(int i = maxnum;i<minnum;i++){
            if(i%ScFactor==0){
                StNumber = i;
                break;
            }
        }

        return StNumber;

    }

    float[] getMaxandMinPoints(int ScaleMultiplier, int ScFactor, int corner, Fraction n1, Fraction n2, Fraction n3, float MaxX, float MaxY){
        float[] Numbers = new float[2];

        MaxY=MaxY*-1;

        float nu1 = n1.devidef();
        float nu2 = n2.devidef();
        float nu3 = n3.devidef();

        float m = (nu1*-1)/nu2;
        float b = (nu3/nu2)/ScaleMultiplier;

        //int cx1 = MaxX;
        float cy1 = (m*MaxX)+b;

        float cx2 = (MaxY+(b*-1))/m;
        //int cy2 = MaxY;

        if(corner==1 && m>0 || corner==2 && m<0){
            if(cy1<=MaxY){
                Numbers[0] = MaxX*ScFactor;
                Numbers[1] = cy1*ScFactor*-1;
            }else{
                Numbers[0] = cx2*ScFactor;
                Numbers[1] = MaxY*ScFactor*-1;
            }
        }else if(corner==1 && m<0 || corner==2 && m>0){
            if(cy1>=MaxY){
                Numbers[0] = MaxX*ScFactor;
                Numbers[1] = cy1*ScFactor*-1;
            }else{
                Numbers[0] = cx2*ScFactor;
                Numbers[1] = MaxY*ScFactor*-1;
            }
        }

        return Numbers;
    }

    float[] Solve2x2(Fraction nu1, Fraction nu2, Fraction nu3, Fraction nu4, Fraction nu5, Fraction nu6){
        float[] Solve = new float[2];
        //determinate
        Fraction desho1 = Fraction.multiply(nu1,nu5);
        Fraction desho2 = Fraction.multiply(nu4,nu2);
        Fraction det = Fraction.subtract(desho1, desho2);

        //Search for x
        Fraction xsho1 = Fraction.multiply(nu3,nu5);
        Fraction xsho2 = Fraction.multiply(nu6,nu2);
        Fraction xsho3 = Fraction.subtract(xsho1,xsho2);

        Fraction x = Fraction.divide(xsho3,det);

        //Search for y
        Fraction ysho1 = Fraction.multiply(nu1,nu6);
        Fraction ysho2 = Fraction.multiply(nu4,nu3);
        Fraction ysho3 = Fraction.subtract(ysho1,ysho2);

        Fraction y = Fraction.divide(ysho3,det);

        Solve[0] = x.devidef();
        Solve[1] = y.devidef();

        return Solve;
    }

    public boolean isparallel(Fraction nu1, Fraction nu2, Fraction nu3, Fraction nu4, Fraction nu5, Fraction nu6){
        boolean isp;
        //float m = (nu1*-1)/nu2;
        Fraction mm1 = Fraction.multiply(nu1,new Fraction(-1));
        Fraction m1 = Fraction.divide(mm1,nu2);

        Fraction mm2 = Fraction.multiply(nu4,new Fraction(-1));
        Fraction m2 = Fraction.divide(mm2,nu5);

        isp = m1.divide() == m2.divide();

        return isp;
    }
}
