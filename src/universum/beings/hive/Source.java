package universum.beings.hive;

import universum.bi.Location;

class Source implements Comparable {

	Location l;
	float rate;

	Source(Location l, float rate) {
		this.l = l;
		this.rate = rate;
	}
	
	public int compareTo(Object x) {
		float extRate = ((Source) x).rate;
		
		if (rate < extRate) {
			return -1;
		}
		
		if (rate > extRate) {
			return 1;
		}
		
		return 0;
	}
}
