package engine;

public enum WallVersor {
    LEFT(new double[]{1, 0}, new double[]{0, 1}),
    RIGHT(new double[]{-1, 0}, new double[]{0, -1}),
    DOWN(new double[]{0, 1}, new double[]{-1, 0});

    final double[] en;
    final double[] et;

    WallVersor(double[] en, double[] et) {
        this.en = en;
        this.et = et;
    }

    public double[] getEn() {
        return en;
    }

    public double[] getEt() {
        return et;
    }
}
