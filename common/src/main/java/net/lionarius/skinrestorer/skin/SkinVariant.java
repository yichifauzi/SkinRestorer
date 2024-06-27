package net.lionarius.skinrestorer.skin;

public enum SkinVariant {
    
    CLASSIC("classic"),
    SLIM("slim");
    
    private final String name;
    
    SkinVariant(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
