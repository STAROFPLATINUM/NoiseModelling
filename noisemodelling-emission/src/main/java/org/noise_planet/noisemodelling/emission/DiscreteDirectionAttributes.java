/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */
package org.noise_planet.noisemodelling.emission;

import java.io.Serializable;
import java.util.*;

/**
 * Describe Attenuation directivity over a sphere
 * Values between specified angles are interpolated following a method (linear by default)
 */
public class DiscreteDirectionAttributes implements DirectionAttributes {
    int interpolationMethod = 1;
    int directionIdentifier;
    double[] frequencies;
    Map<Long, Integer> frequencyMapping = new HashMap<>();
    // List of records, maintain the two lists sorted
    List<DirectivityRecord> recordsTheta = new ArrayList<>();
    List<DirectivityRecord> recordsPhi = new ArrayList<>();

    ThetaComparator thetaComparator = new ThetaComparator();
    PhiComparator phiComparator = new PhiComparator();

    public DiscreteDirectionAttributes(int directionIdentifier, double[] frequencies) {
        this.directionIdentifier = directionIdentifier;
        this.frequencies = frequencies;
        for(int idFrequency = 0; idFrequency < frequencies.length; idFrequency++) {
            frequencyMapping.put(Double.doubleToLongBits(frequencies[idFrequency]), idFrequency);
        }
    }

    public void setInterpolationMethod(int interpolationMethod) {
        this.interpolationMethod = interpolationMethod;
    }

    public List<DirectivityRecord> getRecordsTheta() {
        return recordsTheta;
    }

    public List<DirectivityRecord> getRecordsPhi() {
        return recordsPhi;
    }

    public int getDirectionIdentifier() {
        return directionIdentifier;
    }

    @Override
    public double getAttenuation(double frequency, double phi, double theta) {
        DirectivityRecord query = new DirectivityRecord(theta, phi, null);

        // look for frequency index
        Integer idFreq = frequencyMapping.get(Double.doubleToLongBits(frequency));
        if(idFreq == null) {
            // get closest index
            idFreq = Arrays.binarySearch(frequencies, frequency);
            if(idFreq < 0) {
                int last = Math.min(-idFreq - 1, frequencies.length - 1);
                int first = Math.max(last - 1, 0);
                idFreq = Math.abs(frequencies[first] - frequency) < Math.abs(frequencies[last] - frequency) ?
                        first : last;
            }
        }
        return getRecord(query.theta, query.phi, interpolationMethod).getAttenuation()[idFreq];
    }

    @Override
    public double[] getAttenuationArray(double[] frequencies, double phi, double theta) {
        DirectivityRecord query = new DirectivityRecord(theta, phi, null);

        DirectivityRecord record = getRecord(query.theta, query.phi, interpolationMethod);

        double[] returnAttenuation = new double[frequencies.length];

        for(int frequencyIndex = 0; frequencyIndex < frequencies.length; frequencyIndex++) {
            double frequency = frequencies[frequencyIndex];
            // look for frequency index
            Integer idFreq = frequencyMapping.get(Double.doubleToLongBits(frequency));
            if (idFreq == null) {
                // get closest index
                idFreq = Arrays.binarySearch(frequencies, frequency);
                if (idFreq < 0) {
                    int last = Math.min(-idFreq - 1, frequencies.length - 1);
                    int first = Math.max(last - 1, 0);
                    idFreq = Math.abs(frequencies[first] - frequency) < Math.abs(frequencies[last] - frequency) ? first : last;
                }
            }
            returnAttenuation[frequencyIndex] = record.attenuation[idFreq];
        }

        return returnAttenuation;
    }

