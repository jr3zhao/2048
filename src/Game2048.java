   import java.lang.Math;
   import java.io.*;
	import java.util.Stack;
   import java.util.Arrays;

   public class Game2048 
   {

      final public static int LEFT_INPUT 	= 0;
      final public static int DOWN_INPUT 	= 1;
      final public static int RIGHT_INPUT = 2;
      final public static int UP_INPUT 	= 3;
		final public static int UNDO_INPUT  = 4;
   
      final public static int VALUE_GRID_SETTING 	= 0;
      final public static int INDEX_GRID_SETTING	= 1;
   	
		final public static int GRID_LENGTH = 4;
		final public static int GRID_WIDTH = 4;
		
		final public static int MAX_X_INDEX = 3;
		final public static int MAX_Y_INDEX = 3;
		
      final public static int WIN_CONDITION = 2048;
      
      public static int amount = 0;
      
      public static long score = 0;
      
      public static int x, y, num;
      //boolean array to make sure that three numbers don't combine into one, ex.  2 2 4  -> should be 4 4 not 8
      private boolean combined[][] = new boolean[GRID_LENGTH][GRID_WIDTH];
      //boolean variable that the program uses to check if it needs to generate a new slot or not
		private boolean doNewSlot = true;
      //boolean variable that the program uses to check if it needs to record the grid or not
      private boolean doNewRecord = true;
      
      private String GAME_CONFIG_FILE = "config/game_config.txt";
   
      private Game2048GUI gui;
		
   /* position [0][0] represents the Top-Left corner and
    * position [max][max] represents the Bottom-Right corner */
      private int grid [][] = new int[GRID_LENGTH][GRID_WIDTH];  
      //2D array representing the values of grid[][] in the past
		private int lastGrid[][] = new int[GRID_LENGTH][GRID_WIDTH];
      //Stacks to keep track of all the past grid values and as well as the scores
		Stack<int[][]> lastGrids = new Stack<int[][]>();
      Stack<Long> lastScores = new Stack<Long>();
      
		   	
		private int count = 0;


      private final int EMPTY_SLOT = -1;
   
      private int winningLevel;  
      private long currentScore;
      private int currentLevel;
   
   /**
    * Constructs Game2048 object.
    *
    *  gameGUI	The GUI object that will be used by this class.
    */   
      public Game2048(Game2048GUI gameGUI)
      {
         gui = gameGUI;
         
		   //initialized the grid to be all empty (-1)
			for(int i = 0;i<GRID_LENGTH;i++){
				for(int j = 0;j<GRID_WIDTH;j++){
					grid[i][j] = EMPTY_SLOT;
				}
			}
         //Why so cheap
         /*
         grid[3][3] = 1024;
         grid[3][2] = 512;
         grid[3][1] = 256;
         grid[3][0] = 128;
         grid[2][0] = 64;
         grid[2][1] = 32;
         grid[2][2] = 16;
         grid[2][3] = 8;
         grid[1][3] = 4;
         grid[1][2] = 2;
			*/

         
         //attempt to read from stream
			try{
				BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(GAME_CONFIG_FILE)));
				String gameMode = in.readLine();
            //convert the line to int
				winningLevel = Integer.parseInt(in.readLine());
				in.close();
			}catch(IOException iox){
				System.out.printf("%s", "Error reading file.");
			}
			currentScore = 0;
			currentLevel = 0;
			
         //At the beginning of the game, two random tiles will appear.
			newSlot();
		   newSlot();

      }
   
   
   /**
    * Place a new number tile on a random slot on the grid.
	 * This method is called every time a key is released.
    */		
      public void newSlot()
      {  
         //new slot will only appear if the grid isn't all filled
         //and if something was moved or combined in the last turn
			if(!allFilled() && doNewSlot){
            //Start to generate a random index between 0 and 3 
			   do{
				   x = 0 + (int)(Math.random() * ((MAX_X_INDEX - 0) + 1));
				   y = 0 + (int)(Math.random() * ((MAX_Y_INDEX - 0) + 1));
			   }while(grid[x][y] != EMPTY_SLOT); //exit if that position is empty
			   
            //generate a random number between 10 and 1
			   num = 1 + (int)(Math.random() * ((10 - 1) + 1));
			   
            //if that number is larger than two then our new slot will have value 2
			   if(num > 2) num = 0;
            //else new slot will have value 4 (4 has a lot less chance to occur than 2 in the original game)
			   else num = 1;
            
            //assign the generated values to grid
			   grid[x][y] = (int)Math.pow(2, num + 1);
            //tell the gui to make the tile show up
			   gui.setNewSlotBySlotIndex(x,y,num);
            //since a new slot is generated, the grid should be recorded
            doNewRecord = true;
         } 
         
         //check whether the player won or whether the game is over
         if(checkWin()){
            gui.showGameWon();
         } else if(checkGameOver()){
            gui.showGameOver();
         }

      }
   
   
   /**
    * Plays the game by the direction specified by the user.
	 * This method is called every time a button is pressed
	 */		
      public void play(int direction)
      {  
         //tell the program not to do a new slot unless otherwise specified
         doNewSlot = false;
         
         //if undo is input, call the undoGrid function
         if(direction == UNDO_INPUT && moved()){
				undoGrid();
            //tell the GUI to update everything on the screen
            gui.setGridByValue(grid);
			}else{
            //if it should record the grid, then do so
            if(doNewRecord) recordGrid();
			   //reset boolean variables to false, ex combined[][]
            resetBool();
            //slide the grid according to the direction
            doSlide(direction);
            //combine the values if there are any available according to the direction
            doCombine(direction);
            //slide the grid again after combining because some grids would be left behind after the slots beside it combine
            doSlide(direction);
            //tell the GUI to update the score on the screen
            gui.setScore(score);
            //if something was slid or combined then do a new slot
            if(!Arrays.deepEquals(grid, lastGrids.peek())){
               doNewSlot = true;
            }
            //else don't do a new slot and as well as don't record anything
            else{
               doNewRecord = false;
            }
            //tell the GUI to update everything on the screen
            gui.setGridByValue(grid);
            
         
			}

      }
		
      /**
       * Method that checks if a certain slot is slidable in a given direction, if yes, then return the amount that it can slide, else return 0
       */    
		public int isSlidable(int x, int y, int direction){
         //if direction is up
			if(direction == UP_INPUT){
				if(x == 0) return 0; //can't slide if it is in the uppermost row
            else if(x == 3){ //else if it is at the bottom
               //if everything above it is empty, then it can move to the very top, therefore return 3
               if(grid[x-3][y] == EMPTY_SLOT && grid[x-2][y] == EMPTY_SLOT && grid[x-1][y] == EMPTY_SLOT) return 3; 
               //else if there are two empty slots, then return 3
               else if(grid[x-2][y] == EMPTY_SLOT && grid[x-1][y] == EMPTY_SLOT) return 2; 
               //else if there are only one empty slot, return 1
               else if(grid[x-1][y] == EMPTY_SLOT) return 1;
            }
            //if it is at one space to the bottom
            else if(x == 2){
                //if both slots are empty above it, return 2
                if(grid[x-2][y] == EMPTY_SLOT && grid[x-1][y] == EMPTY_SLOT) return 2;
                //else if there is one slot empty above, return 1
                else if(grid[x-1][y] == EMPTY_SLOT) return 1;
            }
            //else if the slot is close to the very top and there is a space above, return 1
				else if(x == 1 && grid[x-1][y] == EMPTY_SLOT) return 1;
            //else nothing can be moved, return 0
				else return 0;
            
            //The algorithm is very similar for down, left, right directions below
			} else if(direction == DOWN_INPUT){
				if(x == 3) return 0;
            else if(x == 0){
               if(grid[x+3][y] == EMPTY_SLOT && grid[x+2][y] == EMPTY_SLOT && grid[x+1][y] == EMPTY_SLOT) return 3;
               else if(grid[x+2][y] == EMPTY_SLOT && grid[x+1][y] == EMPTY_SLOT) return 2;
               else if(grid[x+1][y] == EMPTY_SLOT) return 1;
            }
				else if(x == 1){
               if(grid[x+2][y] == EMPTY_SLOT && grid[x+1][y] == EMPTY_SLOT) return 2;
               else if(grid[x+1][y] == EMPTY_SLOT) return 1;
            }
            else if(x == 2 && grid[x+1][y] == EMPTY_SLOT) return 1;
				else return 0;
            
			} else if(direction == LEFT_INPUT){
				if(y == 0) return 0;
            else if(y == 3){
               if(grid[x][y-3] == EMPTY_SLOT && grid[x][y-2] == EMPTY_SLOT && grid[x][y-1] == EMPTY_SLOT) return 3;
               else if(grid[x][y-2] == EMPTY_SLOT && grid[x][y-1] == EMPTY_SLOT) return 2;
               else if(grid[x][y-1] == EMPTY_SLOT) return 1;
            }
				else if(y == 2){
               if(grid[x][y-2] == EMPTY_SLOT && grid[x][y-1] == EMPTY_SLOT) return 2;
               else if(grid[x][y-1] == EMPTY_SLOT) return 1;
            }
            else if(y == 1 && grid[x][y-1] == EMPTY_SLOT) return 1;
				else return 0;
            
			} else if(direction == RIGHT_INPUT){
				if(y == 3) return 0;
            else if(y == 0){
               if(grid[x][y+3] == EMPTY_SLOT && grid[x][y+2] == EMPTY_SLOT && grid[x][y+1] == EMPTY_SLOT) return 3;
               else if(grid[x][y+2] == EMPTY_SLOT && grid[x][y+1] == EMPTY_SLOT) return 2;
               else if(grid[x][y+1] == EMPTY_SLOT) return 1;
            }
				else if(y == 1){
               if(grid[x][y+2] == EMPTY_SLOT && grid[x][y+1] == EMPTY_SLOT) return 2;
               else if(grid[x][y+1] == EMPTY_SLOT) return 1;
            }
            else if(y == 2 && grid[x][y+1] == EMPTY_SLOT) return 1;
				else return 0;
			}
			return 0;
		}
		
      /**
       * Method that checks if a certain slot is combinable in a given direction, if yes then return true, else return false
       */   
		public boolean isCombinable(int x, int y, int direction){
      //if the slot is not empty
		if(grid[x][y] != -1){
         //if direction is up
			if(direction == UP_INPUT){
            //if the slot is in the uppermost row, return false
				if(x == 0) return false;
            //else if the slot and the slot above it are equal, return true
				else if(grid[x][y] == grid[x-1][y]) return true;
            //else just return false
				else return false;
            
         //The algorithm for the other three directions are similar
			} else if(direction == DOWN_INPUT){
				if(x == 3) return false;
				else if(grid[x][y] == grid[x+1][y]) return true;
				else return false;
			} else if(direction == LEFT_INPUT){
				if(y == 0) return false;
				else if(grid[x][y] == grid[x][y-1]) return true;
				else return false;
			} else if(direction == RIGHT_INPUT){
				if(y == 3) return false;
				else if(grid[x][y] == grid[x][y+1]) return true;
				else return false;
			}
			}
			return false;
		}
		
      
      /**
       * Method that slides a slot given the direction and the amount to slide
       */   
		public void slide(int x, int y, int amount, int direction){
         if(amount != 0){ //if amount is not equal to 0
			   if(direction == LEFT_INPUT){ //if direction is left
				   grid[x][y-amount] = grid[x][y]; //whatever slot amount distance away from the original slot will be equal to the original slot
				   grid[x][y] = EMPTY_SLOT;        //set the orignal slot to be empty
               
            //Algorithm for other three direction is similar
			   }else if(direction == RIGHT_INPUT){
				   grid[x][y+amount] = grid[x][y];
				   grid[x][y] = EMPTY_SLOT;
			   }else if(direction == UP_INPUT){
				   grid[x-amount][y] = grid[x][y];
				   grid[x][y] = EMPTY_SLOT;
			   }else if(direction == DOWN_INPUT){
				   grid[x+amount][y] = grid[x][y];
				   grid[x][y] = EMPTY_SLOT;
			   }
			}
      }
       
       
      /**
       * Method that combines two slots together, given the direction
       */   
      public void combine(int x, int y, int direction){
         //if direction is left
         if(direction == LEFT_INPUT){
            //the slot to the left of this slot would be the sum of both the slots
            grid[x][y-1] += grid[x][y];
            //this slot would be empty after they combine
				grid[x][y] = EMPTY_SLOT;
            //add the value of the slot to the left to the score
            score += grid[x][y-1];
            
         //Algorithm for other three directions are similar   
         }else if(direction == RIGHT_INPUT){
            grid[x][y+1] += grid[x][y];
				grid[x][y] = EMPTY_SLOT;
            score += grid[x][y+1];
         }else if(direction == UP_INPUT){
            grid[x-1][y] += grid[x][y];
				grid[x][y] = EMPTY_SLOT;
            score += grid[x-1][y];
         }else if(direction == DOWN_INPUT){
            grid[x+1][y] += grid[x][y];
				grid[x][y] = EMPTY_SLOT;
            score += grid[x+1][y];
         }
      }
      
      /**
       * Method that does the combination by checking if a slot is combinable and then calling the combine method
       */   
      public void doCombine(int direction){
         //if direction is left
         if(direction == LEFT_INPUT){
         //for every slot on the grid, starting from the upper left most			
			for(int i = 0;i<GRID_WIDTH;i++){
				for(int j = 0;j<GRID_LENGTH;j++){
               //if a value hasn't been combined there already
               if(!combined[i][j]){
                  //if the slot is combinable
					   if(isCombinable(i,j,direction)){ 
                     //combine the two slots
                     combine(i,j,direction);
                     //mark the two slots as already combined in the boolean array
                     combined[i][j] = true;
                     combined[i][j-1] = true;
                  }
               }   
				}
			}
         
         //Algorithm for other three directions are quite similar
         }else if(direction == RIGHT_INPUT){
         for(int i = 0;i<GRID_WIDTH;i++){
            //This loops from the right most of the grid because it is combining right, and the rules
            //say that when 3 numbers of the same value are together, always combine the ones closest
            //to that direction
				for(int j = GRID_LENGTH-1;j>=0;j--){
               if(!combined[i][j]){
					   if(isCombinable(i,j,direction)){ 
                     combine(i,j,direction);
                     combined[i][j] = true;
                     combined[i][j+1] = true;
                  }
               }   
				}
			}
         }else if(direction == UP_INPUT){
         for(int i = 0;i<GRID_WIDTH;i++){
				for(int j = 0;j<GRID_LENGTH;j++){
               if(!combined[i][j]){
					   if(isCombinable(i,j,direction)){ 
                     combine(i,j,direction);
                     combined[i][j] = true;
                     combined[i-1][j] = true;
                  }
               }   
				}
			}
         }else if(direction == DOWN_INPUT){
         //Need to start from the bottom of the grid to avoid the error stated above
         for(int i = GRID_WIDTH-1;i>=0;i--){
				for(int j = 0;j<GRID_LENGTH;j++){
               if(!combined[i][j]){
					   if(isCombinable(i,j,direction)){ 
                     combine(i,j,direction);
                     combined[i][j] = true;
                     combined[i+1][j] = true;
                  }
               }   
				}
			}
         }
      }
      
      /**
       * Method that resets the boolean 2D array to false for every round
       */   
      public void resetBool(){
         for(int i = 0;i<GRID_LENGTH;i++){
            for(int j = 0;j<GRID_WIDTH;j++){
               combined[i][j] = false;
            }
         }
      }
       
      /**
       * Method that does the sliding by checking if a slot is slidable and then calling the slide method
       */   
      public void doSlide(int direction){
         //if direction is left
         if(direction == LEFT_INPUT){
         //for every slot on the grid
         for(int i = 0;i<GRID_LENGTH;i++){
				for(int j = 0;j<GRID_WIDTH;j++){
               //calculate the amount that is slidable for each slot in that certain direction
               amount = isSlidable(i,j,direction);
               //slide the slot with whatever the amount may be
					slide(i,j, amount, direction);
				}
			}
         
         //Alogrithm for other three directions are similar
         }else if(direction == RIGHT_INPUT){
            for(int i = 0;i<GRID_LENGTH;i++){
				   for(int j = GRID_WIDTH-1;j>=0;j--){
                  amount = isSlidable(i,j,direction);
					   slide(i,j, amount, direction);
				   }
			   }
         }else if(direction == UP_INPUT){
            for(int i = 0;i<GRID_LENGTH;i++){
				   for(int j = 0;j<GRID_WIDTH;j++){
                  amount = isSlidable(i,j,direction);
					   slide(i,j, amount, direction);
				   }
			   }
         }else if(direction == DOWN_INPUT){
            for(int i = GRID_LENGTH-1;i>=0;i--){
				   for(int j = 0;j<GRID_WIDTH;j++){
                  amount = isSlidable(i,j,direction);
					   slide(i,j, amount, direction);
				   }
			   }
         }   
      }
      
      /**
       * Method that checks if the player has won
       */   
      public boolean checkWin(){
         //for every slot on the grid
         for(int i = 0;i<GRID_LENGTH;i++){
            for(int j = 0;j<GRID_WIDTH;j++){
               //if any slot is equal to the win condition(2048), then return true
               if(grid[i][j] == WIN_CONDITION) return true;
            }
         }
         //else just return false
         return false;
      }
      
      /**
       * Method that checks if the game is over
       */   
      public boolean checkGameOver(){
         //if the grid is not all filled, then return false
         if(!allFilled()) return false;
         //for every slot on the grid, horizontally
         for(int i = 0;i<GRID_LENGTH;i++){
            for(int j = 1;j<GRID_WIDTH-1;j++){
               //if a slot can be combined to either side of it ( left or right ) then return false
               if(grid[i][j] == grid[i][j+1] || grid[i][j] == grid[i][j-1]) return false;
            }
         }
         //for every slot on the grid, vertically
         for(int i = 1;i<GRID_LENGTH-1;i++){
            for(int j = 0;j<GRID_WIDTH;j++){
               //if a slot can be combined to either side ( top or bottom ) then return false
               if(grid[i][j] == grid[i+1][j] || grid[i][j] == grid[i-1][j]) return false;
            }
         }
         //else nothing can be possibly combined or moved, return true, game is over
         return true;
      }
      
      /**
       * Method that checks if the grid is all filled(full) or not
       */ 
      public boolean allFilled(){
         //for every slot on the grid
         for(int i = 0;i<GRID_LENGTH;i++){
            for(int j = 0;j<GRID_WIDTH;j++){
               //if a slot is empty, return false
               if(grid[i][j] == EMPTY_SLOT) return false;
            }
         }
         //else return true
         return true;
      }
      
      /**
       * Method that records the grid for the undo function
       */ 
		public void recordGrid(){
         //assign a new block of memory for lastGrid each time or else the pointer will always point to the same one
			lastGrid = new int[GRID_LENGTH][GRID_WIDTH];
         //for every slot on the grid
			for(int i = 0;i<GRID_LENGTH;i++){
				for(int j = 0;j<GRID_WIDTH;j++){
               //record the values onto the lastGrid variable
					lastGrid[i][j] = grid[i][j];
				}
			}
         //push the 2D array into the stack
			Object obj = lastGrid.clone(); 
			lastGrids.push((int[][])obj);
         //also push the score into the other stack
         lastScores.push(score);
		} 
      
      /**
       * Method that set the grid to its values a move before (undo)
       */ 
		public void undoGrid(){
         //if the stack is not empty
         if(!lastGrids.isEmpty()){
            //pop out the front value in the stack and assign it to a variable
				lastGrid = (int[][]) lastGrids.pop();
            //also pop out the score
            score = lastScores.pop();
            //for every slot on the grid
		      for(int i = 0;i<GRID_LENGTH;i++){
				   for(int j = 0;j<GRID_WIDTH;j++){
                  //assign the values of the current grid as the ones before
					   grid[i][j] = lastGrid[i][j];
				   }
			   }
            //tell the GUI to update the score on the screen
            gui.setScore(score);
         }
		}
		
      /**
       * Method that checks if anything has moved after the first two tiles were generated
       */ 
		public boolean moved(){
         //count variable to keep track of how many tiles there are on the screen
			int cnt = 0;
         //for every slot on the grid
			for(int i = 0;i<GRID_LENGTH;i++){
				for(int j = 0;j<GRID_WIDTH;j++){
               //if the slot is not empty, add 1 to count
					if(grid[i][j] != EMPTY_SLOT) cnt++;
				}
			}
         //if count is larger than 1 then return true
			if(cnt > 1) return true;
         //else return false
			else return false;
		}
			 
}