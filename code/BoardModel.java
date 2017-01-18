
import java.awt.Point;
import java.util.*;

public class BoardModel {
	public static final int DEFAULT_WIDTH = 15;
	public static final int DEFAULT_HEIGHT = 15;
	public static final int DEFAULT_K = 5;

	public int width;
	public int height;
	public int kLength;
	
	public int spacesLeft;
	private int hash;
	private byte winner = -2;
	public java.awt.Point lastMove;
	
	public byte[][] pieces; //[column][row] 1, 2, or null for empty
	
	BoardModel(int width, int height, int k){//new board
		this.width = width;
		this.height = height;
		this.kLength = k;
		spacesLeft = width*height;
		hash = 0;
		pieces = new byte[width][height];
	}
	
	//returns a new board with the piece placed
	public BoardModel placePiece(java.awt.Point p, byte player){
		assert (pieces[p.x][p.y] == 0);
		java.awt.Point move = (Point) p.clone(); //*new place of piece by player
		BoardModel nextBoard = (BoardModel) this.clone();//*****

		nextBoard.lastMove = move;
		nextBoard.pieces[move.x][move.y] = player;//*set the player tag
		nextBoard.spacesLeft = spacesLeft - 1;
		return nextBoard;
	}
	
	@Deprecated
	static public BoardModel newBoard(int width, int height, int k){
		return new BoardModel(width, height, k);
	}
	
	static public BoardModel defaultBoard(){
		return new BoardModel(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_K);
	}
	
	public boolean valid(Point p){
		 return (p.x >= 0 && p.x < width && p.y >= 0 && p.y < height);
	}
	
	public byte getSpace(Point p){
		assert(p.x >= 0 && p.x < width);
		assert(p.y >= 0 && p.y < height);
		return pieces[p.x][p.y];
	}
	
	public byte getSpace(int x, int y){
		assert(x >= 0 && x < width);
		assert(y >= 0 && y < height);
		return pieces[x][y];
	}
	
	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getkLength() {
		return kLength;
	}

	public java.awt.Point getLastMove() {
		return lastMove;
	}
	
	public boolean hasMovesLeft(){
		return spacesLeft > 0;
	}
	
	//returns winner (1|2) if there is one, 0 if draw, else -1
	public byte winner(){
		uncached: if(winner == -2){
			for(int i=0; i<width; ++i){
				for(int j=0; j<height; ++j){
					//if the space previous is either not the same as current, empty, or OOB
					//while the next thing is the same AND not OOB
					//increment contiguous count
					//if count greater than k, return the winner
					//returns on first winning sequence found
					//searches to the right and up
					
					if(pieces[i][j] == 0){//*还没有置子
							continue;//move up
					}
					// i= 0  || i - 1 和i不相等，从左边第一个不相等的棋子开始
					if(i-1<0 || pieces[i-1][j] != pieces[i][j]){ //horizontal 
						int count = 1;
						while(i+count < width && pieces[i][j] == pieces[i+count][j]){
							++count;
							if(count >= kLength){
								winner = pieces[i][j];
								break uncached;
							}
						}
					}
					// i = 0 或者j = 0 或者 左下不等于当前，从当前左下第一个不相等的棋子开始
					if(i-1<0 || j-1<0 || pieces[i-1][j-1] != pieces[i][j]){ //diagonal, (j-1<0) needed to avoid OOB
						int count = 1;
						while(i+count < width && j+count < height && pieces[i][j] == pieces[i+count][j+count]){
							++count;
							if(count >= kLength){
								winner = pieces[i][j];
								break uncached;
							}
						}
					}
					// i = 0 或者 j = height 或者第一个左上不等于当前
					if(i-1<0 || j+1>=height || pieces[i-1][j+1] != pieces[i][j]){ //diagonal, (j+1>=height) needed to avoid OOB
						int count = 1;
						while(i+count < width && j-count >= 0 && pieces[i][j] == pieces[i+count][j-count]){
							++count;
							if(count >= kLength){
								winner = pieces[i][j];
								break uncached;
							}
						}
					}
					// j = 0  从第一个下面的棋子开始
					if(j-1<0 || pieces[i][j-1] != pieces[i][j]){ //vertical
						int count = 1;
						while(j+count < height && pieces[i][j] == pieces[i][j+count]){
							++count;
							if(count >= kLength){
								winner = pieces[i][j];
								break uncached;
							}
						}
					}
				}
			}
			winner = (byte) (hasMovesLeft()? -1: 0);
		}
		return winner;
	}
	
