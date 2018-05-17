package universum.engine.resource;

import java.util.*;
import universum.bi.Location;
import universum.bi.Constants;

import universum.engine.Topology;
import universum.util.Util;

/**
 *
 * @author nike
 */
public class FairResourceControl extends DefaultResourceControl {
    float reg_data[][];
    float gold_data[][];    
    int idx_regular, idx_golden;

    public FairResourceControl() {
        super();
        reg_data = new float[3][];
        gold_data = new float[3][];        
    }

    public void init(Topology topo, Object ... args) {        
        if (args.length != 2 || 
            !(args[0] instanceof Integer) ||
            !(args[1] instanceof Integer)) {
            throw new IllegalArgumentException("must be 2 ints, regular and golden");
        }                
        this.reg = (Integer)args[0];
	this.golden = (Integer)args[1];
        
        // we split available energy budgets between sources here
        allocateAndSplit(reg, reg_data, 0, Constants.TOTAL_STARTED_REGULAR);
        allocateAndSplit(reg, reg_data, 1, Constants.TOTAL_GROWTH_REGULAR);
        scale(reg, reg_data, 0, 2, 
              Constants.TOTAL_STARTED_REGULAR, 
              Constants.TOTAL_CAPACITY_REGULAR);
        allocateAndSplit(golden, gold_data, 0, Constants.TOTAL_STARTED_GOLDEN);
        allocateAndSplit(golden, gold_data, 1, Constants.TOTAL_GROWTH_GOLDEN);
        scale(golden, gold_data, 0, 2, 
              Constants.TOTAL_STARTED_GOLDEN, 
              Constants.TOTAL_CAPACITY_GOLDEN);
        
        // and then add resources on the map
        super.init(topo, args);
    }

    private void allocateAndSplit(int num, float result[][], int idx, 
                                    float budget) {
        float d[] = result[idx] = new float[num];
       
        // algorithm idea by Andrei Pangin
        List<Float> tmp = new ArrayList<Float>();
        for (int i=0; i < num - 1; i++) {
            tmp.add(Util.frnd());
        }
        tmp.add(1.0f);
        Collections.sort(tmp);  
        
        float v1 = 0f;
        for (int i = 0; i < num; i++) {
            float v2 =  tmp.get(i);
            d[i] = (v2 - v1)*budget;
            v1 = v2;
        }
    }
    
    // somewhat stupid, but truly random is hard to do
    private void scale(int num, float result[][], 
                         int idx1, int idx2,
                         float budget1, float budget2) {
        float scaler = budget2 / budget1;
        result[idx2] = new float[num];
        for (int i=0; i<num; i++) {
            result[idx2][i] = result[idx1][i] * scaler;
        }
    }

    protected void addNextResource(Location loc, boolean golden) {
        if (golden) {
            addSource(loc, 
                      gold_data[0][idx_golden],
                      gold_data[1][idx_golden],
                      gold_data[2][idx_golden]);
            if (++idx_golden == this.golden) {
                gold_data = null;
            }
        } else {
            addSource(loc, 
                      reg_data[0][idx_regular],
                      reg_data[1][idx_regular],
                      reg_data[2][idx_regular]);
            if (++idx_regular == this.reg) {
                reg_data = null;
            }
        }
    }    
}
