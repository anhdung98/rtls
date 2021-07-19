package com.aselab.rtls.model;

public class Fraction {
    private int num; // numerator
    private int den; // denominator

    private double dounu;
    private double doude;

    public Fraction() {
        num=0;
        den=1;
    }
    public Fraction(int x, int y) {
        num=x;
        den=y;
    }
    public Fraction(int x){
        num=x;
        den=1;
    }

    public Fraction(double x, double y){
        dounu=x;
        doude=y;
    }

    public Fraction(double x){
        dounu=x;
        doude=1;
    }

    public static Fraction sumar(Fraction a, Fraction b){
        a.simplify();
        b.simplify();
        Fraction c=new Fraction();
        c.num=a.num*b.den+b.num*a.den;
        c.den=a.den*b.den;
        return c.simplify();
    }

    public static Fraction subtract(Fraction a, Fraction b){
        a.simplify();
        b.simplify();
        Fraction c=new Fraction();
        c.num=a.num*b.den-b.num*a.den;
        c.den=a.den*b.den;
        return c.simplify();
    }

    public static Fraction multiply(Fraction a, Fraction b){
        a.simplify();
        b.simplify();
        return new Fraction(a.num*b.num, a.den*b.den).simplify();
    }

    public static Fraction multiply2(Fraction a, Fraction b , Fraction c){
        a.simplify();
        b.simplify();
        c.simplify();
        return new Fraction(a.num*b.num*c.num, a.den*b.den*c.den).simplify();
    }

    public static Fraction reverse(Fraction a){
        return new Fraction(a.den, a.num).simplify();
    }

    public static Fraction divide(Fraction a, Fraction b){
        return multiply(a, reverse(b)).simplify();
    }

    // Find the greatest common divisor
    private int gcd() {
        int u= Math.abs(num);
        int v= Math.abs(den);
        if(v==0){
            return u;
        }
        int r;
        while(v!=0){
            r=u%v;
            u=v;
            v=r;
        }
        return u;
    }
    public Fraction simplify(){
        int dividir= gcd();
        num/=dividir;
        den/=dividir;
        return this;
    }

    public String totext(){
        String text;
        if(num%den==0){
            text = String.valueOf(num/den);
        }else{
            if(den<0){
                text =num*-1+"/"+ Math.abs(den);
            }else if(den<0 && num<0){
                text =num*-1+"/"+den*-1;
            }else{
                text =num+"/"+den;
            }

        }

        return text;
    }

    public int getden(){
        return den;
    }

    public int getnum(){
        return num;
    }

    public double divide(){
        return (double)num/den;
    }

    public float devidef(){
        return (float)num/den;
    }

    public Fraction abs(){
        return new Fraction(Math.abs(num), Math.abs(den));
    }


    public String all3(){
        String sign;
        String fra = new Fraction(num,den).abs().simplify().totext();
        if(divide()<0){
            sign = " - ";
        }else{
            sign = " + ";
        }
        return sign + fra;
    }

    public String all2(){
        String sign;
        String fra = new Fraction(num,den).abs().simplify().totext();
        if(divide()<0){
            sign = " - ";
        }else{
            sign = " + ";
        }
        return sign + fra;
    }

    public String all(){
        return new Fraction(num,den).simplify().totext();
    }

    public Fraction simred(){
        Fraction f;
        if(num<=0 && den<=0){
            f = new Fraction(num,den).abs().simplify();
        }else{
            f = new Fraction(num,den).simplify();
        }
        return f;
    }

    public int FraInt(){
        return num/den;
    }


    public boolean si_divi(){
        boolean valor;
        valor = num % den == 0;
        return valor;
    }

    public Fraction Xredundancy() {

        if (num <= 0 && den < 0) {
            return new Fraction(num * -1, den * -1);
        }else{
            return new Fraction(num , den );
        }
    }

}