    /**
     * Add angle attenuation record
     * @param theta (-π/2 π/2) 0 is horizontal π is top
     * @param phi (0 2π) 0 is front
     * @param attenuation Attenuation in dB
     */
    public void addDirectivityRecord(double theta, double phi, double[] attenuation) {
        DirectivityRecord record = new DirectivityRecord(theta, phi, attenuation);
        int index = Collections.binarySearch(recordsTheta, record, thetaComparator);
        if(index >= 0) {
            // This record already exists
            return;
        } else {
            index = - index - 1;
        }
        recordsTheta.add(index, record);
        index = Collections.binarySearch(recordsPhi, record, phiComparator);
        index = - index - 1;
        recordsPhi.add(index, record);
    }

    /**
     * @return Frequencies of columns
     */
    public double[] getFrequencies() {
        return frequencies;
    }

    private static double getDistance(double theta, double phi, DirectivityRecord b) {
        return Math.acos(Math.sin(phi) * Math.sin(b.phi) + Math.cos(phi) * Math.cos(b.phi) * Math.cos(theta - b.theta));
    }
    /**
     * Retrieve DirectivityRecord for the specified angles
     * @param theta in radians
     * @param phi in radians
     * @param interpolate 0 for closest neighbor, 1 for Bilinear interpolation
     * @return DirectivityRecord instance
     */
    public DirectivityRecord getRecord(double theta, double phi, int interpolate) {
        DirectivityRecord[] allRecords;
        DirectivityRecord record = new DirectivityRecord(theta, phi, null);

        int thetaIndex = getThetaIndex(record);
        double theta1 = getTheta1(thetaIndex);
        double theta2 = getTheta2(thetaIndex);

        int phiIndex = getPhiIndex(record);
        double phi1 = getPhi1(phiIndex);
        double phi2 = getPhi2(phiIndex);

        int[] indexes = getIndexes(theta1, phi1, theta2, phi2);

        if (Arrays.stream(indexes).min().getAsInt() < 0) {
            // got issues looking for directivity
            return new DirectivityRecord(theta, phi, new double[frequencies.length]);
        }

        allRecords = getRecords(indexes);

        return interpolate == 0 ? findClosestRecord(theta, phi, allRecords) : bilinearInterpolation(theta, phi, allRecords);
    }

    private int getThetaIndex(DirectivityRecord record) {
        int index = Collections.binarySearch(recordsTheta, record, thetaComparator);
        return (index >= 0) ? index : -index - 1;
    }

    private double getTheta1(int index) {
        index = index >= recordsTheta.size() ? 0 : index;
        return recordsTheta.get(index).getTheta();
    }

    private double getTheta2(int index) {
        index = adjustIndex(index, recordsTheta.size());
        return recordsTheta.get(index).getTheta();
    }

    private int getPhiIndex(DirectivityRecord record) {
        int index = Collections.binarySearch(recordsPhi, record, phiComparator);
        return -index - 1;
    }

    private double getPhi1(int index) {
        index = index >= recordsPhi.size() ? 0 : index;
        return recordsPhi.get(index).getPhi();
    }

    private double getPhi2(int index) {
        index = adjustIndex(index, recordsPhi.size());
        return recordsPhi.get(index).getPhi();
    }

    private int adjustIndex(int index, int size) {
        index -= 1;
        return index < 0 ? size - 1 : index;
    }

    private int[] getIndexes(double theta1, double phi1, double theta2, double phi2) {
        return new int[]{
                Collections.binarySearch(recordsTheta, new DirectivityRecord(theta1, phi1, null), thetaComparator),
                Collections.binarySearch(recordsTheta, new DirectivityRecord(theta2, phi1, null), thetaComparator),
                Collections.binarySearch(recordsTheta, new DirectivityRecord(theta2, phi2, null), thetaComparator),
                Collections.binarySearch(recordsTheta, new DirectivityRecord(theta1, phi2, null), thetaComparator)
        };
    }

    private DirectivityRecord[] getRecords(int[] indexes) {
        return new DirectivityRecord[]{
                recordsTheta.get(indexes[0]),
                recordsTheta.get(indexes[1]),
                recordsTheta.get(indexes[2]),
                recordsTheta.get(indexes[3])
        };
    }

