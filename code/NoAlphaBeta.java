import java.awt.Point;
import java.util.*;

//GAMMA CHANGES
//Prepared and added in mentalMap clones for use in IDS
//Implementing evaluation function

//Noticed that during evaluation, values returned were inaccurate
//Realized that player was not properly mapping its own moves to the mental map
//Altered the mapping accordingly

//EPSILON CHANGES
//BUG WHERE AI CHOOSES TO FORM AN OPEN THREE   - - O O O - - segment rather than blocking
//opponent's combo threat    - O O - O - 
//Need to adjust AI for this, or revise comboThreat function in segment class

//LAMBDA CHANGES
//With Pierre's help, able to hammer out some issues in IDS caused by integration of mental map
//Addressed issue with AI choosing optimal moves
//Issue was in ordering of evaluation function and shortcircuiting the return value too early before fully evaluating state of board and its threats
//Currently, AI performs strongly on Phillipp's machine

public class NoAlphaBeta extends CKPlayer {
	
	int nodeEvaluated = 0;
	
    ArrayList<Point>    EmptyTiles;//Set up an arraylist of Points which represent empty tiles on the game board.
    ArrayList<Integer>  TileValues;//Set up an arraylist of Integers which represent the found values of different moves. This is for sorting and determining the best course of action.
    
    ArrayList<Point>   CloneTiles  = new ArrayList<Point>();
    ArrayList<Integer> CloneValues = new ArrayList<Integer>();
    
    Boolean abPruning = true;         //Set up to be true.
    int prunes = 0;            //For analysis. Replace all System.out with System.out
    Boolean implementQuiescence = false;
    
    //Mental map of extant segments
    ExistingSegments mentalMap;
    
    //Tracks opponent
    byte opponent;
    
    public NoAlphaBeta(byte player, BoardModel state) {
        super(player, state);
        mentalMap = new ExistingSegments();
		opponent = player==((byte)1)?(byte)2: (byte)1;

        teamName = "Gomoku Player Mu";
    }
        
    @Override
    public Point getMove(BoardModel state) {
     //Get move to be played and update mental map
        Point toPlay = getMove(state, 5000);
        ExistingSegments.mapTile(toPlay, mentalMap, player);
        
        return toPlay;
    }
    
