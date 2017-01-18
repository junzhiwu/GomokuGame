# Gomoku Game (Java)
a course project for artificial intelligence

1) introduce a data structure segment implement comparable:
Member Fields:
point members lists, 
end points, 
length, 
player, 
orientation, 
threat lists (the nearest four position to the two ends given an orientation)
farSideCombo lists 

~+*o..o*+~: two + are low threats, two * are high threats, ~ are two farSide combo points that may contribute if the segment length is <=2

Member Function:
get function: 
getPoints (end point and point list), getThreats(list), getLength, getOrientation, belongsTo (getPlayer), get number of high/low threats
print() print the segment  
clone() get the clone of the segment
compare (segment A): compare the length of segment A with the object, < (-1), = (0), > (1)

addPoint (Point P): add a point into the segment
extend (Point P, orientation): extend the segment by adding a point following the given orientation, increase the length and update the endpoints, threat and farSideCombo list
merge (Segment A): merge the segment with A if they belong to the same player and have the same length, orientation and intersection point(s)


check function: 
contains(Point p): check if the segment contains point P
intersect(segment A): check if has intersection with segment A
equivalent(segment A): check if the segment is equivalent to segment A (same length, end points, player)

check the threat situation given a board state (the border information)
dead(BoardModel state): check if a segment is boxed by the border/opponent and no chance to win
zombie(BoardModel state): check if one side of a segment is blocked by the border/opponent 
comboThreat(BoardModel state): check if the segment has the win potent of - O - O O -  or - O O - O -


2) use ExistingSegments implements Cloneable
to record the segments in a boardState to speed up searching the optimal step 
get: segment list, size, clone() 
addSegment, removeSegment, 
conSolidate(): remove duplicate segments and merge two segments if they intersect, belongs to the same player and  have the same orientation
mapTile(Point p, ExistingSegments list, byte player): for a point to add, check if the point is to be extended by any segment in the list, if yes extend the segment then consolidate the list, else new segment in the list. Then reorder the list by descending length order