    private DirectivityRecord findClosestRecord(double theta, double phi, DirectivityRecord[] allRecords) {
        double minDist = Double.MAX_VALUE;
        DirectivityRecord closest = allRecords[0];
        for(DirectivityRecord r : allRecords) {
            double testDist = getDistance(theta, phi, r);
            if(testDist < minDist) {
                minDist = testDist;
                closest = r;
            }
        }
        return closest;
    }

    private DirectivityRecord bilinearInterpolation(double theta, double phi, DirectivityRecord[] allRecords) {
        double x1 = allRecords[0].theta;
        double y1 = allRecords[0].phi;

        double xLength = getDistance(allRecords[1].theta, allRecords[0].phi, allRecords[0]);
        double yLength = getDistance(allRecords[0].theta, allRecords[2].phi, allRecords[0]);
        // compute expected phi, theta as a normalized vector
        double x = normalize(getDistance(x1, phi, new DirectivityRecord(theta, phi, null)) / xLength);
        double y = normalize(getDistance(theta, y1, new DirectivityRecord(theta, phi, null)) / yLength);

        double[] att = new double[frequencies.length];
        for(int idFrequency = 0; idFrequency < frequencies.length; idFrequency++) {
            att[idFrequency] = Utils.wToDb(Utils.dbToW(allRecords[0].attenuation[idFrequency]) * (1 - x) * (1 - y)
                    + Utils.dbToW(allRecords[1].attenuation[idFrequency]) * x * (1 - y)
                    + Utils.dbToW(allRecords[3].attenuation[idFrequency]) * (1 - x) * y
                    + Utils.dbToW(allRecords[2].attenuation[idFrequency]) * x * y);
        }
        return new DirectivityRecord(theta, phi, att);
    }
    private double normalize(double value) {
        double normalized = Math.max(0, Math.min(1, value));
        return Double.isNaN(normalized) ? 0 : normalized;
    }

    /**
     * Add new records.
     * This function is much more efficient than {@link #addDirectivityRecord(double, double, double[])}
     * @param newRecords Records to push
     */
    public void addDirectivityRecords(Collection<DirectivityRecord> newRecords) {
        recordsTheta.addAll(newRecords);
        recordsTheta.sort(thetaComparator);
        recordsPhi.addAll(newRecords);
        recordsPhi.sort(phiComparator);
    }

    public static class ThetaComparator implements Comparator<DirectivityRecord>, Serializable {

        @Override
        public int compare(DirectivityRecord o1, DirectivityRecord o2) {
            final int thetaCompare = Double.compare(o1.theta, o2.theta);
            if(thetaCompare != 0) {
                return thetaCompare;
            }
            return Double.compare(o1.phi, o2.phi);
        }

    }

    public static class PhiComparator implements Comparator<DirectivityRecord>, Serializable {

        @Override
        public int compare(DirectivityRecord o1, DirectivityRecord o2) {
            final int phiCompare = Double.compare(o1.phi, o2.phi);
            if(phiCompare != 0) {
                return phiCompare;
            }
            return Double.compare(o1.theta, o2.theta);
        }

    }
    public static class DirectivityRecord {
        private double theta;
        private double phi;
        private double[] attenuation;

        public DirectivityRecord(double theta, double phi, double[] attenuation) {
            this.theta = theta;
            this.phi = phi;
            this.attenuation = attenuation;
        }

        public double getTheta() {
            return theta;
        }

        public double getPhi() {
            return phi;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DirectivityRecord record = (DirectivityRecord) o;
            return Double.compare(record.theta, theta) == 0 &&
                    Double.compare(record.phi, phi) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(theta, phi);
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "DirectivityRecord{theta=%.2f (%.2g°)" +
                            ", phi=%.2f (%.2g°) , attenuation=%s}", theta, Math.toDegrees(theta), phi, Math.toDegrees(phi),
                    Arrays.toString(attenuation));
        }

        public double[] getAttenuation() {
            return attenuation;
        }
    }
}