	List<Point> winningSpaces(){//记录赢的子的位置
		List<Point> ws = new ArrayList<Point>(kLength);
		for(int i=0; i<width; ++i){
			for(int j=0; j<height; ++j){
				//if the space previous is either not the same as current, empty, or OOB (out of bounds)
					//while the next thing is the same AND not OOB
						//increment contiguous count
							//if count greater than k, return the winner
				//returns on first winning sequence found
				//searches to the right and up
				
//	g
				if(pieces[i][j] == 0){
					continue;//move up
				}
				
				if(i-1<0 || pieces[i-1][j] != pieces[i][j]){ //horizontal
					int count = 1;
					while(i+count < width && pieces[i][j] == pieces[i+count][j]){
						++count;
						if(count >= kLength){
							for(int k=0; k<kLength; ++k)
								ws.add(new Point(i+k,j));
							return ws;
						}
					}
				}
				
				if(i-1<0 || j-1<0 || pieces[i-1][j-1] != pieces[i][j]){ //diagonal up, (j-1<0) needed to avoid OOB
					int count = 1;
					while(i+count < width && j+count < height && pieces[i][j] == pieces[i+count][j+count]){
						++count;
						if(count >= kLength){
							for(int k=0; k<kLength; ++k)
								ws.add(new Point(i+k,j+k));
							return ws;
						}
					}
				}
				
				if(i-1<0 || j+1>=height || pieces[i-1][j+1] != pieces[i][j]){ //diagonal down, (j+1>=height) needed to avoid OOB
					int count = 1;
					while(i+count < width && j-count >= 0 && pieces[i][j] == pieces[i+count][j-count]){
						++count;
						if(count >= kLength){
							for(int k=0; k<kLength; ++k)
								ws.add(new Point(i+k,j-k));
							return ws;
						}
					}
				}
				
				if(j-1<0 || pieces[i][j-1] != pieces[i][j]){ //vertical
					int count = 1;
					while(j+count < height && pieces[i][j] == pieces[i][j+count]){
						++count;
						if(count >= kLength){
							for(int k=0; k<kLength; ++k)
								ws.add(new Point(i,j+k));
							return ws;
						}
					}
				}
			}
		}
		return ws;
	}
	
	@Override
	public String toString(){
		String ret = "";
		for(int j = height-1; j>=0; --j){
			for(int i = 0; i < width; ++i){
				ret += pieces[i][j];
			}
			ret += "\n";
		}
		return ret;
	}
	
	@Override
	public BoardModel clone(){
		BoardModel cloned = new BoardModel(width, height, kLength);
		cloned.lastMove = this.lastMove;
		cloned.spacesLeft = this.spacesLeft;
		for(int i=0; i<width; ++i)
			for(int j=0; j<height; ++j)
				cloned.pieces[i][j] = this.pieces[i][j];
		return cloned;
	}
	
	//two games are equal if their shape, rules, and pieces are equal; last move does not matter
	@Override
	public boolean equals(Object o){
		if(!(o instanceof BoardModel))
			return false;
		BoardModel b = (BoardModel) o;

		if(this.width != b.width || this.height != b.height || this.kLength != b.kLength)
			return false;
	
		for(int i=0; i<width; ++i)
			for(int j=0; j<height; ++j)
				if(this.pieces[i][j] != b.pieces[i][j])
					return false;
		return true;
	}
	
	//hashCode operates on the same variables as equals()
	@Override
	public int hashCode(){
		if(hash == 0){
			hash ^= width ^ height ^ kLength;
			hash ^= Arrays.deepHashCode(pieces); 
		}
		return hash;
	}
}