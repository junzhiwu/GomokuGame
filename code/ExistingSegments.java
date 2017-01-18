	import java.util.*;
	import java.awt.Point;
	
	//first build class of segments, then class of existing segments for a board
	public class ExistingSegments implements Cloneable{
		
		/****************************************************************************************************************************/ 
	    //use segment class (below) create existingSegments class
		/****************************************************************************************************************************/ 
	    ArrayList<segment> segmentList;  //List of segments player currently sees
	    
	    public ExistingSegments(){
	        segmentList = new ArrayList<segment>();
	    }
	    
	    public int size(){
	        return segmentList.size();
	    }
	    
	    //Get list of segments
	    public ArrayList<segment> getList(){
	        return segmentList;
	    }
	    
	    public void addSegment(segment seg){
	        segmentList.add(seg);
	    }
	   
	    public void removeSegment(segment seg){
	        segmentList.remove(seg);
	    }
	    
	    public ExistingSegments clone(){
	        ExistingSegments clone = new ExistingSegments();
	        for (segment i: this.getList())
	            clone.addSegment(i.clone());
	        return clone;
	    }
	    
	    //Consolidates existingSegments to rid redundant segments
	    public void consolidate(){
	        //record the segments to delete during iteration
	        ArrayList<segment> toDelete = new ArrayList<segment>();
	        
	        for (int i = 0; i < size(); i++){
	            for (int j = i; j < size(); j++){
	                segment that = segmentList.get(i);
	                segment other = segmentList.get(j);
	                //Checks for redundant segments
	                if (that.equivalent(other) && !that.equals(other))
	                    toDelete.add(other);
	                //Merge the segments that intersect, belong to the same player and have the same orientation 
	                if (!that.equivalent(other) && that.belongsTo() == other.belongsTo() && that.intersect(other) && that.getOrientation() == other.getOrientation()){     
	                    //System.out.println("MATCH" + " " + that.getOrientation() + " " +other.getOrientation());
	                    that.merge(other);
	                    toDelete.add(other);
	                }
	            }
	        }
	        for (segment k:toDelete) removeSegment(k);
	    }
	    
	    //Maps a Tile into a list of line segments
	    //Rely on valid list of empty Tiles to not add redundant points
	    public static boolean mapTile(Point p, ExistingSegments segList, byte player){
	        int xCoord = (int)p.getX();
	        int yCoord = (int)p.getY();
	        
	        //boolean to see if Point p has been attached to any existing segment
	        boolean extended = false;
	        
	        //Creates putative point positions to consider
	        Point NW = new Point(xCoord-1, yCoord+1);
	        Point N = new Point(xCoord, yCoord+1);
	        Point NE = new Point(xCoord+1, yCoord+1);
	        Point W = new Point(xCoord-1, yCoord);
	        Point E = new Point(xCoord+1, yCoord);
	        Point SW = new Point(xCoord-1, yCoord-1);
	        Point S = new Point(xCoord, yCoord-1);
	        Point SE = new Point(xCoord+1, yCoord-1);
	        
	        //Iterates through the given list of existing segments and checks for contact.
	        //Since we're modifying the list, we iterate over a clone
	        ArrayList<segment> copy = (ArrayList<segment>)segList.getList().clone();
	        for (segment i: copy){
	            if (i.belongsTo() == player){
	                //Scans around point to see if adjacent segments exist
	                if (i.contains(NE) || i.contains(SW)){
	                    //If point is added and matched orientation of extant segment, simply extend
	                    extended = true;
	                    if (i.contains(NE)) {
	                        segment toAdd = new segment(p, player);
	                        toAdd.extend(NE, 'P');
	                        segList.addSegment(toAdd);
	                    } if (i.contains(SW)) {
	                        segment toAdd = new segment(p, player);
	                        toAdd.extend(SW, 'P');
	                        segList.addSegment(toAdd);
	                    } 
	                }   
	                
	                if (i.contains(NW) || i.contains(SW)){
	                    //If point is added and matched orientation of extant segment, simply extend
	                    extended = true;
	                    if (i.contains(NW)) {
	                        segment toAdd = new segment(p, player);
	                        toAdd.extend(NW, 'N');
	                        segList.addSegment(toAdd);
	                    }
	                    if (i.contains(SE)) {
	                        segment toAdd = new segment(p, player);
	                        toAdd.extend(SE, 'N');
	                        segList.addSegment(toAdd);
	                    }
	                }             
	                
	                if (i.contains(W) || i.contains(E)){
	                    extended = true;
	                    if (i.contains(W)) {
	                        segment toAdd = new segment(p, player);
	                        toAdd.extend(W, 'H');
	                        segList.addSegment(toAdd);
	                    }
	                    if (i.contains(E)) {
	                        segment toAdd = new segment(p, player);
	                        toAdd.extend(E, 'H');
	                        segList.addSegment(toAdd);
	                    }
	                }
	                       
	                if (i.contains(N) || i.contains(S)){
	                    extended = true;
	                    if (i.contains(N)) {
	                        segment toAdd = new segment(p, player);
	                        toAdd.extend(N, 'V');
	                        segList.addSegment(toAdd);
	                    }
	                    if (i.contains(S)) {
	                        segment toAdd = new segment(p, player);
	                        toAdd.extend(S, 'V');
	                        segList.addSegment(toAdd);
	                    }
	                }
	            }
	        }
	        //If point has not extended any extant segments, then place it on its own into list of segments
	        if (!extended){
	            segment toAdd = new segment(p, player);
	            segList.addSegment(toAdd);
	        } 
	        segList.consolidate();
	        Collections.sort(segList.getList());
	        return extended;
	    }
	    
	    /****************************************************************************************************************************/
	    static class segment implements Comparable<segment> { 
	        HashSet<Point> line;  //Line segment   
	        ArrayList<Point> threats; //Lists potential threats available to segment (mainly open ends to add onto)
	        ArrayList<Point> farSideCombo; //Lists points required to be clear to fully realize a - - O O - O - combo
	        byte belongsTo;     //Associated player
	        char orientation;    //Orientation of segment
	        Point left;
	        Point right;
	        
	        public segment(Point p, byte player){
	            line = new HashSet<Point>();
	            line.add(p);
	            threats = new ArrayList<Point>();
	            farSideCombo = new ArrayList<Point>();
	            
	            int xCoord = (int)p.getX();
	            int yCoord = (int)p.getY();
	            
	            Point N = new Point(xCoord, yCoord +1);
	            Point E = new Point(xCoord+1, yCoord);
	            Point S = new Point(xCoord, yCoord-1);
	            Point W = new Point(xCoord-1, yCoord);
	            
	            Point farL = new Point(xCoord, yCoord + 2);
	            Point farR = new Point(xCoord, yCoord - 2);
	            
	            threats.add(N);
	            threats.add(E);
	            threats.add(S);
	            threats.add(W);
	            
	            farSideCombo.add(farL);
	            farSideCombo.add(farR);	            
	        
	            belongsTo = player;
	            left = p;
	            right = p;
	        }
	        
	        //Get the set of points in segment
	        public HashSet<Point> getPoints(){
	            return line;
	        }
	        
	        //Return list of potential threat spaces
	        public ArrayList<Point> getThreats(){
	            return threats;
	        }
	        
	        //Get length of segment
	        public int getLength(){
	            return line.size();
	        }
	        
	        //Returns orientation of line segment
	        public char getOrientation(){
	            return orientation;
	        }
	        
	        
	        //Player to which segment belongs to
	        public byte belongsTo(){
	            return belongsTo;
	        }
	        
	       //Get rightmost/highest point in segment
	        public Point getRightEnd(){
	            return right;
	        }
	        
	        //Get leftmost/lowest point in segment
	        public Point getLeftEnd(){
	            return left;
	        }     
	        
	        //high threat: near left and near right
	        //Returns number of open Threat[1] or Threat[2] if it be realized on given board
	        public int highThreatNum(BoardModel state){
	         int count = 2;
	         byte opponent = belongsTo() == 1?(byte)2:(byte)1;
	
	         if(!state.valid(getThreats().get(1)) || state.getSpace(getThreats().get(1)) == opponent) count--;
	         if(!state.valid(getThreats().get(2)) || state.getSpace(getThreats().get(2)) == opponent) count--;
	         
	         return count;
	        }
	        
	        //low threat: far left and far right
	        //Returns number of open Threat[0] or Threat[3] if it be realized on given board
	        public int lowThreatNum(BoardModel state){
	         int count = 2;
	         byte opponent = (belongsTo() == 1?(byte)2:(byte)1);
	
	         if (!state.valid(getThreats().get(0)) || state.getSpace(getThreats().get(0)) == opponent ) count--;
	         if (!state.valid(getThreats().get(3)) || state.getSpace(getThreats().get(3)) == opponent ) count--;
	         
	         return count;
	        }
	        
	        //Adds point to segment
	        public void addPoint(Point p){
	            line.add(p);
	        }  
	        //Extends the segment with a Point
	        //Directions are horizontal, vertical, positive diagonal and negative diagonal
	        //Should only ever be called in valid mapping, extending segment in valid direction
	        public void extend(Point p, char dir){
	        	// insert point and identify direction
	            this.addPoint(p);
	            this.orientation = dir;
	            
	            //Updates endpoints of segment
	            //Update right end only when newX > x and (newX = x and newY > y) 
	            if(p.getX() > right.getX() || (p.getX() == right.getX() && p.getY() > right.getY())){
	            	right = p;
	            }
	            //Updates left endpoint only when newX < x and (newX = x and newY < y) 
	            if(p.getX() < left.getX() || (p.getX() == left.getX() && p.getY() < left.getY())) {
	            	left = p;
	            }
	            int leftX = (int)left.getX();
	            int leftY = (int)left.getY();
	            int rightX = (int)right.getX();
	            int rightY = (int)right.getY();
	            
	           
	            //Defines new potential threats based on orientation
	            //Threats actually dependent on length of segment, but factor that into calculations later
	            Point farLeft, nearLeft, nearRight, farRight;
	            Point comboLeft, comboRight;
	            
	            if (dir == 'H'){
	                farLeft = new Point(leftX-2, leftY);
	                nearLeft = new Point(leftX-1, leftY);
	                nearRight = new Point(rightX+1, rightY);
	                farRight = new Point(rightX+2, rightY);
	                
	                comboLeft = new Point(leftX-3, leftY);
	                comboRight = new Point(rightX+3, rightY);
	            }
	            else if (dir == 'V'){
	                farLeft = new Point(leftX, leftY-2);
	                nearLeft = new Point(leftX, leftY-1);
	                nearRight = new Point(rightX, rightY+1);
	                farRight = new Point(rightX, rightY+2);
	                
	                comboLeft = new Point(leftX, leftY - 3);
	                comboRight = new Point(rightX, rightY + 3);
	            }
	            else if (dir == 'P'){
	                farLeft = new Point(leftX-2, leftY-2);
	                nearLeft = new Point(leftX-1, leftY-1);
	                nearRight = new Point(rightX+1, rightY+1);
	                farRight = new Point(rightX+2, rightY+2);
	                
	                comboLeft = new Point(leftX-3, leftY-3);
	                comboRight = new Point(rightX+3, rightY+3);
	            }
	            else if (dir == 'N'){
	                farLeft = new Point(leftX-2, leftY+2);
	                nearLeft = new Point(leftX-1, leftY+1);
	                nearRight = new Point(rightX+1, rightY-1);
	                farRight = new Point(rightX+2, rightY-2);
	                
	                comboLeft = new Point(leftX-3, leftY+3);
	                comboRight = new Point(rightX+3, rightY-3);
	            }
	            
	            else{
	                farLeft = new Point(leftX, leftY);
	                nearLeft = new Point(leftX, leftY);
	                nearRight = new Point(rightX, rightY);
	                farRight = new Point(rightX, rightY);
	                
	                comboLeft = new Point(leftX, leftY);
	                comboRight = new Point(rightX, rightY);
	            }
	            
	            //Update threat spaces
	            threats.clear();
	            threats.add(farLeft);
	            threats.add(nearLeft);
	            threats.add(nearRight);
	            threats.add(farRight);
	            
	            farSideCombo.clear();
	            farSideCombo.add(comboLeft);
	            farSideCombo.add(comboRight);
	        }
	        
	        //Merges two segments. Should only be called where there is an intersection
	        //AND in same direction AND belong to same player
	        //THIS segment will be extended
	        public void merge(segment other){
	            for(Point i: other.getPoints()){
	                if(!this.contains(i)) this.extend(i, getOrientation());
	            }
	        }
	        
	        //Checks if segment contains Point p
	        public boolean contains(Point p){
	            return line.contains(p);
	        }
	        
	        //Checks if two segments intersect by checking their points
	        public boolean intersect(segment other){
	            boolean intersect = false;
	            for (Point i: other.getPoints()){
	                if (this.contains(i)) intersect = true;
	            }
	            return intersect;
	        }
	        //Checks if two segments are equivalent
	        //The check is based on fact that if two segments have same endpoints, same orientation and same length
	        //They must be equivalent
	        public boolean equivalent(segment other){
	            return this.belongsTo() == other.belongsTo() && this.getRightEnd() == other.getRightEnd() && this.getLeftEnd() == other.getLeftEnd();        
	        }

	        
	        //Clones segment
	        public segment clone(){
	        	Point p = new Point(getLeftEnd());
	        	segment clone = new segment(p, this.belongsTo());
	        	for(Point i:this.getPoints()){
	        		Point temp = new Point(i);
	        		clone.extend(temp, this.getOrientation());
	        	}
	        	return clone;
	        }
	        
	        //Prints segment Points to console
	        public void print(){
	            StringBuilder toPrint = new StringBuilder();
	            for (Point i: getPoints())
	                toPrint.append(i.toString());
	            System.out.println(getOrientation() +" " + toPrint.toString() + " Belongs to: " + belongsTo() );
	        }
	       
	        
	        //Returns True if segment is deadend
	        public boolean dead(BoardModel state){            
	        	//If immediate threat spaces blocked off, segment not useful
	            if (highThreatNum(state) == 0) return true;
	            byte opponent = belongsTo() == 1?(byte)2:(byte)1;
	            
	            //If segment boxed in, either by board or opponent, segment is dead end
	            if ((!state.valid(getThreats().get(0)) || state.getSpace(getThreats().get(0)) == opponent) && ( !state.valid(getThreats().get(2)) || state.getSpace(getThreats().get(2)) == opponent)) return true;
	            if ((!state.valid(getThreats().get(1)) || state.getSpace(getThreats().get(1)) == opponent) && ( !state.valid(getThreats().get(3)) || state.getSpace(getThreats().get(3)) == opponent)) return true;
	            
	            return false;
	        }
	        
	      //Returns True if segment has one side blocked off
	        public boolean zombie(BoardModel state){            
	        	//If immediate threat spaces blocked off, segment not useful
	            if (highThreatNum(state) == 1) return true;	            
	            return false;
	        }
	        
	        //Checks potentialCombothreat
	        //Such as - O O - O -
	        public boolean comboThreat(BoardModel state){
	            byte opponent = belongsTo() == 1?(byte)2:(byte)1;
	            
	            //Check - O - O O -  or - O O - O -
	            if (getLength() == 2){
		        	if (highThreatNum(state) == 2 && state.valid(getThreats().get(0)) && state.getSpace(getThreats().get(0)) == belongsTo() &&
		        			state.valid(getThreats().get(1)) && state.getSpace(getThreats().get(1)) == (byte)0 && 
		        				state.valid(farSideCombo.get(0)) && state.getSpace(farSideCombo.get(0)) == (byte)0) return true;
			        if (highThreatNum(state) == 2 && state.valid(getThreats().get(3)) && state.getSpace(getThreats().get(3)) == belongsTo() && 
			        		state.valid(getThreats().get(2)) && state.getSpace(getThreats().get(2)) == (byte)0 &&
			        			state.valid(farSideCombo.get(1)) && state.getSpace(farSideCombo.get(1)) == (byte)0) return true;
	            }
	            
	            //Check O - O O O or O O O - O
	            if (getLength() == 3){
//	            if(getLength() == 2 || getLength() ==3){
	            	if (state.valid(getThreats().get(0)) && state.getSpace(getThreats().get(0)) == belongsTo() &&
		        			state.valid(getThreats().get(1)) && state.getSpace(getThreats().get(1)) != opponent) return true;
			        if (state.valid(getThreats().get(3)) && state.getSpace(getThreats().get(3)) == belongsTo() && 
			        		state.valid(getThreats().get(2)) && state.getSpace(getThreats().get(2)) != opponent) return true;
	            }
	        	return false;   
	        }	        
	        
	        //Descending ordering
	        public int compareTo(segment p) {
	        	if(this.getLength() == p.getLength()) return 0;
	        	if(this.getLength() > p.getLength()) return -1;
	        	else return +1;
	        } 			  
	    }
	    
	    public static void main(String[] args){    
	        byte one = 1;
	        Point a = new Point(0,0);
	        Point b = new Point(1,1);
	        Point c = new Point(2,2);
	        byte two = 2;
	        Point x = new Point(0,1);
	        Point y = new Point(0,2);
	        Point z = new Point(0,3);
	        
	        ExistingSegments over = new ExistingSegments();
	        
	        //first should be positive diagonal
	        segment first = new segment(a, one);
	        first.extend(b, 'P');
	        first.extend(c, 'P');
	        
	        segment f = new segment(a, one);
	        f.extend(b, 'P');
	        f.extend(c, 'P');
	        
	        //second should be a vertical segment
	        segment second = new segment(x,two);
	        second.extend(y, 'V');
	        second.extend(z, 'V');
	        
	        //third is a segment to be merged with first
	        Point d = new Point(3,3);
	        Point e = new Point(4,4);
	        segment third = new segment(c, one);
	        third.extend(d, 'P');
	        third.extend(e, 'P');
	        
	        Point n = new Point(1,2);
	        Point m = new Point(3,2);
	        
	        //Fourth is a negative diagonal segment
	        Point t = new Point(6,6);
	        Point u = new Point(7,5);
	        segment fourth = new segment(t, two);
	        fourth.extend(u, 'N');
	        
	        //Fifth is a horizontal segment
	        Point v = new Point (7,7);
	        Point w = new Point(8,7);
	        segment fifth = new segment(v, two);
	        fifth.extend(w, 'H');
	        
	        System.out.println("Testing merge and endpoints");
	        first.merge(third);
	        
	        System.out.println(first.getLength() + "\n " + first.getRightEnd() + " " + first.getLeftEnd());
	        for(Point i: first.getPoints()) 
	            System.out.println(i);
	        
	        over.addSegment(first);
	        over.addSegment(second);
	        
	        //Testing clone
	        System.out.println("Testing cloning: Expect outputs 2 and 1");
	        ExistingSegments under = over.clone();
	        System.out.println(under.size());
	        under.removeSegment(first);
	        System.out.println(under.size());
	        
	        //Testing segment contains and interesect method
	        System.out.println("Testing contains and intersect");
	        System.out.println(first.contains(a) + " " + first.contains(b) + " "+ first.contains(c));
	        System.out.println(second.contains(x) + " " + second.contains(y) + " "+ second.contains(z));
	        System.out.println(first.intersect(second));
	        
	        //Testing threats from segment
	        System.out.println("Threat spaces for vertical line 'Second':");
	        for(Point i: second.getThreats()) System.out.println(i.toString());
	
	        System.out.println("Threat spaces for Positive diagonal line 'First':");
	        for(Point i: first.getThreats()) System.out.println(i.toString());
	
	        System.out.println("Threat spaces for Negative diagonal line 'Fourth':");
	        for(Point i: fourth.getThreats()) System.out.println(i.toString());
	
	        System.out.println("Threat spaces for Horizontal diagonal line 'Fifth':");
	        for(Point i: fifth.getThreats()) System.out.println(i.toString());
	        
	        //Testing behavior of existingSegments
	        System.out.println("Testing behaviors of existingSegmens: Expect output 2, 4 and 5" + " \n" +over.size());
	        for(segment i:over.getList()){i.print();}
	        
	        mapTile(n, over, one);
	        System.out.println("Map n" + " "+ over.size());
	        for(segment i:over.getList()){i.print();}
	        
	        mapTile(m,over,one); //Produces redundant segments
	        System.out.println("Map m" +" " + over.size());
	        for(segment i:over.getList()){i.print();}
	        
	        //Test consolidation of map
	        over.consolidate();
	        System.out.println("Testing consolidation of map: Expect output 5" +"\n "+ over.size());
	        for(segment i:over.getList()){i.print();}
	        
	        //Benchmark data structure by adding a full board of i x j pieces
	        System.out.println("Existing Segments benchmark");
	        long startTime = System.currentTimeMillis();
	        ExistingSegments test = new ExistingSegments();
	        
	        //MAX Board is 45 by 45
	        int k = 3;
	        for (int i = 0; i < k; i++){
	            for (int j = 0; j < k; j++){
	                Point add = new Point(i,j);
	                mapTile(add, test, one);
	                
	//                //Checking segments of the list
	//                System.out.println("Turn " + i +  " Segments in list: ");
	//                for (segment o: test.getList()){
	//                    o.print();
	//                }
	            }
	            System.out.println("Memory for Column " + i + " KB: " + (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024);
	        }
	        long endTime   = System.currentTimeMillis();
	        long totalTime = endTime - startTime;
	        System.out.println("Total time to populate board: " + totalTime);
	        System.out.println("Average time per mapping: " + totalTime/(k*k));
	        System.out.println(test.size());
	
	        //Checking segments of the list
	        System.out.println("Segments in list: ");
	        for (segment i: test.getList()){
	        i.print();
	        }            
	    }
	}
	
