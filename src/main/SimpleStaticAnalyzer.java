package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Stack;
import java.util.TreeSet;
import helper.VarTaintStatus;
import helper.ScopeStack;

public class SimpleStaticAnalyzer {
	// to store function arguments in the program to be analyzed
	private static HashSet<String> funcArguments = new HashSet<String>();
	// to store variables declared inside the function in the program to be analyzed
	private static HashSet<String> funcVariables = new HashSet<String>(); 
	// to store variable sreturned by the function
	private static HashSet<String> returnVariables = new HashSet<String>(); 
	// to store current taintness of a variable
	private static HashMap<String, Boolean> currentlyTainted = new HashMap<String, Boolean>();
	// to store the line at which the variable gets tainted
	private static HashMap<String, TreeSet<Integer>> taintResult = new HashMap<String, TreeSet<Integer>>();
	// just for faster lookup in computeTaintness() function
	private static HashMap<Integer, Integer> lineNumToIndex = new HashMap<Integer, Integer>(); 
	private static HashMap<Integer, String> lineNumToVar = new HashMap<Integer, String>(); 
	// to maintain brackets
	private static HashMap<Integer, String> lineNumToBracketType = new HashMap<Integer, String>();
	// to handle if-else inside while loop
	private static HashMap<String, Boolean> val = new HashMap<String, Boolean>();
	
	private static ArrayList<VarTaintStatus> varTaintStatus = new ArrayList<VarTaintStatus>();
	private static ArrayList<ScopeStack> scopeStack = new ArrayList<ScopeStack> ();
	
	private static Integer lineNum = 0;
	private static Integer scopeNum = 0;
	private static Boolean insideIf = false;
	private static Boolean insideElse = false;
	private static Boolean outsideIf = false;
	private static Boolean whileInsideIf = false;
	private static Boolean whileInsideElse = false;
	private static Boolean insideWhile = false;
	
	// variable inside if and their taintness
	private static HashMap<String, Boolean> varInsideIf = new HashMap<String, Boolean>();
	// variable inside else and their taintness
	private static HashMap<String, Boolean> varInsideElse = new HashMap<String, Boolean>();
		
	public static void main(String[] args) throws IOException {
		Scanner input = new Scanner(System.in);
		
		System.out.println("NOTE: MAKE SURE YOUR TEST CASE FILE IS PRESENT INSIDE THE test PACKAGE\n");
		System.out.print("Enter the name of the file containing the program to be analyzed: ");
		
		String fileName = input.nextLine();
		String filePath = "src" + File.separator + "test" + File.separator + fileName; 
		
		File f = new File(filePath);
		
		// check whether the file exists or not
		if(f.exists()) {
			// get all the variables which are passed as arguments to the function in the program to be analyzed
			SimpleStaticAnalyzer.getFuncArguments(filePath);
			
			// function to perform static taint analysis of the program
			SimpleStaticAnalyzer.staticTaintAnalysis(filePath);
			
			// print whether the variables returned by the function in the given program are tainted or not
			SimpleStaticAnalyzer.printResult();
		}
		else {
			System.out.println("\nERROR: The file doesn't exists. Please make sure that the file is present inside the 'test' package.");
		}
		
		input.close();
	}
	
	// get all the variables which are passed as arguments to the function in the program to be analyzed
	private static void getFuncArguments(String filePath) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		String readLine = reader.readLine();
		lineNum++;
		
		String funcArgs[] = readLine.trim().split("[,\\s\\;\\(\\)]+");
		
		for(int i = 0; i < funcArgs.length; ++i) {
			// function arguments will be at odd positions in the String array funcArguments except position 1
			// position 1 will contain the function name
			if(i != 1 && i % 2 != 0) {
				funcArguments.add(funcArgs[i]);
				scopeStack.add(new ScopeStack(funcArgs[i], lineNum, true));
				currentlyTainted.put(funcArgs[i], true);
			}
			// check if there is an opening bracket after the function definition
			if(funcArgs[i].equals("{")) {
				scopeStack.add(new ScopeStack("{", lineNum, null));
				lineNumToBracketType.put(lineNum, "{");
				scopeNum++;
			}
		}
		
