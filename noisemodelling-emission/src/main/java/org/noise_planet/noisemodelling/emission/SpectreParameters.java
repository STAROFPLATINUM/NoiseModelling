package org.noise_planet.noisemodelling.emission;


public class SpectreParameters {
    private String typeVehicle;
    private String ref;
    private int runningCondition;
    private String sourceHeight;
    private int spectreVer;
    private int freqId;

    public SpectreParameters(String typeVehicle, String ref, int runningCondition, String sourceHeight, int spectreVer, int freqId) {
        this.typeVehicle = typeVehicle;
        this.ref = ref;
        this.runningCondition = runningCondition;
        this.sourceHeight = sourceHeight;
        this.spectreVer = spectreVer;
        this.freqId = freqId;
    }

    public String getTypeVehicle() {
        return typeVehicle;
    }

    public String getRef() {
        return ref;
    }

    public int getRunningCondition() {
        return runningCondition;
    }

    public String getSourceHeight() {
        return sourceHeight;
    }

    public int getSpectreVer() {
        return spectreVer;
    }

    public int getFreqId() {
        return freqId;
    }

    public void setTypeVehicle(String typeVehicle) {
        this.typeVehicle = typeVehicle;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public void setRunningCondition(int runningCondition) {
        this.runningCondition = runningCondition;
    }

    public void setSourceHeight(String sourceHeight) {
        this.sourceHeight = sourceHeight;
    }

    public void setSpectreVer(int spectreVer) {
        this.spectreVer = spectreVer;
    }

    public void setFreqId(int freqId) {
        this.freqId = freqId;
    }
}