    @Override
    public Point getMove(BoardModel state, int deadline) {
    	nodeEvaluated = 0;
    	
    //randomized first move for eval	
     if(state.getLastMove() == null){
    	 
    	 int x = (int)(Math.random() * 15);
     	 int y = (int)(Math.random() * 15);

//    	 int x = (int)state.height/2;
//    	 int y = (int)state.height/2;
    	
     	 return new Point(x,y);
    	 }
     
     //Account for opponent's last move by adding it to mental map
     //Makes call to static method mapTile
     if(state.getLastMove() != null) {
      ExistingSegments.mapTile(state.getLastMove(), mentalMap, opponent);
     }
     
        double startTime = System.currentTimeMillis();
        if (deadline <= 0) {deadline = 5000;}
        
        EmptyTiles = new ArrayList<Point>();     //Set up an arraylist of Points which represent empty tiles on the game board.
        TileValues = new ArrayList<Integer>();   //Set up an arraylist of Integers which represent the found values of different moves. This is for sorting and determining the best course of action.//////This describes the tile's value. Used at the root level to sort the various lines.
        
        //Step 1: Upload the empty tiles on the board into the array list EmptyTiles, and resize TileValues to the number of empty tiles.
        UploadEmptyTiles(state);
        
        //Step 2: Analyze the first depth in detail.
        Point 	BestPoint = EmptyTiles.get(0);
        int 	BestValue = Integer.MIN_VALUE;
        int 	win = Integer.MAX_VALUE; //Our goal is to WIN. If we can win on the first move, so be it.
        
        for (Point p: EmptyTiles) {
        	//Quick win check on the next turn. (This is only done on the first depth, to save time)
        	BoardModel cloneW = state.placePiece(p, player);
            if (cloneW.winner() == player)
            	return p;
        }
        
        for (Point p: EmptyTiles) { //for each empty tile,
            	//First, make sure that either gravity is off, or that the move is legitimate.
                if (state.valid(p))
                {   
                	BoardModel clone = state.placePiece(p, player);   		//Create a clone of the board but with a player piece at the empty tile
                	                    
                    //Quick loss check on the next turn.
                    //If opponent can win off this, block it
                    BoardModel cloneD = state.placePiece(p,  opponent);
                    if (cloneD.winner() == opponent)          
                    {
                    	return p;
                    }

                    ExistingSegments cloneMap = mentalMap.clone();		//Clone both the physical map and the mental map
                    ExistingSegments.mapTile(p, cloneMap, player);		//Map the tile according to the new clones' newest player piece.
                    
                    int value = EvaluateBoard(clone, cloneMap, opponent);	//Evaluate the board. The board will output 2^30 or 2^-30 if it finds a winning move.
                    
                    TileValues.set(EmptyTiles.indexOf(p), (Integer) value);	//Record the value of that piece

                    if (value == win)
                    {
                    	//System.out.println("VALUEWIN");
                        return p;			//Any winning move will be instantly played. ExistingSegments may be ignored since the player instantly wins.
                    }
                    //After placement, record best point so far
                    if ((BestValue < value))            //Make sure to tag the best value FOR PLAYER
                    {
                        BestPoint = new Point(p);
                        BestValue = value;
                    }
                }
            }
        
//        System.out.println("Mu - Current Point: " + BestPoint + " Current Value: " + BestValue + " at Depth 1 (Node Searches Pruned: 0) Time: " + (int)(System.currentTimeMillis() - startTime));
        
        //Step 3: MergeSort To merge sort, take small subsets and choose elements so that they are in order now. Do this for doubly increasing subsets.
//        MergeSort(startTime, deadline);
        
        //Step 4: Initiate IDS.
        BoardModel 				state_clone;
        ExistingSegments	map_clone;
        
        Point temp_point;
        int depth = 2;

        int Size  = EmptyTiles.size();
        int alpha;
        int beta;
        
        Point 	CurrentBestPoint = new Point(BestPoint); //Stores copy of BestPoint from first depth analysis
        Integer CurrentBestValue = BestValue; //Some situations using the heuristic evaluation have the same exact answer
        
        boolean new_value_in = false;	//In this case, the answer must be erased, or it will not work.
        int value;
        int same_value = 0;				//Boolean will be false if every returned value is the same.
        int best_depth = 1;
        int best_prunes = 0;
        
        while ((int)(System.currentTimeMillis() - startTime) < deadline - 100 && depth <= Size)
        {
        	new_value_in = false; //This determines if a new value was evaluated.
        	
        	BestValue	= Integer.MIN_VALUE;
        	BestPoint	= EmptyTiles.get(0);
        	alpha		= Integer.MIN_VALUE;
        	beta		= Integer.MAX_VALUE;
        	prunes		= 0;
        	//For every empty tile
        	for (int i = 0; i < Size; i++)   
        	{
        		if ((int)(System.currentTimeMillis() - startTime) > deadline - 100) {break;}     //Stop if the deadline is met.
        		
        		//Create a new state where the empty tile is used by the current player (note that this ends their turn)
        		state_clone = state.placePiece(EmptyTiles.get(i),player);    
        		
        		map_clone = mentalMap.clone();
        		ExistingSegments.mapTile(EmptyTiles.get(i), map_clone, player);
        		
        		temp_point = EmptyTiles.get(i);
        		EmptyTiles.remove(i);								//Remove move to avoid redundant inclusion in recursive IDS calls
        	
        		//Analyze using iterative deepening search, considering responses from opponent
        		value = IDS(state_clone, map_clone, alpha, beta, opponent, depth-1, startTime, deadline);
        		        			                    
        		TileValues.set(i, value);                //Update played move's value
        		EmptyTiles.add(i, temp_point);                 	   //Re-add removed play earlier
        		
    			if (alpha < value) {alpha = value;}             //Update alpha or beta, so that these values can be passed down to other branches.
    			if (BestValue < value)                				//Note that no alpha-beta pruning is done at the root.
    			{
    				BestPoint = EmptyTiles.get(i);               			//Be sure to update best point if necessary.
    				BestValue = value;                				//As well as its corresponding value.
    			}
    			
    			//This step, while seemingly unnecessary, is to prevent a previously acceptable move from being replaced with
    			//A dumb one just because the last depth that was explored had values that were simultaneously the same, forcing a single move.
        		if (i == 0) {same_value = value;} //It sometimes occurs on white's first move, when they can't honestly tell.
        		else if (same_value != value) {new_value_in = true;}
        	}
        	
        	//Perform mergesort after successfully updating the values. 
//        	MergeSort(startTime, deadline);
        	
        	if ((int)(System.currentTimeMillis() - startTime) >= deadline - 100) {break;} //An incomplete answer cannot be accepted.
        	
        	//Update the values accordingly.
        	if (new_value_in)
        	{
        		CurrentBestPoint = new Point(BestPoint);
        		CurrentBestValue = BestValue;
        		best_depth = depth;
        		best_prunes = prunes;
        	}
        	
        	depth++; //Now that the search tree for this cutoff depth level has been successfully searched, we move to the next cutoff depth level.
        }
        //CurrentBestPoint will contain the best possible point that was evaluated by the system.TileValues.get(EmptyTiles.indexOf(CurrentBestPoint))
        System.out.println("NoAlphaBeta - Best Point: " + CurrentBestPoint + " Best Value: " + CurrentBestValue + " Max Depth: " + best_depth);
        //System.out.println("# of subtress pruned: " + best_prunes);
        //Update mental map with chosen move
        ExistingSegments.mapTile(CurrentBestPoint, mentalMap, player);
        
        System.out.println("Number of nodes evaluated: " + nodeEvaluated);
        
        return CurrentBestPoint;
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Adds every empty tile to the EmptyTiles array list.
    //Also adds a zero value for every empty tile to TileValues, to be used later.
    public void UploadEmptyTiles(BoardModel state)
    {
        for (int x = 0; x < state.getWidth(); x++)
        {
            for (int y = 0; y < state.getHeight(); y++)
            {
                if (state.getSpace(x,y) == 0)                       //Find every empty space in the board...
                {
                    EmptyTiles.add(new Point(x,y));                 //And add these points to Empty Tiles arraylist.
                    TileValues.add((Integer) 0);     				//Also increase the size of Tile Values by 1. Default value: zero.
                }
            }
        }
    }
    
    //Sorts the arrays to make alpha-beta pruning more frequent in future iterations.
    public void MergeSort(double startTime, int deadline)
    {
        CloneTiles.clear();        //Erase the clone arrays used to transfer data.
        CloneValues.clear();
        int index;
        int left;
        int left_end = 0;
        int right;
        int right_end = 1;
        int stepSize = 1;
        if (true) //Sort the greatest elements must be at the start of the array.
        {
            while (stepSize < EmptyTiles.size())
            {
                if ((int)(System.currentTimeMillis() - startTime) > deadline - 100) {break;}
                index = 0;
                while (index < EmptyTiles.size())
                {
                    left = index;
                    if (left + stepSize >= EmptyTiles.size()) //If the end was reached a long time ago,
                    {
                        while (left != EmptyTiles.size()) //Copy over the elements into the copy listArray.
                        {
                            CloneTiles.add(new Point(EmptyTiles.get(left)));
                            CloneValues.add(new Integer(TileValues.get(left)));
                            left++;
                        }
                    }
                    else
                    {
                        left_end = left + stepSize;
                        right    = left + stepSize;
                        right_end = (right+stepSize < EmptyTiles.size())?(right+stepSize):(EmptyTiles.size());
                        while (left < left_end && right < right_end)
                        {
                            if (TileValues.get(left) > TileValues.get(right))
                            {
                                CloneTiles.add(new Point(EmptyTiles.get(left)));
                                CloneValues.add(new Integer(TileValues.get(left)));
                                left++;
                            }
                            else
                            {
                                CloneTiles.add(new Point(EmptyTiles.get(right)));
                                CloneValues.add(new Integer(TileValues.get(right)));
                                right++;
                            }
                        }
                        while (left < left_end)
                        {
                            CloneTiles.add(new Point(EmptyTiles.get(left)));
                            CloneValues.add(new Integer(TileValues.get(left)));
                            left++;
                        }
                        while (right < right_end)
                        {
                            CloneTiles.add(new Point(EmptyTiles.get(right)));
                            CloneValues.add(new Integer(TileValues.get(right)));
                            right++;
                        }
                    }
                    index += (stepSize*2);
                }
                for (int i = 0; i < CloneTiles.size(); i++)  //Copy over the more sorted list to the real arrays.
                {
                    EmptyTiles.set(i, new Point(CloneTiles.get(i)));
                    TileValues.set(i, new Integer(CloneValues.get(i)));
                }
                CloneTiles.clear();        //Erase the clone arrays used to transfer data.
                CloneValues.clear();
                stepSize *= 2;         //Double the step size
            }
        }
        CloneTiles.clear();
        CloneValues.clear();
    }
    
    //Iterative Deepening Search
    public int IDS(BoardModel state, ExistingSegments map, int alpha, int beta, byte current_player, int depth, double startTime, int deadline)
    {
    	nodeEvaluated++;
    	//DEBUG
//    	System.out.println("Prune: " + prunes + " Alpha: " + alpha + " Beta: " + beta);

        if ((int)(System.currentTimeMillis() - startTime) > deadline - 100) return (current_player == player)?(Integer.MIN_VALUE):(Integer.MAX_VALUE); //If the deadline is reached, Quit now
        
        //At a frontier node, evaluate and return the result, UNLESS quiescence is activated and applies to a really close evaluation.
        int ObservationalBound = 128;
        if (depth == 0)                    
        {
//        	System.out.println("TimeBefore: " + (int)(System.currentTimeMillis() - startTime));
            //implementQuiescence = false;      								//Secondary variable
            int result = EvaluateBoard(state, map, current_player);             //Return the value of the node.
            if (implementQuiescence)
            {
            	if (result == Integer.MIN_VALUE || result == Integer.MAX_VALUE)
            		{return result;}
            	else if (result <= Integer.MIN_VALUE+ObservationalBound || result >= Integer.MAX_VALUE-ObservationalBound)
            		{depth++;} //We implement quiescence as follows: If we think we are REALLY close to discovering the true value, we go one more depth.
        	}
//        	System.out.println("TimeAfter : " + (int)(System.currentTimeMillis() - startTime));
            return result;
        }
        else if (Winner(state,map,current_player) != 0)
        {
        	return Winner(state, map, current_player);
        }
        
        int i = 0;
        int value;
        int best_value = (current_player == player?(Integer.MIN_VALUE):(Integer.MAX_VALUE));       //Similar to the first iteration, but with alpha-beta pruning
        
        //MergeSort(startTime, deadline);

        while ((System.currentTimeMillis() - startTime < deadline - 100) && i < EmptyTiles.size())
        {	
        	BoardModel state_clone = state.placePiece(EmptyTiles.get(i),current_player);
        	
        	ExistingSegments map_clone = map.clone();
        	ExistingSegments.mapTile(EmptyTiles.get(i), map_clone, current_player);
        	
        	Point temp_point = EmptyTiles.get(i);
        	EmptyTiles.remove(i);
        	
        	//Iterative Deepening search on opposite player
        	value = IDS(state_clone, map_clone, alpha, beta, (current_player == player?opponent:player), depth-1, startTime, deadline);
        	EmptyTiles.add(i,temp_point);
	                
        	//At MAX NODE
        	if (current_player == player)
        	{
        		if (best_value < value) {best_value = value;}
        		if (alpha < value)   {alpha = value;}
        	}
        	
        	//AT MIN NODE
        	else if (current_player == opponent)
        	{
        		if (best_value > value) {best_value = value;}
        		if (beta > value) {beta = value;}
        	}
        	
        	//if (depth == 1) {System.out.println("Value of this move is " + value);}
        	
        	if (alpha >= beta && abPruning == true)            //If at any point alpha >= beta, LEAVE.
        	{
        		//if (depth == 1){System.out.println("Pruning in progress: " + alpha + " >= " + beta);}
        		prunes++;
        		return (current_player == player?(Integer.MAX_VALUE):(Integer.MIN_VALUE)); //our goal is to make the move as unappetizing to the PREVIOUS state
        	}
        	i++;
        }        
        
        //if (depth == 1){System.out.println(best_value + " Ding");}
        
        return best_value;//else	return (current_player == player)?(Integer.MIN_VALUE):(Integer.MAX_VALUE); //In the unlikely but possible event that search terminates without looking
    }

    //The board evaluation method. Evaluate board for current-player
    public int EvaluateBoard(BoardModel state, ExistingSegments map, byte current_player)
    {         
        //The idea is to use the existingSegment list and iterate through it to scan for any segments of length five. If so, return infinity if belongs to player one, else negative infinity for player two
        //Similarly, the existence of a segment of length 4 with both ends open OR multiple length 4s with an open end is also a win for the associated player
        //A length 4 with one end open signifies a single threat and would weigh a high number, though less than infinity.
        //The existence of multiple segments of length 3 with both ends open signifies multiple threats to opponent and should be weighted accordingly ie 100 * sigma((#length 3s) ^ (their open threats)
        
        //Segments of length 2 are weighted with 10*# length 2s
        //Segments of length 1 are weighted with 1
        
        //The main challenge is constructing the existingSegments list so it remains robust. It will be memory intensive.
    	
    	byte warning = 0;
    	double value = 0;
    	//Track number of potential threats and wins 

    	//Your threats
    	int win = 0;			// Winning segment O O O O O
    	int openFour = 0;		//Open four, extending into win - O O O O -
    	int combo = 0;			//Can combine segments into open four  - - O O - O -
    	int combo5 = 0;			//Can create winning segment - O O O - O -
    	int singleFour = 0;		//A Four with one end free, to create threat  X O O O O -
    	int openThree = 0;		//A Three with high threats free and one low threat, create open four    - - O O O - - or - - O O O - X

    	//Opponents threats
    	int winOpposing = 0;
    	int openFourOpposing = 0;
    	int comboOpposing = 0;
    	int combo5Opposing = 0;
    	int singleFourOpposing = 0;		
    	int openThreeOpposing = 0;		
    	
    	//Evaluation to be returned at end if no threat sequences can be realized
    	int playerVal = 0;
    	int opponentVal = 0;
    	
    	//Loop through the given mental map to check for potentially useful segments
    	//Check for opponent advantage first, then own
    	//Capitalize on advantage if possible, else handle opponent's threat
    	if (current_player == opponent)
    	{
        	for (ExistingSegments.segment i: map.getList())
        	{
        		if (i.dead(state)) continue;
    			if (i.belongsTo() == opponent)
    			{
    				if (i.getLength() == 5) winOpposing++;
    				if (i.getLength() == 4 && i.highThreatNum(state) > 1) openFourOpposing++;
    				if (i.getLength() == 4 && i.highThreatNum(state)== 1) singleFourOpposing++;
    				if (i.getLength() == 3 && i.highThreatNum(state) > 1 && i.lowThreatNum(state) > 0) openThreeOpposing++;
    				if (i.getLength() == 2 && i.comboThreat(state)) comboOpposing++;
    				if (i.getLength() == 3 && i.comboThreat(state)) combo5Opposing++;
    				opponentVal -= 2* Math.pow(4,i.getLength()) * Math.pow(3,i.highThreatNum(state)) * Math.pow(2,i.lowThreatNum(state));	
    			}
    			if (i.belongsTo() == player)
    			{
    				if (i.getLength() == 5) win++;
    				if (i.getLength() == 4 && i.highThreatNum(state) > 1) openFour++;
    				if (i.getLength() == 4 && i.highThreatNum(state)== 1) singleFour++;
    				if (i.getLength() == 3 && i.highThreatNum(state) > 1 && i.lowThreatNum(state) > 0) openThree++;
    				if (i.getLength() == 2 && i.comboThreat(state)) combo++;
    				if (i.getLength() == 3 && i.comboThreat(state)) combo5++;
    				playerVal += Math.pow(4,i.getLength()) * Math.pow(3,i.highThreatNum(state)) * Math.pow(2,i.lowThreatNum(state));	
    			}
        	}
//        	//Opponent Win in one turn:
//        	if (openFourOpposing + singleFourOpposing + combo5Opposing > 0) return Integer.MIN_VALUE; //If the opponent can win in a single move and it is their turn, they win.
//        	//Player Win in two turns:
//        	if (openFour > 0 || singleFour + combo5 >= 4) return Integer.MAX_VALUE-1;
//        	//If Opponent can win in three turns: Opponent can create a 4-chain with two open ends in two moves and Player cannot win on their turn
//        	if (openThreeOpposing + comboOpposing > 0 && singleFour + combo5 == 0) return Integer.MIN_VALUE+2;
//        	//If none of the above then we have an evaluation situation:
        	
        	if (winOpposing > 0) return Integer.MIN_VALUE;
        	if (win > 0) return Integer.MAX_VALUE;
        	
        	//Opponent wins in at most one
        	if (openFourOpposing == 1 || singleFourOpposing + combo5Opposing > 0) return Integer.MIN_VALUE;
        	//You win in one
        	if (openFour == 1) return Integer.MAX_VALUE;
        	if (singleFour + combo5 > 0) return Integer.MAX_VALUE - 10;
        	
        	
        	
        	//Opponent wins in at most two. You can't win in one
        	if (openFourOpposing > 0  && openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE;
        	if (singleFourOpposing + combo5Opposing > 1) return Integer.MIN_VALUE + 10;

        	
        	
        	//Opponent can create open four before player can win.
        	if (openThreeOpposing + comboOpposing > 1 && openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE;
        	//You create open four before opponent can win
        	if (openThree + combo > 1 && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE;


        	
        	//Opponent can create open four before player can win.
        	if (openThreeOpposing + comboOpposing == 1 && openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE + 10;
        	//You create open four before opponent can win
        	if (openThree + combo == 1 && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE -10;

        	
        	
        	
        	
        	// You win in at most two. Opponent can't win in one
        	if (openFour > 0 && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE;
        	if (singleFour + combo5 > 1 && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE - 10;     
        	
        	
        	
        	
        	//Opponent guarantee win in 3. You can't win in one
        	if (openThreeOpposing > 1 && openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE + 300;
        	//Guarantee win in 3. Opponent can't win in one
        	if (openThree > 1 && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE - 300;
        	
        	
        	
        	//Opponent wins in [1,3], You can't win in one
        	if (openThreeOpposing  == 1 && openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE + 300;
        	//You win in [1,3], Opponent can't win in one
        	if (openThree  == 1 && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE - 300;
        	
        	opponentVal *= Math.pow(8,(singleFourOpposing + combo5Opposing));
        	opponentVal *= Math.pow(64, openFourOpposing);
        	opponentVal *= Math.pow(8, (openThreeOpposing + comboOpposing));
        	
        	playerVal *= Math.pow(16,(singleFour + combo5));
        	playerVal *= Math.pow(128, openFour);
        	playerVal *= Math.pow(4, (openThree + combo));
        	
        	return playerVal+opponentVal;
    	}
    	
    	if (current_player == player)
    	{
    		for (ExistingSegments.segment i: map.getList()){
				if (i.dead(state)) continue;
				
				if (i.belongsTo() == opponent){
					if (i.getLength() == 5) winOpposing++;
					if (i.getLength() == 4 && i.highThreatNum(state) > 1) openFourOpposing++;
					if (i.getLength() == 4 && i.highThreatNum(state) == 1) singleFourOpposing++;
					if (i.getLength() == 3 && i.comboThreat(state)) openThreeOpposing++;
    				if (i.getLength() == 2 && i.comboThreat(state)) comboOpposing++;
    				if (i.getLength() == 3 && i.comboThreat(state)) combo5Opposing++;
					opponentVal -= Math.pow(4,i.getLength()) * Math.pow(3,i.highThreatNum(state)) * Math.pow(2,i.lowThreatNum(state));	
				}
				if (i.belongsTo() == player){
					if (i.getLength() == 5) win++;
					if (i.getLength() == 4 && i.highThreatNum(state) > 1) openFour ++;;
					if (i.getLength() == 4 && i.highThreatNum(state) == 1) singleFour++;
					if (i.getLength() == 3 && i.highThreatNum(state) > 1 && i.lowThreatNum(state) > 0) openThree++;;
    				if (i.getLength() == 2 && i.comboThreat(state)) combo++;
    				if (i.getLength() == 3 && i.comboThreat(state)) combo5++;
					playerVal += 2 * Math.pow(4,i.getLength()) * Math.pow(3,i.highThreatNum(state)) * Math.pow(2,i.lowThreatNum(state));	
				} 
    		}
//    		//Player Win in one turn:
//        	if (openFour + singleFour + combo5 > 0) return Integer.MAX_VALUE; //If the opponent can win in a single move and it is their turn, they win.
//        	//Opponent Win in two turns:
//        	if (openFourOpposing > 0 || singleFourOpposing + combo5Opposing >= 4) return Integer.MIN_VALUE+1;
//        	//If player can win in two turns: Player can create a 4-chain with two open ends in two moves and opponent cannot win on their turn
//        	if (openThree + combo > 0 && singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE-2;
        	
			//Ordering is based on distance from an end
        	//Bracketed numbers is minimum and maximum to guaranteed win. [1,2] is minimum win in 1, maximum win in 2

    		//Player win in 1, Opponent can't win
    		if (win > 0) return Integer.MAX_VALUE;
    		if (winOpposing > 0) return Integer.MIN_VALUE;
    		
    		//Player win in 2. Opponent't can't win in 1
        	if (openFour > 0 && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE;
        	if (singleFour + combo5 > 0 && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE - 10;
        	
        	if (openFourOpposing > 0 && openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE;
        	if (singleFourOpposing + combo5Opposing > 1 &&  openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE + 10;

        	
        	//You create open four before opponent can win
        	if (openThree + combo == 1 && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE -10;
        	//Opponent can create open four before player can win.
        	if (openThreeOpposing + comboOpposing == 1 && openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE + 10;

        	
        	// You win in at most two. Opponent can't win in one
        	if (openFour > 0  && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE;
        	if (singleFour + combo5 > 1  && openFourOpposing + singleFourOpposing + combo5Opposing == 0)return Integer.MAX_VALUE - 10;
        	//Opponent wins in at most two. You can't win in one
        	if (openFourOpposing > 0 && openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE;
        	if (singleFourOpposing + combo5Opposing > 1 && openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE + 10;
        	
        	//You create open four before opponent can win
        	if (openThree + combo > 1 && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE;
        	//Opponent can create open four before player can win.
        	if (openThreeOpposing + comboOpposing > 1 && openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE;

        	//Guarantee win in 3. Opponent can't win in one
        	if (openThree > 1 && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE - 300;
        	//Opponent guarantee win in 3. You can't win in one
        	if (openThreeOpposing > 1 && openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE + 300;

        	//You win in [1,3], Opponent can't win in one
        	if (openThree  == 1 && openFourOpposing + singleFourOpposing + combo5Opposing == 0) return Integer.MAX_VALUE - 300;
        	//Opponent wins in [1,3], You can't win in one
        	if (openThreeOpposing  == 1 && openFour + singleFour + combo5 == 0) return Integer.MIN_VALUE + 300;
        	
        	opponentVal *= Math.pow(16,(singleFourOpposing + combo5Opposing));
        	opponentVal *= Math.pow(128, openFourOpposing);
        	opponentVal *= Math.pow(4, (openThreeOpposing + comboOpposing));
        	
        	playerVal *= Math.pow(8,(singleFour + combo5));
        	playerVal *= Math.pow(64, openFour);
        	playerVal *= Math.pow(6, (openThree + combo));

        	//If none of the above then we have an evaluation situation:
        	return playerVal+opponentVal;
		}
    		//DEBUG: Check segments seen by evaluation
//    		System.out.println("Segment belongs to: " + i.belongsTo() + " Value: " +  value);
//    		i.print();
//    		System.out.println("End");
    	

    	//DEBUG: Check evaluation of board    	
//    	System.out.println("Value " + valueSum);

        return 0;
    }
    //*****************************************************************************************************************  

    
    //Determines if there is a winning state or not. 0 for no wins, infinity for player wins, -infinity for opponent wins.
    public int Winner(BoardModel state, ExistingSegments map, byte current_player)
    {
    	for (ExistingSegments.segment i: map.getList())
    	{
    		if (!i.dead(state) && i.getLength() == state.getkLength())
    		{            	
    			return (i.belongsTo() == player)?Integer.MAX_VALUE:Integer.MIN_VALUE;
    		}
    	}
    	return 0;
    }
    //*****************************************************************************************************************   
}