		reader.close();
	}
	
	// function to perform static taint analysis of the program
	private static void staticTaintAnalysis(String filePath) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		// skipping the first line because we have already dealt with it in getFuncArguments() function
		String readLine = reader.readLine();
				
		while((readLine = reader.readLine()) != null) {
			lineNum++;
			
			String words[] = readLine.trim().split("[,\\s\\;\\(\\)\\+\\-\\/\\*//%//=]+");
			
			// check if the statement starts with an opening bracket
			if(words[0].equals("{")) {
				scopeStack.add(new ScopeStack("{", lineNum, null));
				lineNumToBracketType.put(lineNum, "{");
				scopeNum++;
			}
			// check if the statement starts with a closing bracket
			else if(words[0].equals("}")) {
				SimpleStaticAnalyzer.computeCurrentTaintness();
				
				if(insideWhile && scopeNum == 2) {
					insideWhile = false;
					revertValues();
				}
				
				if(insideElse) {
					SimpleStaticAnalyzer.checkUntaintness(lineNum, scopeNum);
					insideIf = false;
					outsideIf = false;
					insideElse = false;
					whileInsideIf = false;
					whileInsideElse = false;
					varInsideIf.clear();
					varInsideElse.clear();
				}
				else if(insideIf) {
					outsideIf = true;
				}
				
				lineNumToBracketType.put(lineNum, "}");
				scopeNum--;
			}
			// check if the statement starts with an int keyword
			else if(words[0].equals("int")) {
				Boolean flag = true;
				
				funcVariables.add(words[1]);
				
				// check if it is a variable initialization
				if(words.length > 2) {
					// check if there is a tainted variable in the RHS of assignment
					// if there is at-least one tainted variable, then LHS of assignment will be tainted
					for(int i = 2; i < words.length; ++i) {
						if(currentlyTainted.containsKey(words[i]) && currentlyTainted.get(words[i])) {
							varTaintStatus.add(new VarTaintStatus(words[1], lineNum, true, scopeNum));
							scopeStack.add(new ScopeStack(words[1], lineNum, true));
							currentlyTainted.put(words[1], true);
							flag = false;
							break;
						}
					}
				}
				
				if(flag) {
					varTaintStatus.add(new VarTaintStatus(words[1], lineNum, false, scopeNum));
					scopeStack.add(new ScopeStack(words[1], lineNum, false));
					currentlyTainted.put(words[1], false);
				}
				
				lineNumToIndex.put(lineNum, varTaintStatus.size()-1);
				lineNumToVar.put(lineNum, words[1]);
			}
			// check if the statement starts with a function argument variable or a variable declared inside the function
			else if(funcArguments.contains(words[0]) || funcVariables.contains(words[0])) {
				Boolean flag = true;
				
				// check if it is a variable initialization
				if(words.length > 1) {
					// check if there is a tainted variable in the RHS of assignment
					// if there is at-least one tainted variable, then LHS of assignment will be tainted
					for(int i = 1; i < words.length; ++i) {
						if(currentlyTainted.containsKey(words[i]) && currentlyTainted.get(words[i])) {
							varTaintStatus.add(new VarTaintStatus(words[0], lineNum, true, scopeNum));
							scopeStack.add(new ScopeStack(words[0], lineNum, true));
							currentlyTainted.put(words[0], true);
							
							if(!whileInsideElse && insideElse) {
								varInsideElse.put(words[0], true);
							}
							else if(!whileInsideIf && insideIf) {
								varInsideIf.put(words[0], true);
							}
							
							flag = false;
							break;
						}
					}
				}
				
				if(flag) {
					varTaintStatus.add(new VarTaintStatus(words[0], lineNum, false, scopeNum));
					scopeStack.add(new ScopeStack(words[0], lineNum, false));
					currentlyTainted.put(words[0], false);
					
					if(!whileInsideElse && insideElse) {
						varInsideElse.put(words[0], false);
					}
					else if(!whileInsideIf && insideIf) {
						varInsideIf.put(words[0], false);
					}
				}
				
				lineNumToIndex.put(lineNum, varTaintStatus.size()-1);
				lineNumToVar.put(lineNum, words[0]);
			}
			// check if the statement starts with if statement
			else if(words[0].equals("if")) {
				
				if(insideIf && outsideIf) {
					insideIf = false;
					outsideIf = false;
					whileInsideIf = false;
					varInsideIf.clear();
				}
				
				insideIf = true;
				for(int i = 1; i < words.length; ++i) {
					if(words[i].equals("{")) {
						scopeStack.add(new ScopeStack("{", lineNum, null));
						lineNumToBracketType.put(lineNum, "{");
						scopeNum++;
					}
				}
			}
			// check if the statement starts with else statement
			else if(words[0].equals("else")) {
				
				if(insideIf && outsideIf) {
					insideElse = true;
				}
				
				for(int i = 1; i < words.length; ++i) {
					if(words[i].equals("{")) {
						scopeStack.add(new ScopeStack("{", lineNum, null));
						lineNumToBracketType.put(lineNum, "{");
						scopeNum++;
					}
				}
			}
			// check if the statement starts with while statement
			else if(words[0].equals("while")) {

				if(insideIf && !outsideIf) {
					whileInsideIf = true;
				}
				else if(outsideIf && insideElse) {
					whileInsideElse = true;
				}
				else {
					insideWhile = true;
				}
				
				for(int i = 1; i < words.length; ++i) {
					if(words[i].equals("{")) {
						scopeStack.add(new ScopeStack("{", lineNum, null));
						lineNumToBracketType.put(lineNum, "{");
						scopeNum++;
					}
				}
			}
			// check if it is a return statement
			else if(words[0].equals("return")) {
				String returnedVar = words[1];
				// add this to returnVariables set
				returnVariables.add(returnedVar);
				// check whether the returned variable is tainteed or not
				SimpleStaticAnalyzer.computeTaintness(returnedVar, lineNum, scopeNum);
			}
		}
		
		reader.close();
	}

	private static void revertValues() {
		if(!val.isEmpty()) {
			for(String var : val.keySet()) {
				currentlyTainted.put(var, val.get(var));
			}
		}
		val.clear();
	}

	private static void checkUntaintness(Integer lineNum, Integer scopeNum) {
		for(String var : varInsideIf.keySet()) {
			//System.out.println(currentlyTainted.get(var));
			//System.out.println(var);
			if(!varInsideIf.get(var) && varInsideElse.containsKey(var) && !varInsideElse.get(var)) {
				//System.out.println(lineNum + ":" + scopeNum);
				if(scopeNum == 3) {
					val.put(var, currentlyTainted.get(var));
				}
				currentlyTainted.put(var, false);
			}

			//System.out.println(currentlyTainted.get(var));
		}
	}

	// compute current taintness of variables when a closing bracket is encountered
	private static void computeCurrentTaintness() {
		Integer pos = null;
		/*System.out.println("boo");
		for(ScopeStack s : scopeStack) {
			System.out.println(s.getItem() + "->" + s.getLineNum() + "->" + s.getIsTainted());
		}*/
		
		for(int i = scopeStack.size()-1 ; i >= 0; --i) {
			if(scopeStack.get(i).getItem().equals("{")) {
				pos = i;
				break;
			}
		}
		
		for(int i = scopeStack.size()-1 ; i >= pos; --i) {
			if(i == pos) {
				scopeStack.remove((int)pos);
				break;
			}
			
			String var = scopeStack.get(i).getItem();
			
			for(int j = pos-1; j >= 0; --j) {
				if(scopeStack.get(j).getItem().equals(var)) {
					currentlyTainted.put(var, scopeStack.get(j).getIsTainted());
					break;
				}
			}
			
			scopeStack.remove(i);
		}
	}

	private static void computeTaintness(String returnedVar, Integer lineNum, Integer returnScopeNum) {
		// to check if the variable is not tainted
		Boolean isTainted = true;
		Integer parsedBracketNum = 0;
		
		for(int i = lineNum-1; i > 1; --i) {
			if(lineNumToBracketType.containsKey(i) && lineNumToBracketType.get(i).equals("{")) {
				parsedBracketNum++;
			}
			else if(lineNumToVar.containsKey(i)) {
				String var = lineNumToVar.get(i);
				
				if(var.equals(returnedVar)) {
					int ind = lineNumToIndex.get(i);
					int parsedScopeNum = varTaintStatus.get(ind).getScopeNum();
					boolean parsedIsTainted = varTaintStatus.get(ind).getIsTainted();
					
					if(returnScopeNum == 1) {
						if(parsedScopeNum == 1 && !parsedIsTainted) {
							isTainted = false;
							break;
						}
						else {
							break;
						}
					}
					else if(returnScopeNum == 2) {
						if(parsedScopeNum == 1 && !parsedIsTainted) {
							isTainted = false;
							break;
						}
						else if(parsedScopeNum == 2 && parsedBracketNum == 0 && !parsedIsTainted) {
							isTainted = false;
							break;
						}
						else {
							break;
						}
					}
					else if(returnScopeNum == 3) {
						if(parsedScopeNum == 3 && parsedBracketNum == 0 && !parsedIsTainted) {
							isTainted = false;
							break;
						}
						else if(parsedScopeNum == 2 && parsedBracketNum == 1 && !parsedIsTainted) {
							isTainted = false;
							break;
						}
						else if(parsedScopeNum == 1 && !parsedIsTainted) {
							isTainted = false;
							break;
						}
						else {
							break;
						}
					}
				}
			}
		}
		
		Stack<ScopeStack> tmp = new Stack<ScopeStack>();
		
		// the variable may be tainted
		if(isTainted) {
			tmp.clear();
			taintResult.put(returnedVar, new TreeSet<Integer>());
			
			for(int i = 1; i < lineNum; ++i) {
				if(lineNumToBracketType.containsKey(i)) {
					if(lineNumToBracketType.get(i).equals("}")) {
						Boolean flag = true;
						while(!tmp.peek().getItem().equals("{")) {
							ScopeStack s = tmp.pop();
							
							if(flag && s.getItem().equals(returnedVar) && s.getIsTainted()) {
								taintResult.get(returnedVar).add(s.getLineNum());
							}
							else if(flag && s.getItem().equals(returnedVar) && !s.getIsTainted()) {
								flag = false;
							}
						}
						tmp.pop();
					}
					else {
						tmp.push(new ScopeStack("{", i, null));
					}
				}
				else if(lineNumToVar.containsKey(i)) {
					tmp.push(new ScopeStack(lineNumToVar.get(i), i, varTaintStatus.get(lineNumToIndex.get(i)).getIsTainted()));
				}
			}
			
			Boolean flag = true;
			
			while(!tmp.empty()) {
				ScopeStack s = tmp.pop();
				
				if(flag && s.getItem().equals(returnedVar) && s.getIsTainted()) {
					taintResult.get(returnedVar).add(s.getLineNum());
				}
				else if(flag && s.getItem().equals(returnedVar) && !s.getIsTainted()) {
					flag = false;
				}
				
				if(s.getItem().equals("{")) {
					flag = true;
				}
			}
			
			if(taintResult.get(returnedVar).isEmpty()) {
				taintResult.remove(returnedVar);
			}
		}
	}
	
	public static void printResult() {
		/*for(VarTaintStatus v : varTaintStatus) {
			System.out.println(v.getVar() + ":" + v.getLineNum() + ":" + v.getIsTainted() + ":" + v.getScopeNum());
		}*/
		
		Boolean flag = false;
		
		System.out.println("\nPotentially tainted variables at the start of return statement:");
		System.out.print("{");
		
		for(String var : taintResult.keySet()) {
			for(Integer lineNum : taintResult.get(var)) {
				if(flag) {
					System.out.print(", <" + var + ", " + lineNum + ">");
				}
				else {
					System.out.print("<" + var + ", " + lineNum + ">");
					flag = true;
				}
			}
		}
		System.out.println("}");
		System.out.print("Variables which are untainted: ");
		System.out.print("<");
		
		flag = false;
		
		for(String var : returnVariables) {
			if(flag && !taintResult.containsKey(var)) {
				System.out.print(", " + var);
			}
			else if(!taintResult.containsKey(var)) {
				System.out.print(var);
				flag = true;
			}
		}
		
		System.out.println(">");
	}
}