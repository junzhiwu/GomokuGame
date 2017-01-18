//import connectK.CKPlayer;
//import connectK.BoardModel;


import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class McMaster_And_Shih extends CKPlayer {
    ArrayList<Point>    EmptyTiles = new ArrayList<Point>();     //Set up an arraylist of Points which represent empty tiles on the game board.
    ArrayList<Integer>  TileValues = new ArrayList<Integer>();   //Set up an arraylist of Integers which represent the found values of different moves. This is for sorting and determining the best course of action.
    ArrayList<Point>   CloneTiles  = new ArrayList<Point>();
    ArrayList<Integer> CloneValues = new ArrayList<Integer>();
	ArrayList<Point> WinSpaces1  = new ArrayList<Point>();
	ArrayList<Point> WinSpaces2  = new ArrayList<Point>();
    Boolean abPruning = true;									//Set up to be true.
    int prunes = 0;												//For analysis. Replace all System.out with System.out
    Boolean implementQuiescence = false;
	public McMaster_And_Shih(byte player, BoardModel state)
	{
		super(player, state);
		teamName = "Shih and McMaster";
	}
	
	@Override
	public Point getMove(BoardModel state)
	{
		return getMove(state, 5000);
	}
	
	@Override
	public Point getMove(BoardModel state, int deadline)
	{
		double startTime = System.currentTimeMillis();
		if (deadline <= 0) {deadline = 5000;}
	    EmptyTiles = new ArrayList<Point>();     //Set up an arraylist of Points which represent empty tiles on the game board.
	    TileValues = new ArrayList<Integer>();   //Set up an arraylist of Integers which represent the found values of different moves. This is for sorting and determining the best course of action.//////This describes the tile's value. Used at the root level to sort the various lines.
	    
	    //Step 1: Upload the empty tiles on the board into the array list EmptyTiles, and resize TileValues to the number of empty tils.
	    UploadEmptyTiles(state);
	    
	    //Step 2: Analyze the first depth in detail. The very first is the most important.
	    Point BestPoint = EmptyTiles.get(0);
	    int   BestValue = ((player==(byte)1)?(-9999999):(9999999));
	    Point  sabotage = EmptyTiles.get(0);
	    Boolean screwed = false;
	    if (player == (byte) 1)//For player 1, so that we don't have to ask "are you player 1 or 2" over and over.
	    {
	    	for (int i = 0; i < EmptyTiles.size(); i++)//for each empty tile,
	    	{
//	g
	    		if (EmptyTiles.get(i).getY() == 0 || state.getSpace((int)EmptyTiles.get(i).getX(),(int)EmptyTiles.get(i).getY()-1) != 0)
	    		{
	    			BoardModel clone = state.placePiece(EmptyTiles.get(i), (byte) 1);   //Create a clone of the board but with a player 1 piece at the empty tile
	    			int value = EvaluateBoard(clone,(byte)2);							//Evaluate the board. The board will output 2^30 or 2^-30 if it finds a winning move.
		    		TileValues.set(i, (Integer) value);
	    			if (value == 9999999)
	    			{
	    				return EmptyTiles.get(i);										//If the AI finds a winning move for player 1, and it is player 1's turn, player 1 moves there.
	    			}
	    			if (BestValue < value)												//Make sure to tag the best value
	    			{
	    				BestPoint = new Point(EmptyTiles.get(i));
	    				BestValue = value;
	    			}
	    			clone = state.placePiece(EmptyTiles.get(i), (byte) 2);				//Now check to make sure that your opponent will not win on the next turn. (This is only done on the first depth, to save time)
	    			if (clone.winner() == (byte) 2) 									//In the event that player 2 can win on their next turn,
	    	    	{
	    				screwed = true;     											//Set a flag that explains that if there are no winning moves,
	    				sabotage = new Point(EmptyTiles.get(i));						//then the AI for player 1 MUST move here to prevent losing.(If there are two winning moves, the AI loses anyway.)
	    			}
	    		}
	    	}
	    }
	    else //This one is for player 2
	    {
	    	for (int i = 0; i < EmptyTiles.size(); i++)//for each empty tile,
	    	{
//g	    	
	    		if (EmptyTiles.get(i).getY() == 0 || state.getSpace((int)EmptyTiles.get(i).getX(),(int)EmptyTiles.get(i).getY()-1) != 0)
	    		{
	    			BoardModel clone = state.placePiece(EmptyTiles.get(i), (byte) 2);   //Create a clone of the board but with a player 2 piece at the empty tile
		    		int value = EvaluateBoard(clone,(byte)1);							//Evaluate the board. The board will output 2^30 or 2^-30 if it finds a winning move.
		    		TileValues.set(i, (Integer) value);
		    		if (value == -9999999)
		    		{
		    			return EmptyTiles.get(i);										//If the AI finds a winning move for player 2, and it is player 2's turn, player 2 moves there.
		    		}
		    		if (BestValue > value)												//Make sure to tag the best value
	    			{
	    				BestPoint = new Point(EmptyTiles.get(i));
	    				BestValue = value;
	    			}
		    		clone = state.placePiece(EmptyTiles.get(i), (byte) 1);				//Now check to make sure that your opponent will not win on the next turn.
		    		if (clone.winner() == (byte) 1)											//In the event that player 1 can win on their next turn,
		    		{
		    			screwed = true;     											//Set a flag that explains that if there are no winning moves,
		    			sabotage = EmptyTiles.get(i);									//then the AI for player 2 MUST move here to prevent losing.(If there are two winning moves, the AI loses anyway.)
		    		}
	    		}
	    		else
	    		{
	    			TileValues.set(i, (player==1)?(-9999999):(9999999)); //Make this one of the worst moves ever - the illegal move. Do not do this in IDS
	    		}
	    	}
	    	
	    }
	    if (screwed)													//And after searching for winning moves, if it happened to find a losing one, Then it has to move there to prevent losing.
	    {
	    	if (EmptyTiles.size() == 1)
	    	{
	    		return sabotage; //No need to panic, this is THE LAST TILE AND IT'S YOUR TURN.
	    	}
	    	for(int i = 0; i < EmptyTiles.size(); i++)	//The AI now desperately searches for a move that prevents the opponent from winning in one turn. This supercedes iterative deepening search
	    	{											//because of its vast importance, as all the planning in the world is worth nothing if your opponent wins.
    			Boolean test_bit = true;
	    		BoardModel HOPE_THIS_WORKS = state.placePiece(EmptyTiles.get(i),(player == (byte)1)?((byte)1):((byte)2));
	    		for (int j = 0; j < EmptyTiles.size(); j++)
	    		{
	    			if (i != j)
	    			{
	    				BoardModel DID_IT_WORK = HOPE_THIS_WORKS.placePiece(EmptyTiles.get(j),(player == (byte)1)?((byte)2):((byte)1));
	    				if (DID_IT_WORK.winner() == (player == ((byte)1)?((byte)2):((byte)1)))
	    				{
	    					test_bit = false; //Nope, the enemy can win.
	    					break;
	    				}
	    			}
	    		}
	    		if (test_bit == true)
	    		{
	    			return EmptyTiles.get(i);
	    		}
	    	}
	    	return sabotage;
		}
	    
	    //Step 3: MergeSort To merge sort, take small subsets and choose elements so that they are in order now. Do this for doubly increasing subsets.
	    MergeSort(startTime, deadline);

	    
	    BoardModel state_clone;
	    Point temp_point;
	    int depth = 2;
	    int Size  = EmptyTiles.size();
	    int alpha = 0;
	    int beta  = 0;
	    Point PreviousBestPoint = new Point(BestPoint);
		Integer PreviousBestValue = BestValue;			//Some situations using the heuristic evaluation have the same exact answer
		boolean new_value_in = false;					//In this case, the answer must be erased, or it will not work.
		int same_value = 0;								//Boolean will be false if every returned value is the same.
		int best_depth = 1;
	    prunes = 0;
	    
//g
	    while ((int)(System.currentTimeMillis() - startTime) < deadline - 100 && depth <= Size)
	    {
	    	new_value_in = false;
	    	BestValue = ((player==(byte)1)?((int)(-9999999)):((int)(9999999)));
		    alpha = (-9999999);
		    beta  = ( 9999999);
	    	for (int i = 0; i < Size; i++)																		//For every empty tile,
	    	{
	    		if ((int)(System.currentTimeMillis() - startTime) > deadline - 100)	{break;}					//Stop if the deadline is met.
	    		state_clone = state.placePiece(EmptyTiles.get(i),(byte)(player==((byte)1)?1:2));				//Create a new state where the empty tile is used by the current player (note that this ends their turn)
	    		temp_point = EmptyTiles.get(i);
	    		EmptyTiles.remove(i);																			//Removing the tile from consideration for this branch means that the program does not trip over itself.
	    		int Setup = IDS(state_clone, alpha, beta, (byte)(player==1?2:1), depth-1, startTime, deadline); //Analyze using iterative deepening search.
	    		TileValues.set(i, (Integer) Setup);																//Add this to the TileValues array, which is used for re-arranging the tiles for the next iteration.
	    		EmptyTiles.add(i, temp_point);																	//The item is added again. This saves time from "if(state.getSpace(EmptyTile.(i)) == 0)"
	    		if (player == (byte) 1)																			//I COULD create IDS1 and IDS2... but that's too redundant.
	    		{
	    			if (alpha < (int) Setup) {alpha = (int) Setup;}												//Update alpha or beta, so that these values can be passed down to other branches.
	    			if (BestValue < (int) Setup)																//Note that no alpha-beta pruning is done at the root.
	    			{
	    				BestPoint = EmptyTiles.get(i);															//Be sure to update best point if necessary.
	    				BestValue = (int) Setup;																//As well as its corresponding value.
	    			}
	    		}
	    		else
	    		{
	    			if (beta > (int) Setup) {beta = (int) Setup;}
	    			if (BestValue > (int) Setup)
	    			{
	    				BestPoint = EmptyTiles.get(i);
	    				BestValue = (int) Setup;
	    			}
	    		}
	    		
	    		if (i == 0)																						//And finally, make sure that there are at least two different values of all the node's successors.
	    		{
	    			same_value = Setup;
	    		}
	    		else
	    		{
	    			if (same_value != Setup)
	    				new_value_in = true;
	    		}
    		
	    	}
	    	MergeSort(startTime, deadline);
	    	if ((int)(System.currentTimeMillis() - startTime) >= deadline - 100) {break;} //An incomplete answer cannot be accepted.
	    	
	    	if (new_value_in)
			{
	    		PreviousBestPoint = new Point(BestPoint);
	    		PreviousBestValue = BestValue;
	    		best_depth = depth;
			}
	    	depth++;
	    }
	    
	    BestPoint = new Point(PreviousBestPoint);
	    BestValue = PreviousBestValue;
		depth = best_depth;
	    return BestPoint;
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
                    TileValues.add((Integer) 0);					//Also increase the size of Tile Values by 1. Default value: zero.
                }
	        }
	    }
    }
    
    //Sorts the arrays to make alpha-beta pruning more frequent in future iterations.
    public void MergeSort(double startTime, int deadline)
    {
    	CloneTiles.clear();								//Erase the clone arrays used to transfer data.
    	CloneValues.clear();
	    int index;
	    int left;
	    int left_end = 0;
	    int right;
	    int right_end = 1;
	    int stepSize = 1;
	    if (player == 1) //If the current player is 1, then the greatest elements must be at the start of the array.
	    {
		    while (stepSize < EmptyTiles.size())
		    {
		    	if ((int)(System.currentTimeMillis() - startTime) > deadline - 100)	{break;}
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
		    	for (int i = 0; i < CloneTiles.size(); i++)		//Copy over the more sorted list to the real arrays.
		    	{
		    		EmptyTiles.set(i, new Point(CloneTiles.get(i)));
		    		TileValues.set(i, new Integer(CloneValues.get(i)));
		    	}
		    	CloneTiles.clear();								//Erase the clone arrays used to transfer data.
		    	CloneValues.clear();
		    	stepSize *= 2;									//Double the step size
		    }
		}
	    else
	    {
		    while (stepSize < EmptyTiles.size())
		    {
		    	if ((int)(System.currentTimeMillis() - startTime) > deadline - 100)	{break;}
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
		    				if (TileValues.get(left) < TileValues.get(right))
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
		    	for (int i = 0; i < CloneTiles.size(); i++)		//Copy over the more sorted list to the real arrays.
		    	{
		    		EmptyTiles.set(i, new Point(CloneTiles.get(i)));
		    		TileValues.set(i, new Integer(CloneValues.get(i)));
		    	}
		    	CloneTiles.clear();								//Erase the clone arrays used to transfer data.
		    	CloneValues.clear();
		    	stepSize *= 2;									//Double the step size
		    	//System.out.println("Next Iteration");
		    }
		}
    	CloneTiles.clear();
    	CloneValues.clear();
    }
	
    //Iterative Deepening Search
	public int IDS(BoardModel state, int alpha, int beta, byte current_player, int depth, double startTime, int deadline)
	{
		if ((int)(System.currentTimeMillis() - startTime) > ((int)(deadline - 100))) {return 0;}	//If the deadline is reached, Quit now

		int p = state.winner();																		//And be sure to quit if this node is a finished state.
		if (p == (byte)1)
		{
			return  9999999;
		}
		else if (p == (byte)2)
		{
			return -9999999;
		}
		
		if (depth == 0)																				//At a frontier node,
		{
			implementQuiescence = false;						//Secondary variable
			int result = EvaluateBoard(state,current_player);												//Return the value of the node.
			if (result != -9999999 && result != 9999999 && implementQuiescence)
				depth++;
			else
				return result;
		}
		
		int i = 0;
		int value;
		int best_value = (current_player == (byte) 1)?(-9999999):(9999999);							//Similar to the first iteration, but with alpha-beta pruning
//g
		while ((System.currentTimeMillis() - startTime < deadline - 100) && i < EmptyTiles.size())
		{
    		BoardModel state_clone = state.placePiece(EmptyTiles.get(i),(byte)(current_player==((byte)1)?1:2));
    		Point temp_point = EmptyTiles.get(i);
    		EmptyTiles.remove(i);
    		value = IDS(state_clone, alpha, beta, (byte)(current_player==((byte)1)?2:1), depth-1, startTime, deadline);
    		EmptyTiles.add(i,temp_point);
    		if (current_player == (byte) 1)
    		{
    			if (best_value < value) {best_value = value;}
    			if (alpha < value) 		{alpha = value;}
    		}
    		else
    		{
    			if (best_value > value) {best_value = value;}
    			if (beta > value) {beta = value;}
    		}
    		if (alpha >= beta && abPruning == true)												//If at any point alpha >= beta, LEAVE.
    		{
    			prunes++;
    			break;
    		}
			i++;
		}
		if (i > 0)
		{
			return best_value;
		}
		else
		{
			return (current_player == (byte) 1)?(9999999):(-9999999);
		}
	}
	
	//The board evaluation method.
	public int EvaluateBoard(BoardModel state, byte current_player)
	{
		ArrayList<Point> EmptySpaces = new ArrayList<Point>();
		WinSpaces1  = new ArrayList<Point>();
		WinSpaces2  = new ArrayList<Point>();
		ArrayList<Integer> Segments_1 = new ArrayList<Integer>();
		ArrayList<Integer> Segments_2 = new ArrayList<Integer>();
		for (int i = 0; i <= state.getkLength(); i++)
		{
			Segments_1.add((Integer)0);
			Segments_2.add((Integer)0);
		}
		Boolean trick_win_1 = false; 	//Involves placing a piece in a situation where, on their turn, placing a piece here is an instant win.
		Boolean trick_win_2 = false; 	//GIVEN that the enemy cannot win first. Other than that, this results in an instant fork.
		int gCost       = 0;
        int current_cost= 0;
        int player_1_pieces = 0; //How many of player 1's pieces are on the current interval.
        int player_2_pieces = 0;
        int min_cost_1 = state.getWidth()*state.getHeight();
        int min_cost_2 = state.getWidth()*state.getHeight();
        int win_sum_1 = 0;
        int win_sum_2 = 0;
        int length = 0;//Length of segment that is being analyzed.
        //Row
        for (int y = 0; y < state.getHeight(); y++)
        {
        	gCost = 0;

        	current_cost = 0;
            player_1_pieces = 0;
            player_2_pieces = 0;
            length = 0;
            for (int x = 0; x < state.getWidth(); x++)					//Create a line segment
            {
                if (state.getSpace(x,y) == 1)							//Record any pieces on this segment
                {
                    player_1_pieces++;
                }
                else if (state.getSpace(x,y) == 2)
                {
                    player_2_pieces++;
                }
                else													//For empty spaces, add the cost of placing a piece.
                {
                	EmptySpaces.add(new Point(x,y));

                        current_cost++;
                    
                }
                length++;
                if (length > state.getkLength()) //Delete elements that make the segment too large.
                {
                    if (state.getSpace(x-state.getkLength(),y) == 1)		//Remove their values from the analysis as the tile is removed from consideration.
                    {
                        player_1_pieces--;
                    }
                    else if (state.getSpace(x-state.getkLength(),y) == 2)
                    {
                        player_2_pieces--;
                    }
                    else													//There is a trick to winning, unfortunately.
                    {
                    	EmptySpaces.remove(0);

                            current_cost--;
                        
                    }
                    length--;
                }

                if (length == state.getkLength())							//Once the segment reaches k,
                {
                	if (player_1_pieces == state.getkLength())				//Figure out if there is a winner
                	{
                		return 9999999;
                	}
                	if (player_2_pieces == state.getkLength())
                	{
                		return -9999999;
					}
                    if (player_2_pieces == 0)								//Or if this segment has one of the shortest ways to win.
                    {
                    	if (current_cost <= state.getkLength())
                    		Segments_1.set(current_cost-1, Segments_1.get(current_cost-1)+1);
                    	if (current_cost == 1) WinSpaces1.add(new Point(EmptySpaces.get(0))); //And record any potential wins.
                    	if (min_cost_1 > current_cost)
                    	{
                    		win_sum_1 = 1;
                    		min_cost_1 = current_cost;
                    	}
                    	else if (min_cost_1 == current_cost)
                    	{
                    		win_sum_1++;
                    	}
                    }
                    if (player_1_pieces == 0)
                    {
                    	if (current_cost <= state.getkLength())
                    		Segments_2.set(current_cost-1, Segments_2.get(current_cost-1)+1);
                    	if (current_cost == 1) WinSpaces2.add(new Point(EmptySpaces.get(0)));
                    	if (min_cost_2 > current_cost)
                    	{
                    		win_sum_2 = 1;
                    		min_cost_2 = current_cost;
                    	}
                    	else if (min_cost_2 == current_cost)
                    	{
                    		win_sum_2++;
                    	}
                    }
                }
            }
        }
        EmptySpaces.clear();												//Be sure to clean out empty spaces.
        //Column
        for (int x = 0; x < state.getWidth(); x++)
        {
        	gCost = 0;

        	current_cost = 0;
            player_1_pieces = 0;
            player_2_pieces = 0;
            length = 0;
            for (int y = 0; y < state.getHeight(); y++)
            {
                if (state.getSpace(x,y) == 1)
                {
                    player_1_pieces++;
                }
                else if (state.getSpace(x,y) == 2)
                {
                    player_2_pieces++;
                }
                else
                {
                	EmptySpaces.add(new Point(x,y));

                        current_cost++;
                    
                }
                length++;
                if (length > state.getkLength())
                {
                    if (state.getSpace(x,y-state.getkLength()) == 1)
                    {
                        player_1_pieces--;
                    }
                    else if (state.getSpace(x,y-state.getkLength()) == 2)
                    {
                        player_2_pieces--;
                    }
                    else
                    {
                    	EmptySpaces.remove(0);

                            current_cost--;

                    }
                    length--;
                }

                if (length == state.getkLength())
                {
                	if (player_1_pieces == state.getkLength())
                	{
                		return 9999999;
                	}
                	if (player_2_pieces == state.getkLength())
                	{
                		return -9999999;
					}
                	
                	if (player_2_pieces == 0)
                    {
                    	if (current_cost <= state.getkLength())
                    		Segments_1.set(current_cost-1, Segments_1.get(current_cost-1)+1);
                    	if (current_cost == 1) WinSpaces1.add(new Point(EmptySpaces.get(0)));
                    	if (min_cost_1 > current_cost)
                    	{
                    		win_sum_1 = 1;
                    		min_cost_1 = current_cost;
                    	}
                    	else if (min_cost_1 == current_cost)
                    	{
                    		win_sum_1++;
                    	}
                    }
                    if (player_1_pieces == 0)
                    {	
                    	if (current_cost <= state.getkLength())
                    		Segments_2.set(current_cost-1, Segments_2.get(current_cost-1)+1);
                    	if (current_cost == 1) WinSpaces2.add(new Point(EmptySpaces.get(0)));
                    	if (min_cost_2 > current_cost)
                    	{
                    		win_sum_2 = 1;
                    		min_cost_2 = current_cost;
                    	}
                    	else if (min_cost_2 == current_cost)
                    	{
                    		win_sum_2++;
                    	}
                    }
                }
//g
            }
        }
        EmptySpaces.clear();
        //SW-to-NE diagonal
        for (int a = 0 - state.getHeight()+1; a < state.getWidth(); a++)
        {
        	gCost = 0;

        	current_cost = 0;
            player_1_pieces = 0;
            player_2_pieces = 0;
            length = 0;
        	int x = a;
        	int y = 0;
        	while (x < 0)
        	{
        		x++;
        		y++;
        	}
            while (x < state.getWidth() && y < state.getHeight())
            {
                if (state.getSpace(x,y) == 1)
                {
                    player_1_pieces++;
                }
                else if (state.getSpace(x,y) == 2)
                {
                    player_2_pieces++;
                }
                else
                {
                	EmptySpaces.add(new Point(x,y));

                        current_cost++;
                    
                }
                length++;
                if (length > state.getkLength())
                {
                    if (state.getSpace(x-state.getkLength(),y-state.getkLength()) == 1)
                    {
                        player_1_pieces--;
                    }
                    else if (state.getSpace(x-state.getkLength(),y-state.getkLength()) == 2)
                    {
                        player_2_pieces--;
                    }
                    else
                    {
                    	EmptySpaces.remove(0);
                        
                    	current_cost--;

                    }
                    length--;
                }

                if (length == state.getkLength())
                {
                	if (player_1_pieces == state.getkLength())
                	{
                		return 9999999;
                	}
                	if (player_2_pieces == state.getkLength())
                	{
                		return -9999999;
					}
                	
                    if (player_2_pieces == 0)
                    {
                    	if (current_cost <= state.getkLength())
                    		Segments_1.set(current_cost-1, Segments_1.get(current_cost-1)+1);
                    	if (current_cost == 1) WinSpaces1.add(new Point(EmptySpaces.get(0)));
                    	if (min_cost_1 > current_cost)
                    	{
                    		win_sum_1 = 1;
                    		min_cost_1 = current_cost;
                    	}
                    	else if (min_cost_1 == current_cost)
                    	{
                    		win_sum_1++;
                    	}
                    }
                    if (player_1_pieces == 0)
                    {
                    	if (current_cost <= state.getkLength())
                    		Segments_2.set(current_cost-1, Segments_2.get(current_cost-1)+1);
                    	if (current_cost == 1) WinSpaces2.add(new Point(EmptySpaces.get(0)));
                    	if (min_cost_2 > current_cost)//state.getkLength()-player_2_pieces)
                    	{
                    		win_sum_2 = 1;
                    		min_cost_2 = current_cost;
                    	}
                    	else if (min_cost_2 == current_cost)
                    	{
                    		win_sum_2++;
                    	}
                    }
                }
                x++;
                y++;
        	}
        }
        EmptySpaces.clear();
        //NW-to-SE diagonal
        for (int a = 0 - state.getHeight()+1; a < state.getWidth(); a++)
        {
        	gCost = 0;

        	current_cost = 0;
            player_1_pieces = 0;
            player_2_pieces = 0;
            length = 0;
        	int x = a;
        	int y = state.getHeight()-1;
        	while (x < 0)
        	{
        		x++;
        		y--;
        	}
            while (x < state.getWidth() && y >= 0)
            {
                if (state.getSpace(x,y) == 1)
                {
                    player_1_pieces++;
                }
                else if (state.getSpace(x,y) == 2)
                {
                    player_2_pieces++;
                }
                else
                {
                	EmptySpaces.add(new Point(x,y));

                        current_cost++;

                }
                length++;
                if (length > state.getkLength())
                {
                    if (state.getSpace(x-state.getkLength(),y+state.getkLength()) == 1)
                    {
                        player_1_pieces--;
                    }
                    else if (state.getSpace(x-state.getkLength(),y+state.getkLength()) == 2)
                    {
                        player_2_pieces--;
                    }
                    else
                    {
                    	if (state.getSpace(x,y) == 0 && current_cost == 3)
                    	{
                    		if (current_player == 1 && player_2_pieces == 0)
                    			trick_win_1 = true;
                    		if (current_player == 2 && player_1_pieces == 0)
                    			trick_win_2 = true;
                    	}
                    	
                    	EmptySpaces.remove(0);
                        current_cost--;
                    }
                    length--;
                }
                
                if (length == state.getkLength())
                {
                	if (player_1_pieces == state.getkLength())
                	{
                		return 9999999;
                	}
                	if (player_2_pieces == state.getkLength())
                	{
                		return -9999999;
					}
                	if (player_2_pieces == 0)
                    {
                    	if (current_cost <= state.getkLength())
                    		Segments_1.set(current_cost-1, Segments_1.get(current_cost-1)+1);
                    	if (current_cost == 1) WinSpaces1.add(new Point(EmptySpaces.get(0)));
                    	if (min_cost_1 > current_cost)
                    	{
                    		win_sum_1 = 1;
                    		min_cost_1 = current_cost;
                    	}
                    	else if (min_cost_1 == current_cost)
                    	{
                    		win_sum_1++;
                    	}
                    }
                    if (player_1_pieces == 0)
                    {	
                    	if (current_cost <= state.getkLength())
                    		Segments_2.set(current_cost-1, Segments_2.get(current_cost-1)+1);
                    	if (current_cost == 1) WinSpaces2.add(new Point(EmptySpaces.get(0)));
                    	if (min_cost_2 > current_cost)//state.getkLength()-player_2_pieces)
                    	{
                    		win_sum_2 = 1;
                    		min_cost_2 = current_cost;
                    	}
                    	else if (min_cost_2 == current_cost)
                    	{
                    		win_sum_2++;
                    	}
                    }
                }
                x++;
                y--;
        	}
        }
        EmptySpaces.clear();
        
//g
        if (WinSpaces2.size() == 0 && current_player == 2)			//If player 2 does not have winning moves,
        for (int i = 0; i < WinSpaces1.size()-1; i++)
        {
        	for (int j = i+1; j < WinSpaces1.size(); j++)
    	    {
        		if (!WinSpaces1.get(i).equals(WinSpaces1.get(j)))	//And  player 1 has two, (which cannot be blocked with a single move)
        		{
        			return  9999999;								//Then player 1 wins, because player 2 cannot stop them.
        		}
    	    }
        }
        if (WinSpaces1.size() == 0 && current_player == 1) 			//If player 1 does not have winning moves,
        for (int i = 0; i < WinSpaces2.size()-1; i++)
        {
        	for (int j = i+1; j < WinSpaces2.size(); j++)
    		{
        		if (!WinSpaces2.get(i).equals(WinSpaces2.get(j)))	//And  player 2 has two,
        		{
        			return -9999999;								//Then player 1 wins.
        		}
    		}
        }
        
        
        if (current_player == 1 && win_sum_2 > 0)					//Players are important.
        {
        	min_cost_2--;											//Player 2 gets to decrease their min cost next turn.
        	if (min_cost_1 <= 2 && win_sum_1 >= 2 && (min_cost_2 > 1 || win_sum_2 == 0))//Similar to trick wins, but not as strong.
        		return  9999990;
        }
        else if (current_player == 2 && win_sum_1 > 0)
        {
        	min_cost_1--;
        	if (min_cost_2 <= 2 && win_sum_2 >= 2 && (min_cost_1 > 1 || win_sum_1 == 0))//New
        		return -9999990;
        }
        if (trick_win_1)//In the event of a trick win (but only after the AI has confirmed that the opponent cannot win on their next turn) This is one of the best states ever for a single player.
        	return  9999990;
        if (trick_win_2)
        	return -9999990;
        //Implementation of quiescence: if the opponent can win on their next turn, it may be prudent to look at one more depth...
        if ((current_player == 1 && WinSpaces2.size() > 1) || (current_player == 2 && WinSpaces1.size() > 1))
        {
        	implementQuiescence = true;
        }

        
        if (min_cost_1 <= state.getkLength())
        {
        	win_sum_1 += Segments_1.get(min_cost_1);
        }
        if (min_cost_2 <= state.getkLength())
        {
        	win_sum_2 += Segments_1.get(min_cost_2);
        }
        
        
        if (min_cost_1 > min_cost_2)						//player 2 winning
        {
        	if (min_cost_1 - min_cost_2 == 1)
//g
        		return (min_cost_2 - min_cost_1) * 10000 - (win_sum_2*100 - win_sum_1);
        	else
//g
        		return (min_cost_2 - min_cost_1) * 10000 - (win_sum_2*100);//else
        }
        else if (min_cost_1 < min_cost_2)					//player 1 winning
        {
        	if (min_cost_2 - min_cost_1 == 1)
//g
        		return (min_cost_2 - min_cost_1) * 10000 + (win_sum_1*100 - win_sum_2);
        	else
//g
        		return  (min_cost_2 - min_cost_1) * 10000 + (win_sum_1*100);
        }
        else
        {
//g
        		return (win_sum_1 - win_sum_2)*100;
		}
	} //Must count to see if BOTH sides win from a redundant, too-large situation. causes overload!
	
//g
}
